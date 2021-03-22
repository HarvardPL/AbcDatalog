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
package abcdatalog.engine.topdown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import abcdatalog.ast.Clause;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.ast.Term;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.ast.validation.DatalogValidator;
import abcdatalog.ast.validation.DatalogValidator.ValidClause;
import abcdatalog.ast.validation.UnstratifiedProgram;
import abcdatalog.ast.visitors.HeadVisitor;
import abcdatalog.engine.DatalogEngine;
import abcdatalog.util.Utilities;

/**
 * A Datalog engine that uses a variant of the query-subquery evaluation method.
 *
 */
public abstract class AbstractQsqEngine implements DatalogEngine {
	/**
	 * EDB facts mapped by predicate symbol.
	 */
	protected final Map<PredicateSym, Relation> edbRelations = new HashMap<>();
	/**
	 * Rules for deriving IDB facts mapped by predicate symbol.
	 */
	protected final Map<PredicateSym, Set<ValidClause>> idbRules = new HashMap<>();

	@Override
	public void init(Set<Clause> program) throws DatalogValidationException {
		UnstratifiedProgram prog = (new DatalogValidator()).validate(program, true);
		for (PredicateSym p : prog.getEdbPredicateSyms()) {
			edbRelations.put(p, new Relation(p.getArity()));
		}
		HeadVisitor<Void, PredicateSym> getHeadPred = new HeadVisitor<Void, PredicateSym>() {

			@Override
			public PredicateSym visit(PositiveAtom atom, Void state) {
				return atom.getPred();
			}

		};
		
		for (ValidClause c : prog.getRules()) {
			PredicateSym pred = c.getHead().accept(getHeadPred, null);
			Utilities.getSetFromMap(idbRules, pred).add(c);
		}
		for (PositiveAtom fact : prog.getInitialFacts()) {
			edbRelations.get(fact.getPred()).add(new Tuple(fact.getArgs()));
		}
	}

	@Override
	public abstract Set<PositiveAtom> query(PositiveAtom q);

	/**
	 * Creates a new tuple that is like t except the bound terms of t have been
	 * replaced by the terms in input.
	 * 
	 * @param t
	 * @param adornment
	 *            specifies bound terms of t
	 * @param input
	 * @return new tuple
	 */
	protected Tuple applyBoundArgs(Tuple t, List<Boolean> adornment, Tuple input) {
		assert t.size() == adornment.size();

		List<Term> newTerms = new ArrayList<>();
		int bound = 0;
		int j = 0; // Indexes into input.
		for (int i = 0; i < t.size(); ++i) {
			if (adornment.get(i)) {
				newTerms.add(input.get(j++));
				++bound;
			} else {
				newTerms.add(t.get(i));
			}
		}

		assert bound == input.size();
		return new Tuple(newTerms);
	}

	/**
	 * If query is for an EDB relation, returns facts that unify with that
	 * query.
	 * 
	 * @param q
	 *            query
	 * @return facts, or null if query is for an IDB relation
	 */
	protected Set<PositiveAtom> checkIfEdbQuery(PositiveAtom q) {
		Relation facts = this.edbRelations.get(q.getPred());
		if (facts != null) {
			Set<PositiveAtom> result = new LinkedHashSet<>();
			for (Tuple fact : facts) {
				if (fact.unify(new Tuple(q.getArgs())) != null) {
					result.add(PositiveAtom.create(q.getPred(), fact.elts));
				}
			}
			return result;
		}
		return null;
	}

}
