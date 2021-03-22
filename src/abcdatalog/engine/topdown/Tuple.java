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

import java.util.Arrays;
import java.util.List;

import abcdatalog.ast.Term;
import abcdatalog.util.substitution.Substitution;
import abcdatalog.util.substitution.UnionFindBasedUnifier;

/**
 * A tuple of terms, i.e., an ordered list of fixed arity.
 *
 */
public class Tuple {
	/**
	 * The terms in this tuple.
	 */
	public final Term[] elts;

	/**
	 * Constructs a tuple from a list of terms.
	 * 
	 * @param elts
	 *            the list of terms
	 */
	public Tuple(List<Term> elts) {
		Term[] tmp = new Term[elts.size()];
		this.elts = elts.toArray(tmp);
	}
	
	public Tuple(Term[] elts) {
		this.elts = elts;
	}

	/**
	 * Returns the term at the ith position in this tuple (0-indexed).
	 * 
	 * @param i
	 *            the position
	 * @return the term
	 */
	public Term get(int i) {
		return this.elts[i];
	}

	/**
	 * Returns the arity of this tuple.
	 * 
	 * @return the arity
	 */
	public int size() {
		return this.elts.length;
	}

	/**
	 * Attempts to unify this tuple with another tuple.
	 * 
	 * @param other
	 *            the other tuple
	 * @return the substitution resulting from the unification, or null if the
	 *         unification fails
	 */
	public Tuple unify(Tuple other) {
		Substitution subst = UnionFindBasedUnifier.fromTerms(this.elts, other.elts);
		if (subst != null) {
			return new Tuple(subst.apply(this.elts));
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<");
		for (int i = 0; i < this.elts.length; ++i) {
			sb.append(elts[i]);
			if (i < this.elts.length - 1) {
				sb.append(", ");
			}
		}
		sb.append(">");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(elts);
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
		Tuple other = (Tuple) obj;
		if (!Arrays.equals(elts, other.elts))
			return false;
		return true;
	}

}
