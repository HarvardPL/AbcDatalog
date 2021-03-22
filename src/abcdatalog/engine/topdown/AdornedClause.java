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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import abcdatalog.ast.Constant;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.Premise;
import abcdatalog.ast.Term;
import abcdatalog.ast.Variable;
import abcdatalog.ast.validation.DatalogValidator.ValidClause;
import abcdatalog.ast.visitors.HeadVisitor;
import abcdatalog.ast.visitors.PremiseVisitor;
import abcdatalog.ast.visitors.PremiseVisitorBuilder;
import abcdatalog.ast.visitors.TermVisitor;

/**
 * An adorned clause (i.e., a Horn clause where every atom is itself adorned).
 *
 */
public final class AdornedClause {
	private final AdornedAtom head;
	private final List<AdornedAtom> body;

	/**
	 * Constructs an adorned clause given an adorned atom for the head and a
	 * list of adorned atoms for the body.
	 * 
	 * @param head
	 *            head atom of clause
	 * @param body
	 *            atoms for body of clause
	 */
	public AdornedClause(AdornedAtom head, List<AdornedAtom> body) {
		this.head = head;
		this.body = body;
	}

	/**
	 * Constructs an adorned clause given an adornment to apply to the head and
	 * clause to adorn. The head adornment ripples left to right across the
	 * atoms in the body.
	 * 
	 * @param headAdornment
	 *            adornment for head atom. A true value implies that that term
	 *            is bound, false implies free.
	 * @param clause
	 *            original clause
	 */
	public static AdornedClause fromClause(List<Boolean> headAdornment, ValidClause clause) {
		HeadVisitor<Void, PositiveAtom> getHead = new HeadVisitor<Void, PositiveAtom>() {

			@Override
			public PositiveAtom visit(PositiveAtom atom, Void state) {
				return atom;
			}

		};
		PositiveAtom head = clause.getHead().accept(getHead, null);
		if (headAdornment.size() != head.getPred().getArity()) {
			throw new IllegalArgumentException("Adornment of size " + headAdornment.size()
					+ " given for a clause with a head of arity " + head.getPred().getArity() + ".");
		}

		// Determine which variables in head are bound.
		Set<Term> bound = new HashSet<>();
		Term[] args = head.getArgs();
		for (int i = 0; i < args.length; ++i) {
			if (headAdornment.get(i)) {
				bound.add(args[i]);
			}
		}

		// Determine adornment of each atom in body of rule, updating
		// binding information at each step.
		PremiseVisitor<Void, PositiveAtom> getBodyAtom = (new PremiseVisitorBuilder<Void, PositiveAtom>())
				.onPositiveAtom((atom, nothing) -> atom).orCrash();
		List<PositiveAtom> body = new ArrayList<>();
		for (Premise c : clause.getBody()) {
			body.add(c.accept(getBodyAtom, null));
		}
		List<AdornedAtom> newAtoms = new ArrayList<>();
		for (PositiveAtom a : body) {
			Set<Term> newBound = new HashSet<>();
			List<Boolean> adornment = new ArrayList<>();
			TermVisitor<Void, Void> tv = new TermVisitor<Void, Void>() {

				@Override
				public Void visit(Variable t, Void state) {
					if (bound.contains(t)) {
						adornment.add(true);
					} else {
						adornment.add(false);
						newBound.add(t);
					}
					return null;
				}

				@Override
				public Void visit(Constant t, Void state) {
					adornment.add(true);
					return null;
				}
				
			};
			for (Term t : a.getArgs()) {
				t.accept(tv, null);
			}
			newAtoms.add(new AdornedAtom(new AdornedPredicateSym(a.getPred(), adornment), a.getArgs()));
			bound.addAll(newBound);
		}

		AdornedAtom newHead = new AdornedAtom(new AdornedPredicateSym(head.getPred(), headAdornment), args);
		return new AdornedClause(newHead, newAtoms);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(head);
		if (!body.isEmpty()) {
			sb.append(" :- ");
			for (int i = 0; i < body.size(); ++i) {
				sb.append(body.get(i));
				if (i < this.body.size() - 1) {
					sb.append(", ");
				}
			}
		}
		sb.append('.');
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result + ((head == null) ? 0 : head.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AdornedClause other = (AdornedClause) obj;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		if (head == null) {
			if (other.head != null)
				return false;
		} else if (!head.equals(other.head))
			return false;
		return true;
	}

	public List<AdornedAtom> getBody() {
		return body;
	}
	
	public AdornedAtom getHead() {
		return head;
	}

}
