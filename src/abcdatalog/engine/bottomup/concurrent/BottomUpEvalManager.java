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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import abcdatalog.ast.Clause;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.ast.validation.DatalogValidator;
import abcdatalog.ast.validation.UnstratifiedProgram;
import abcdatalog.engine.bottomup.AnnotatedAtom;
import abcdatalog.engine.bottomup.ClauseEvaluator;
import abcdatalog.engine.bottomup.EvalManager;
import abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator;
import abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator.SemiNaiveClause;
import abcdatalog.util.ExecutorServiceCounter;
import abcdatalog.util.Utilities;
import abcdatalog.util.datastructures.ConcurrentFactTrie;
import abcdatalog.util.datastructures.FactIndexer;
import abcdatalog.util.datastructures.FactIndexerFactory;
import abcdatalog.util.datastructures.IndexableFactCollection;
import abcdatalog.util.substitution.ClauseSubstitution;

/**
 * An evaluation manager that implements a saturation algorithm similar to
 * semi-naive evaluation. It supports explicit unification.
 *
 */
public class BottomUpEvalManager implements EvalManager {

	protected final Map<PredicateSym, Set<ClauseEvaluator>> predToEvalMap = new HashMap<>();
	protected final ExecutorServiceCounter exec = new ExecutorServiceCounter(
			new ForkJoinPool(Utilities.concurrency, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true));
	protected final FactIndexer facts = FactIndexerFactory.createConcurrentQueueFactIndexer();
	protected final Set<PositiveAtom> initialFacts = Utilities.createConcurrentSet();
	protected final ConcurrentFactTrie trie = new ConcurrentFactTrie();

	@Override
	public synchronized void initialize(Set<Clause> program) throws DatalogValidationException {
		UnstratifiedProgram prog = (new DatalogValidator()).withBinaryDisunificationInRuleBody()
				.withBinaryUnificationInRuleBody().validate(program);
		initialFacts.addAll(prog.getInitialFacts());

		SemiNaiveClauseAnnotator annotator = new SemiNaiveClauseAnnotator(prog.getIdbPredicateSyms());
		// set up map from predicate sym to rules. this depends on the first
		// atom in the annotated rule body being the "delta" atom
		for (SemiNaiveClause cl : annotator.annotate(prog.getRules())) {
			Utilities.getSetFromMap(this.predToEvalMap, cl.getFirstAtom().getPred())
					.add(new ClauseEvaluator(cl, this::newFact, this::getFacts));
		}
	}

	@Override
	public synchronized IndexableFactCollection eval() {
		this.facts.addAll(this.initialFacts);
		for (PositiveAtom fact : this.initialFacts) {
			this.trie.add(fact);
		}
		this.processInitialFacts(this.initialFacts);
		this.exec.blockUntilFinished();
		this.exec.shutdownAndAwaitTermination();
		return this.facts;
	}

	protected void processInitialFacts(Set<PositiveAtom> facts) {
		for (PositiveAtom fact : facts) {
			this.processNewFact(fact);
		}
	}

	protected void processNewFact(PositiveAtom newFact) {
		Set<ClauseEvaluator> evals = this.predToEvalMap.get(newFact.getPred());
		if (evals != null) {
			for (ClauseEvaluator ce : evals) {
				Runnable task = new Runnable() {

					@Override
					public void run() {
						ce.evaluate(newFact);
					}
				};
				this.exec.submitTask(task);
			}
		}
	}

	protected Iterable<PositiveAtom> getFacts(AnnotatedAtom atom, ClauseSubstitution s) {
		return facts.indexInto(atom.asUnannotatedAtom(), s);
	}

	protected void newFact(PositiveAtom atom, ClauseSubstitution s) {
		if (trie.add(atom, s)) {
			PositiveAtom f = atom.applySubst(s);
			facts.add(f);
			processNewFact(f);
		}
	}

}
