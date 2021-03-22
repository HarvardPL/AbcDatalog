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
package abcdatalog.engine.bottomup.sequential;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import abcdatalog.ast.Clause;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.ast.Premise;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.ast.validation.DatalogValidator;
import abcdatalog.ast.validation.StratifiedNegationValidator;
import abcdatalog.ast.validation.StratifiedProgram;
import abcdatalog.ast.validation.DatalogValidator.ValidClause;
import abcdatalog.ast.validation.UnstratifiedProgram;
import abcdatalog.ast.visitors.HeadVisitor;
import abcdatalog.ast.visitors.PremiseVisitor;
import abcdatalog.ast.visitors.PremiseVisitorBuilder;
import abcdatalog.engine.bottomup.AnnotatedAtom;
import abcdatalog.engine.bottomup.ClauseEvaluator;
import abcdatalog.engine.bottomup.EvalManager;
import abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator;
import abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator.SemiNaiveClause;
import abcdatalog.util.Utilities;
import abcdatalog.util.datastructures.ConcurrentFactIndexer;
import abcdatalog.util.datastructures.FactIndexer;
import abcdatalog.util.datastructures.FactIndexerFactory;
import abcdatalog.util.datastructures.IndexableFactCollection;
import abcdatalog.util.substitution.ClauseSubstitution;

public class SemiNaiveEvalManager implements EvalManager {
	private final ConcurrentFactIndexer<Set<PositiveAtom>> allFacts = FactIndexerFactory
			.createConcurrentSetFactIndexer();
	private final List<StratumEvaluator> stratumEvals = new ArrayList<>();

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void initialize(Set<Clause> program) throws DatalogValidationException {
		UnstratifiedProgram prog = (new DatalogValidator()).withBinaryDisunificationInRuleBody()
				.withBinaryUnificationInRuleBody().withAtomNegationInRuleBody().validate(program);
		StratifiedProgram stratProg = StratifiedNegationValidator.validate(prog);
		List<Set<PredicateSym>> strata = stratProg.getStrata();
		int nstrata = strata.size();
		Map<PredicateSym, Set<SemiNaiveClause>>[] firstRoundRules = new HashMap[nstrata];
		Map<PredicateSym, Set<SemiNaiveClause>>[] laterRoundRules = new HashMap[nstrata];
		Set<PositiveAtom>[] initialIdbFacts = new HashSet[nstrata];
		for (int i = 0; i < nstrata; ++i) {
			firstRoundRules[i] = new HashMap<>();
			laterRoundRules[i] = new HashMap<>();
			initialIdbFacts[i] = new HashSet<>();
		}
		Map<PredicateSym, Integer> predToStratumMap = stratProg.getPredToStratumMap();

		HeadVisitor<Void, PredicateSym> getHeadPred = new HeadVisitor<Void, PredicateSym>() {

			@Override
			public PredicateSym visit(PositiveAtom atom, Void state) {
				return atom.getPred();
			}

		};
		for (ValidClause clause : prog.getRules()) {
			PredicateSym pred = clause.getHead().accept(getHeadPred, null);
			int stratum = predToStratumMap.get(pred);
			// Treat IDB predicates from earlier strata as EDB predicates.
			Set<PredicateSym> idbs = strata.get(stratum);
			PremiseVisitor<Boolean, Boolean> checkForIdbPred = (new PremiseVisitorBuilder<Boolean, Boolean>())
					.onPositiveAtom((atom, idb) -> idbs.contains(atom.getPred()) || idb).or((premise, idb) -> idb);
			SemiNaiveClauseAnnotator annotator = new SemiNaiveClauseAnnotator(idbs);
			boolean hasIdbPred = false;
			for (Premise c : clause.getBody()) {
				hasIdbPred = c.accept(checkForIdbPred, hasIdbPred);
			}
			for (SemiNaiveClause rule : annotator.annotate(clause)) {
				PredicateSym bodyPred = rule.getFirstAtom().getPred();
				if (hasIdbPred) {
					Utilities.getSetFromMap(laterRoundRules[stratum], bodyPred).add(rule);
				} else {
					Utilities.getSetFromMap(firstRoundRules[stratum], bodyPred).add(rule);
				}
			}

		}

		Set<PredicateSym> edbs = prog.getEdbPredicateSyms();
		for (PositiveAtom fact : prog.getInitialFacts()) {
			if (edbs.contains(fact.getPred())) {
				allFacts.add(fact);
			} else {
				initialIdbFacts[predToStratumMap.get(fact.getPred())].add(fact);
			}
		}

		for (int i = 0; i < nstrata; ++i) {
			stratumEvals.add(new StratumEvaluator(firstRoundRules[i], laterRoundRules[i], initialIdbFacts[i]));
		}
	}

	@Override
	public synchronized IndexableFactCollection eval() {
		for (StratumEvaluator se : stratumEvals) {
			se.eval();
		}
		return allFacts;
	}

	private class StratumEvaluator {
		private ConcurrentFactIndexer<Set<PositiveAtom>> idbsPrev = FactIndexerFactory
				.createConcurrentSetFactIndexer();
		private ConcurrentFactIndexer<Set<PositiveAtom>> deltaOld = FactIndexerFactory
				.createConcurrentSetFactIndexer();
		private ConcurrentFactIndexer<Set<PositiveAtom>> deltaNew = FactIndexerFactory
				.createConcurrentSetFactIndexer();
		private final Map<PredicateSym, Set<ClauseEvaluator>> firstRoundEvals;
		private final Map<PredicateSym, Set<ClauseEvaluator>> laterRoundEvals;
		private final Set<PositiveAtom> initialIdbFacts;

		public StratumEvaluator(Map<PredicateSym, Set<SemiNaiveClause>> firstRoundRules,
				Map<PredicateSym, Set<SemiNaiveClause>> laterRoundRules, Set<PositiveAtom> initialIdbFacts) {
			Function<Map<PredicateSym, Set<SemiNaiveClause>>, Map<PredicateSym, Set<ClauseEvaluator>>> translate = (
					clauseMap) -> {
				Map<PredicateSym, Set<ClauseEvaluator>> evalMap = new HashMap<>();
				for (Map.Entry<PredicateSym, Set<SemiNaiveClause>> entry : clauseMap.entrySet()) {
					Set<ClauseEvaluator> s = new HashSet<>();
					for (SemiNaiveClause cl : entry.getValue()) {
						s.add(new ClauseEvaluator(cl, this::addFact, this::getFacts));
					}
					evalMap.put(entry.getKey(), s);
				}
				return evalMap;
			};
			firstRoundEvals = translate.apply(firstRoundRules);
			laterRoundEvals = translate.apply(laterRoundRules);
			this.initialIdbFacts = initialIdbFacts;
		}

		public void eval() {
			deltaNew.addAll(this.initialIdbFacts);
			evalOneRound(allFacts, firstRoundEvals);
			while (evalOneRound(deltaOld, laterRoundEvals)) {
				// Loop...
			}
		}

		private boolean evalOneRound(FactIndexer index, Map<PredicateSym, Set<ClauseEvaluator>> rules) {
			for (PredicateSym pred : index.getPreds()) {
				Set<ClauseEvaluator> evals = rules.get(pred);
				if (evals != null) {
					for (ClauseEvaluator eval : evals) {
						for (PositiveAtom fact : index.indexInto(pred)) {
							eval.evaluate(fact);
						}
					}
				}
			}

			if (deltaNew.isEmpty()) {
				return false;
			}

			idbsPrev.addAll(deltaOld);
			allFacts.addAll(deltaNew);
			deltaOld = deltaNew;
			deltaNew = FactIndexerFactory.createConcurrentSetFactIndexer();
			return true;
		}

		private boolean addFact(PositiveAtom fact, ClauseSubstitution subst) {
			fact = fact.applySubst(subst);
			Set<PositiveAtom> set = allFacts.indexInto(fact);
			if (!set.contains(fact)) {
				deltaNew.add(fact);
				return true;
			}
			return false;
		}

		private Iterable<PositiveAtom> getFacts(AnnotatedAtom atom, ClauseSubstitution subst) {
			Set<PositiveAtom> r = null;
			PositiveAtom unannotated = atom.asUnannotatedAtom();
			switch (atom.getAnnotation()) {
			case EDB:
				// Fall through...
			case IDB:
				r = allFacts.indexInto(unannotated, subst);
				break;
			case IDB_PREV:
				r = idbsPrev.indexInto(unannotated, subst);
				break;
			case DELTA:
				r = deltaOld.indexInto(unannotated, subst);
				break;
			default:
				assert false;
			}
			return r;
		}
	}

}
