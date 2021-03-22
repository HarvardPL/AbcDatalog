/*******************************************************************************
 * This file is part of the AbcDatalog project.
 *
 * Copyright (c) 2016, Harvard University
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the BSD License which accompanies this distribution.
 *
 * The development of the AbcDatalog project has been supported by the 
 * National Science Foundation under Grant Nos. 1237235 and 1054172.
 *
 * See README for contributors.
 ******************************************************************************/
package abcdatalog.engine.bottomup.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import abcdatalog.ast.Clause;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.ast.Premise;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.ast.validation.DatalogValidator;
import abcdatalog.ast.validation.StratifiedNegationValidator;
import abcdatalog.ast.validation.StratifiedProgram;
import abcdatalog.ast.validation.UnstratifiedProgram;
import abcdatalog.ast.visitors.HeadVisitor;
import abcdatalog.ast.visitors.HeadVisitorBuilder;
import abcdatalog.ast.visitors.PremiseVisitor;
import abcdatalog.ast.visitors.PremiseVisitorBuilder;
import abcdatalog.engine.bottomup.AnnotatedAtom;
import abcdatalog.engine.bottomup.ClauseEvaluator;
import abcdatalog.engine.bottomup.EvalManager;
import abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator;
import abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator.SemiNaiveClause;
import abcdatalog.util.ExecutorServiceCounter;
import abcdatalog.util.Utilities;
import abcdatalog.util.datastructures.ConcurrentFactIndexer;
import abcdatalog.util.datastructures.ConcurrentFactTrie;
import abcdatalog.util.datastructures.ConcurrentLinkedBag;
import abcdatalog.util.datastructures.IndexableFactCollection;
import abcdatalog.util.substitution.ClauseSubstitution;

public class StratifiedNegationEvalManager implements EvalManager {
	private final ExecutorServiceCounter handlerExecService = new ExecutorServiceCounter(
			Executors.newCachedThreadPool());
	private final ForkJoinPool saturationPool = new ForkJoinPool(Utilities.concurrency,
			ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

	private final ConcurrentFactIndexer<ConcurrentLinkedBag<PositiveAtom>> facts = new ConcurrentFactIndexer<>(
			() -> new ConcurrentLinkedBag<>(), (bag, atom) -> bag.add(atom), () -> ConcurrentLinkedBag.emptyBag());
	private final ConcurrentFactTrie trie = new ConcurrentFactTrie();

	private final Map<PredicateSym, Set<Integer>> relevantStrataByPred = new HashMap<>();

	private final List<StratumHandler> handlers = new ArrayList<>();

	private StratifiedProgram stratProg;

	private final static int EDB_STRATUM = -1;

	@Override
	public void initialize(Set<Clause> program) throws DatalogValidationException {
		UnstratifiedProgram prog = (new DatalogValidator()).withBinaryDisunificationInRuleBody()
				.withBinaryUnificationInRuleBody().withAtomNegationInRuleBody().validate(program);
		stratProg = StratifiedNegationValidator.validate(prog);

		Map<PredicateSym, Integer> stratumByPred = new HashMap<>(stratProg.getPredToStratumMap());
		for (PredicateSym p : this.stratProg.getEdbPredicateSyms()) {
			stratumByPred.put(p, EDB_STRATUM);
		}

		PremiseVisitor<Void, PredicateSym> getPred = (new PremiseVisitorBuilder<Void, PredicateSym>())
				.onAnnotatedAtom((atom, nothing) -> atom.getPred()).orCrash();
		HeadVisitor<Void, PredicateSym> getHeadPred = (new HeadVisitorBuilder<Void, PredicateSym>())
				.onPositiveAtom((atom, nothing) -> atom.getPred()).orCrash();

		int nstrata = stratProg.getStrata().size();
		@SuppressWarnings("unchecked")
		Set<SemiNaiveClause>[] relevantRulesByStratum = new HashSet[nstrata];
		for (int i = 0; i < nstrata; ++i) {
			relevantRulesByStratum[i] = new HashSet<>();
		}
		SemiNaiveClauseAnnotator annotator = new SemiNaiveClauseAnnotator(stratProg.getIdbPredicateSyms());
		for (SemiNaiveClause rule : annotator.annotate(this.stratProg.getRules())) {
			PredicateSym headPred = rule.getHead().accept(getHeadPred, null);
			int stratum = stratumByPred.get(headPred);
			relevantRulesByStratum[stratum].add(rule);

			PredicateSym firstPred = rule.getBody().get(0).accept(getPred, null);
			Utilities.getSetFromMap(this.relevantStrataByPred, firstPred).add(stratum);
		}

		for (int i = 0; i < nstrata; ++i) {
			this.handlers.add(new StratumHandler(i, relevantRulesByStratum[i], stratumByPred));
		}
	}

	private void propagateStratumCompletion(int stratum) {
		for (StratumHandler handler : this.handlers) {
			handler.reportCompletedStratum(stratum);
		}
	}

	@Override
	public IndexableFactCollection eval() {
		for (PositiveAtom fact : this.stratProg.getInitialFacts()) {
			this.trie.add(fact);
			this.facts.add(fact);
			this.propagateNewFact(fact);
		}

		for (StratumHandler handler : handlers) {
			this.handlerExecService.submitTask(handler);
		}

		this.propagateStratumCompletion(EDB_STRATUM);
		this.handlerExecService.blockUntilFinished();
		this.handlerExecService.shutdownAndAwaitTermination();

		this.saturationPool.shutdown();
		boolean finished = false;
		do {
			try {
				finished = this.saturationPool.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (!finished);

		return this.facts;
	}

	private void propagateNewFact(PositiveAtom fact) {
		Set<Integer> relevantStrata = this.relevantStrataByPred.get(fact.getPred());
		if (relevantStrata != null) {
			for (Integer stratum : relevantStrata) {
				this.handlers.get(stratum).reportFact(fact);
			}
		}
	}

	private class StratumHandler implements Runnable {
		private final Set<Integer> posDependencies;
		private final Set<Integer> negDependencies;
		private final Map<PredicateSym, Set<ClauseEvaluator>> clauseEvaluatorsByFirstPred;
		private final int stratum;
		private volatile boolean running;
		private final Queue<PositiveAtom> queuedFacts = new ConcurrentLinkedQueue<>();
		private final ExecutorServiceCounter exec = new ExecutorServiceCounter(saturationPool);
		private final BlockingQueue<Integer> completedStrataFeed = new LinkedBlockingQueue<>();

		public StratumHandler(int stratum, Set<SemiNaiveClause> relevantRules,
				Map<PredicateSym, Integer> stratumByPred) {
			this.stratum = stratum;

			this.posDependencies = new HashSet<>();
			this.negDependencies = new HashSet<>();

			PremiseVisitor<Void, Boolean> addPred = (new PremiseVisitorBuilder<Void, Boolean>())
					.onAnnotatedAtom((atom, nothing) -> this.posDependencies.add(stratumByPred.get(atom.getPred())))
					.onNegatedAtom((atom, nothing) -> this.negDependencies.add(stratumByPred.get(atom.getPred())))
					.orNull();
			for (SemiNaiveClause rule : relevantRules) {
				for (Premise c : rule.getBody()) {
					c.accept(addPred, null);
				}
			}

			// Account for recursive rules.
			this.posDependencies.remove(this.stratum);
			
			this.clauseEvaluatorsByFirstPred = new HashMap<>();
			BiFunction<AnnotatedAtom, ClauseSubstitution, Iterable<PositiveAtom>> getFacts = (atom, s) -> facts
					.indexInto(atom.asUnannotatedAtom(), s);
			BiConsumer<PositiveAtom, ClauseSubstitution> newFact = (atom, s) -> {
				if (trie.add(atom, s)) {
					PositiveAtom f = atom.applySubst(s);
					facts.add(f);
					propagateNewFact(f);
				}
			};

			PremiseVisitor<Void, PredicateSym> getPred = (new PremiseVisitorBuilder<Void, PredicateSym>())
					.onAnnotatedAtom((atom, nothing) -> atom.getPred()).orCrash();
			for (SemiNaiveClause cl : relevantRules) {
				PredicateSym bodyPred = cl.getBody().get(0).accept(getPred, null);
				ClauseEvaluator ce = new ClauseEvaluator(cl, newFact, getFacts);
				Utilities.getSetFromMap(this.clauseEvaluatorsByFirstPred, bodyPred).add(ce);
			}
		}

		@Override
		public void run() {
			while (!this.negDependencies.isEmpty()) {
				try {
					int n = this.completedStrataFeed.take();
					this.negDependencies.remove(n);
					this.posDependencies.remove(n);
				} catch (InterruptedException e) {
					// do nothing
				}
			}

			this.running = true;

			while (!this.queuedFacts.isEmpty()) {
				this.evaluateWithNewFact(this.queuedFacts.remove());
			}

			while (!this.posDependencies.isEmpty()) {
				try {
					this.posDependencies.remove(this.completedStrataFeed.take());
				} catch (InterruptedException e) {
					// do nothing
				}
			}

			this.exec.blockUntilFinished();

			propagateStratumCompletion(this.stratum);
		}

		private void evaluateWithNewFact(PositiveAtom fact) {
			Set<ClauseEvaluator> ces = this.clauseEvaluatorsByFirstPred.get(fact.getPred());
			assert ces != null;
			for (ClauseEvaluator ce : ces) {
				this.exec.submitTask(new Runnable() {

					@Override
					public void run() {
						ce.evaluate(fact);
					}

				});
			}
		}

		public void reportFact(PositiveAtom fact) {
			if (!this.running) {
				this.queuedFacts.add(fact);
			}

			if (this.running) {
				this.evaluateWithNewFact(fact);
			}
		}

		public void reportCompletedStratum(int stratum) {
			try {
				this.completedStrataFeed.put(stratum);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}