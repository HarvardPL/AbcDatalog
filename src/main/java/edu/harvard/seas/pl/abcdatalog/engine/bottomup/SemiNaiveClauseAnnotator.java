package edu.harvard.seas.pl.abcdatalog.engine.bottomup;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.harvard.seas.pl.abcdatalog.ast.BinaryDisunifier;
import edu.harvard.seas.pl.abcdatalog.ast.BinaryUnifier;
import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.Constant;
import edu.harvard.seas.pl.abcdatalog.ast.Head;
import edu.harvard.seas.pl.abcdatalog.ast.NegatedAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.Premise;
import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.ast.Variable;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator.ValidClause;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.CrashPremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.DefaultConjunctVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitorBuilder;
import edu.harvard.seas.pl.abcdatalog.util.Box;

/**
 * A class for annotating a clause with annotations helpful for semi-naive
 * evaluation.
 *
 */
public class SemiNaiveClauseAnnotator {
	private final Set<PredicateSym> idbPreds;

	public SemiNaiveClauseAnnotator(Set<PredicateSym> idbPreds) {
		this.idbPreds = idbPreds;
	}

	/**
	 * Returns a set of annotated clauses for a given unannotated clause. If the
	 * given clause only contains atoms with EDB predicate symbols, the
	 * resulting set will be a singleton. Otherwise, the cardinality of the
	 * return set will be equal to the number of atoms in the body of the clause
	 * that have IDB predicate symbols. Each returned clause is ordered so that
	 * it can be evaluated from left to right.
	 * 
	 * @param original
	 *            the unannotated clause
	 * @return a set of annotated clauses
	 */
	public Set<SemiNaiveClause> annotate(ValidClause original) {
		List<Premise> body = original.getBody();
		if (body.isEmpty()) {
			throw new IllegalArgumentException("Cannot annotate a bodiless clause.");
		}
		List<Premise> body2 = new ArrayList<>();
		List<Integer> idbPositions = new ArrayList<>();
		Box<Integer> edbPos = new Box<>();
		PremiseVisitor<Integer, Void> findIdbs = (new PremiseVisitorBuilder<Integer, Void>())
				.onPositiveAtom((atom, pos) -> {
					if (idbPreds.contains(atom.getPred())) {
						idbPositions.add(pos);
						body2.add(atom);
					} else {
						if (edbPos.value == null) {
							edbPos.value = pos;
						}
						body2.add(new AnnotatedAtom(atom, AnnotatedAtom.Annotation.EDB));
					}
					return null;
				}).or((premise, ignore) -> {
					body2.add(premise);
					return null;
				});
		int pos = 0;
		for (Premise c : body) {
			c.accept(findIdbs, pos++);
		}
		body = body2;

		if (idbPositions.isEmpty()) {
			if (edbPos.value == null) {
				edbPos.value = 0;
			}
			return Collections.singleton(sort(new Clause(original.getHead(), body), edbPos.value));
		}

		Set<SemiNaiveClause> r = new HashSet<>();
		for (Integer i : idbPositions) {
			List<Premise> newBody = new ArrayList<>();
			PremiseVisitor<AnnotatedAtom.Annotation, Void> annotator = (new PremiseVisitorBuilder<AnnotatedAtom.Annotation, Void>())
					.onPositiveAtom((atom, anno) -> {
						newBody.add(new AnnotatedAtom(atom, anno));
						return null;
					}).or((premise, ignore) -> {
						newBody.add(premise);
						return null;
					});
			Iterator<Premise> it = body.iterator();
			for (int j = 0; j < i; ++j) {
				it.next().accept(annotator, AnnotatedAtom.Annotation.IDB);
			}
			it.next().accept(annotator, AnnotatedAtom.Annotation.DELTA);
			while (it.hasNext()) {
				it.next().accept(annotator, AnnotatedAtom.Annotation.IDB_PREV);
			}
			r.add(sort(new Clause(original.getHead(), newBody), i));
		}
		return r;
	}

	public Set<SemiNaiveClause> annotate(Set<ValidClause> clauses) {
		Set<SemiNaiveClause> r = new HashSet<>();
		for (ValidClause clause : clauses) {
			r.addAll(annotate(clause));
		}
		return r;
	}

	private static SemiNaiveClause sort(Clause original, int firstConjunctPos) {
		List<Premise> body = new ArrayList<>(original.getBody());
		if (body.isEmpty()) {
			return new SemiNaiveClause(original.getHead(), body);
		}
		PremiseVisitor<Set<Variable>, Void> boundVarUpdater = new DefaultConjunctVisitor<Set<Variable>, Void>() {
			@Override
			public Void visit(AnnotatedAtom atom, Set<Variable> boundVars) {
				for (Term t : atom.getArgs()) {
					if (t instanceof Variable) {
						boundVars.add((Variable) t);
					}
				}
				return null;
			}

			@Override
			public Void visit(BinaryUnifier u, Set<Variable> boundVars) {
				for (Term t : u.getArgsIterable()) {
					if (t instanceof Variable) {
						boundVars.add((Variable) t);
					}
				}
				return null;
			}
		};

		PremiseVisitor<Set<Variable>, Double> scorer = new CrashPremiseVisitor<Set<Variable>, Double>() {
			@Override
			public Double visit(AnnotatedAtom atom, Set<Variable> boundVars) {
				int count = 0, total = 0;
				for (Term t : atom.getArgs()) {
					if (t instanceof Constant || boundVars.contains(t)) {
						++count;
					}
					++total;
				}
				return (total == 0) ? 1.0 : count / total;
			}

			@Override
			public Double visit(BinaryUnifier u, Set<Variable> boundVars) {
				for (Term t : u.getArgsIterable()) {
					if (t instanceof Constant || boundVars.contains(t)) {
						return Double.POSITIVE_INFINITY;
					}
				}
				return Double.NEGATIVE_INFINITY;
			}

			@Override
			public Double visit(BinaryDisunifier u, Set<Variable> boundVars) {
				for (Term t : u.getArgsIterable()) {
					if (!(t instanceof Constant || boundVars.contains(t))) {
						return Double.NEGATIVE_INFINITY;
					}
				}
				return Double.POSITIVE_INFINITY;
			}

			@Override
			public Double visit(NegatedAtom atom, Set<Variable> boundVars) {
				for (Term t : atom.getArgs()) {
					if (!(t instanceof Constant || boundVars.contains(t))) {
						return Double.NEGATIVE_INFINITY;
					}
				}
				return Double.POSITIVE_INFINITY;
			}
		};

		Collections.swap(body, 0, firstConjunctPos);
		int size = body.size();
		Set<Variable> boundVars = new HashSet<>();
		for (int i = 1; i < size; ++i) {
			body.get(i - 1).accept(boundVarUpdater, boundVars);
			int bestPos = -1;
			double bestScore = Double.NEGATIVE_INFINITY;
			for (int j = i; j < size; ++j) {
				Double score = body.get(j).accept(scorer, boundVars);
				if (score > bestScore) {
					bestScore = score;
					bestPos = j;
				}
			}
			assert bestPos != -1;
			Collections.swap(body, i, bestPos);
		}
		
		return new SemiNaiveClause(original.getHead(), body);
	}

	public static final class SemiNaiveClause extends Clause {

		private SemiNaiveClause(Head head, List<Premise> body) {
			super(head, body);
			if (body.isEmpty()) {
				throw new IllegalArgumentException("Body must not be empty.");
			}
		}

		public AnnotatedAtom getFirstAtom() {
			assert body.get(0) instanceof AnnotatedAtom;
			return (AnnotatedAtom) body.get(0);
		}

	}

}
