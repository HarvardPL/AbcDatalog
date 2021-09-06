package edu.harvard.seas.pl.abcdatalog.engine.bottomup.sequential;

/*-
 * #%L
 * AbcDatalog
 * %%
 * Copyright (C) 2016 - 2021 President and Fellows of Harvard College
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the President and Fellows of Harvard College nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import edu.harvard.seas.pl.abcdatalog.ast.BinaryDisunifier;
import edu.harvard.seas.pl.abcdatalog.ast.BinaryUnifier;
import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.NegatedAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.Premise;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidationException;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator;
import edu.harvard.seas.pl.abcdatalog.ast.validation.StratifiedNegationValidator;
import edu.harvard.seas.pl.abcdatalog.ast.validation.StratifiedProgram;
import edu.harvard.seas.pl.abcdatalog.ast.validation.UnstratifiedProgram;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator.ValidClause;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.HeadVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitorBuilder;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.AnnotatedAtom;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.ClauseEvaluator;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.EvalManagerWithProvenance;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator.SemiNaiveClause;
import edu.harvard.seas.pl.abcdatalog.util.Utilities;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.ConcurrentFactIndexer;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.FactIndexer;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.FactIndexerFactory;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.IndexableFactCollection;
import edu.harvard.seas.pl.abcdatalog.util.substitution.ClauseSubstitution;
import edu.harvard.seas.pl.abcdatalog.util.substitution.SubstitutionUtils;

public class SemiNaiveEvalManager implements EvalManagerWithProvenance {
	private final ConcurrentFactIndexer<Set<PositiveAtom>> allFacts = FactIndexerFactory
			.createConcurrentSetFactIndexer();
	private final List<StratumEvaluator> stratumEvals = new ArrayList<>();
	private final boolean collectProv;
	private final ConcurrentHashMap<PositiveAtom, Clause> justifications = new ConcurrentHashMap<>();
	
	public SemiNaiveEvalManager(boolean collectProv) {
		this.collectProv = collectProv;
	}

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
			if (collectProv) {
				justifications.put(fact, new Clause(fact, Collections.emptyList()));
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
	
	@Override
	public Clause getJustification(PositiveAtom fact) {
		return justifications.get(fact);
	}
	
	public static Clause stripSemiNaiveClause(SemiNaiveClause cl) {
		List<Premise> newBody = new ArrayList<>();
		for (Premise p : cl.getBody()) {
			newBody.add(p.accept(new PremiseVisitor<Void, Premise>() {

				@Override
				public Premise visit(PositiveAtom atom, Void state) {
					return atom;
				}

				@Override
				public Premise visit(AnnotatedAtom atom, Void state) {
					return atom.asUnannotatedAtom();
				}

				@Override
				public Premise visit(BinaryUnifier u, Void state) {
					return u;
				}

				@Override
				public Premise visit(BinaryDisunifier u, Void state) {
					return u;
				}

				@Override
				public Premise visit(NegatedAtom atom, Void state) {
					return atom;
				}
				
			}, null));
		}
		return new Clause(cl.getHead(), newBody);
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
						Clause stripped = stripSemiNaiveClause(cl);
						s.add(new ClauseEvaluator(cl, (fact, subst) -> addFact(fact, subst, stripped), this::getFacts));
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

		private boolean addFact(PositiveAtom fact, ClauseSubstitution subst, Clause stripped) {
			fact = fact.applySubst(subst);
			Set<PositiveAtom> set = allFacts.indexInto(fact);
			if (!set.contains(fact)) {
				deltaNew.add(fact);
				if (collectProv) {
					justifications.put(fact, SubstitutionUtils.applyToClause(subst, stripped));
				}
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
