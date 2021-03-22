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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import abcdatalog.ast.Clause;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.ast.validation.DatalogValidator;
import abcdatalog.ast.validation.UnstratifiedProgram;
import abcdatalog.engine.bottomup.AnnotatedAtom;
import abcdatalog.engine.bottomup.BottomUpEngineFrame;
import abcdatalog.engine.bottomup.ClauseEvaluator;
import abcdatalog.engine.bottomup.EvalManager;
import abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator;
import abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator.SemiNaiveClause;
import abcdatalog.engine.testing.ConjunctiveQueryTests;
import abcdatalog.engine.testing.CoreTests;
import abcdatalog.engine.testing.ExplicitUnificationTests;
import abcdatalog.util.Box;
import abcdatalog.util.ExecutorServiceCounter;
import abcdatalog.util.Utilities;
import abcdatalog.util.datastructures.ConcurrentFactIndexer;
import abcdatalog.util.datastructures.ConcurrentFactTrie;
import abcdatalog.util.datastructures.FactIndexerFactory;
import abcdatalog.util.datastructures.IndexableFactCollection;
import abcdatalog.util.substitution.ClauseSubstitution;
import abcdatalog.util.substitution.ConstOnlySubstitution;

/**
 * A concurrent bottom-up Datalog engine that employs a saturation algorithm
 * similar to semi-naive evaluation. It supports explicit unification. The
 * client can set the size of the work item (i.e., number of facts that are
 * bundled together during evaluation).
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	ConcurrentChunkedBottomUpEngine.MyCoreTests.class,
	ConcurrentChunkedBottomUpEngine.MyUnificationTests.class,
	})
public class ConcurrentChunkedBottomUpEngine extends BottomUpEngineFrame {

	public ConcurrentChunkedBottomUpEngine(int chunkSize) {
		super(new ChunkedEvalManager(chunkSize));
	}

	private static class ChunkedEvalManager implements EvalManager {
		private UnstratifiedProgram program;
		private final ConcurrentFactTrie redundancyTrie = new ConcurrentFactTrie();
		private final ConcurrentFactIndexer<Queue<PositiveAtom>> index = FactIndexerFactory
				.createConcurrentQueueFactIndexer();
		private final Map<PredicateSym, Set<SemiNaiveClause>> predToRuleMap = new HashMap<>();
		private final ExecutorServiceCounter exec = new ExecutorServiceCounter(
				new ForkJoinPool(Utilities.concurrency, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true));
		private final int chunkSize;

		public ChunkedEvalManager(int chunkSize) {
			this.chunkSize = chunkSize;
		}

		@Override
		public void initialize(Set<Clause> program) throws DatalogValidationException {
			this.program = (new DatalogValidator()).withBinaryUnificationInRuleBody()
					.withBinaryDisunificationInRuleBody().validate(program);
		}

		@Override
		public IndexableFactCollection eval() {
			SemiNaiveClauseAnnotator annotator = new SemiNaiveClauseAnnotator(program.getIdbPredicateSyms());
			for (SemiNaiveClause cl : annotator.annotate(program.getRules())) {
				Utilities.getSetFromMap(predToRuleMap, cl.getFirstAtom().getPred()).add(cl);
			}

			for (PositiveAtom fact : program.getInitialFacts()) {
				if (redundancyTrie.add(fact)) {
					index.add(fact);
				} else {
					throw new AssertionError();
				}
			}

			// FIXME double check on visibility (i.e., whether it's okay to use
			// ArrayList)
			Queue<PositiveAtom> chunk = new ConcurrentLinkedQueue<>();
			int size = 0;
			for (PositiveAtom fact : program.getInitialFacts()) {
				chunk.add(fact);
				if (++size == chunkSize) {
					exec.submitTask(new WorkItem(chunk));
					chunk = new ConcurrentLinkedQueue<>();
					size = 0;
				}
			}
			if (size != 0) {
				exec.submitTask(new WorkItem(chunk));
			}

			exec.blockUntilFinished();
			exec.shutdownAndAwaitTermination();

			return index;
		}

		private Iterable<PositiveAtom> getFacts(AnnotatedAtom a, ConstOnlySubstitution s) {
			return index.indexInto(a.asUnannotatedAtom(), s);
		}

		private class WorkItem implements Runnable {
			private final Iterable<PositiveAtom> facts;

			public WorkItem(Iterable<PositiveAtom> facts) {
				this.facts = facts;
			}

			@Override
			public void run() {
				Box<Queue<PositiveAtom>> acc = new Box<>();
				acc.value = new ConcurrentLinkedQueue<>();
				Box<Integer> size = new Box<>();
				size.value = 0;

				BiConsumer<PositiveAtom, ClauseSubstitution> reportFact = (a, s) -> {
					if (redundancyTrie.add(a, s)) {
						PositiveAtom fact = a.applySubst(s);
						index.add(fact);
						acc.value.add(fact);
						if (++size.value == chunkSize) {
							exec.submitTask(new WorkItem(acc.value));
							acc.value = new ConcurrentLinkedQueue<>();
							size.value = 0;
						}
					}
				};

				Map<PredicateSym, List<ClauseEvaluator>> predToEvalMap = new HashMap<>();

				for (PositiveAtom fact : facts) {
					PredicateSym pred = fact.getPred();
					List<ClauseEvaluator> evals = predToEvalMap.get(pred);
					if (evals == null) {
						Iterable<SemiNaiveClause> rules = predToRuleMap.get(pred);
						if (rules == null) {
							evals = Collections.emptyList();
						} else {
							evals = new ArrayList<>();
							for (SemiNaiveClause cl : rules) {
								evals.add(new ClauseEvaluator(cl, reportFact, ChunkedEvalManager.this::getFacts));
							}
						}
						predToEvalMap.put(pred, evals);
					}

					for (ClauseEvaluator eval : evals) {
						eval.evaluate(fact);
					}
				}

				if (size.value != 0) {
					exec.submitTask(new WorkItem(acc.value));
				}

			}

		}

	}

	public static class MyCoreTests extends CoreTests {

		public MyCoreTests() {
			super(() -> new ConcurrentChunkedBottomUpEngine(4));
		}

	}

	public static class MyUnificationTests extends ExplicitUnificationTests {

		public MyUnificationTests() {
			super(() -> new ConcurrentChunkedBottomUpEngine(4));
		}

	}
	
	public static class MyConjunctiveQueryTests extends ConjunctiveQueryTests {

		public MyConjunctiveQueryTests() {
			super(() -> new ConcurrentChunkedBottomUpEngine(4));
		}

	}

}
