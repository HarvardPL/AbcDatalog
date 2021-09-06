package edu.harvard.seas.pl.abcdatalog.engine.topdown;

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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidationException;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator;
import edu.harvard.seas.pl.abcdatalog.ast.validation.UnstratifiedProgram;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator.ValidClause;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.HeadVisitor;
import edu.harvard.seas.pl.abcdatalog.engine.DatalogEngine;
import edu.harvard.seas.pl.abcdatalog.util.Utilities;

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
