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
package abcdatalog.engine.bottomup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import abcdatalog.ast.BinaryDisunifier;
import abcdatalog.ast.BinaryUnifier;
import abcdatalog.ast.Clause;
import abcdatalog.ast.Constant;
import abcdatalog.ast.Head;
import abcdatalog.ast.NegatedAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.ast.Premise;
import abcdatalog.ast.Term;
import abcdatalog.ast.Variable;
import abcdatalog.ast.validation.DatalogValidator.ValidClause;
import abcdatalog.ast.visitors.CrashPremiseVisitor;
import abcdatalog.ast.visitors.DefaultConjunctVisitor;
import abcdatalog.ast.visitors.PremiseVisitor;
import abcdatalog.ast.visitors.PremiseVisitorBuilder;
import abcdatalog.util.Box;

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
