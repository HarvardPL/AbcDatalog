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
package abcdatalog.util.substitution;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import abcdatalog.ast.Constant;
import abcdatalog.ast.Term;
import abcdatalog.ast.TermHelpers;
import abcdatalog.ast.Variable;

/**
 * A mapping from variables to constants.
 *
 */
public class SimpleConstSubstitution implements ConstOnlySubstitution {
	/**
	 * The map that represents the substitution.
	 */
	private final Map<Variable, Constant> subst;

	/**
	 * Constructs an empty substitution.
	 */
	public SimpleConstSubstitution() {
		this.subst = new LinkedHashMap<>();
	}

	/**
	 * Constructs a copy of another substitution.
	 * 
	 * @param other
	 *            the other substitution
	 */
	public SimpleConstSubstitution(SimpleConstSubstitution other) {
		this.subst = new LinkedHashMap<>(other.subst);
	}

	/**
	 * Returns the constant a variable is mapped to in this substitution.
	 * 
	 * @param v
	 *            the variable
	 * @return the constant, or null if v is not mapped
	 */
	@Override
	public Constant get(Variable v) {
		return this.subst.get(v);
	}

	/**
	 * Adds a mapping from a variable to a constant to this substitution.
	 * 
	 * @param v
	 *            the variable
	 * @param c
	 *            the constant
	 * @throws IllegalArgumentException
	 *             if v is already mapped to another constant
	 */
	public void put(Variable v, Constant c) {
		Constant val = this.subst.get(v);
		if (val != null && !val.equals(c)) {
			throw new IllegalArgumentException(
					"Cannot remap a variable to another constant.");
		}
		this.subst.put(v, c);
	}

	/**
	 * Creates a substitution from unifying two lists of terms, the second of
	 * which must be ground (i.e., contain no variables).
	 * 
	 * @param xs
	 *            the first list
	 * @param ys
	 *            the second list, which must be ground
	 * @return the substitution, or null if the unification is not possible
	 * @throws IllegalArgumentException
	 *             if the second list of terms is not ground
	 */
	public static SimpleConstSubstitution unify(Term[] xs, Term[] ys) {
		if (xs.length != ys.length) {
			return null;
		}

		SimpleConstSubstitution r = new SimpleConstSubstitution();

		for (int i = 0; i < xs.length; ++i) {
			Term x = xs[i];
			Term y = ys[i];
			if (!TermHelpers.unify(x, y, r)) {
				return null;
			}
		}
		return r;
	}

	@Override
	public Term[] apply(Term[] terms) {
		Term[] newTerms = new Term[terms.length];
		for (int i = 0; i < terms.length; ++i) {
			Term t = terms[i];
			if (t instanceof Variable) {
				Constant s = this.subst.get(t);
				if (s != null) {
					t = s;
				}
			}
			newTerms[i] = t;
		}
		return newTerms;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (Iterator<Variable> it = this.subst.keySet().iterator(); it
				.hasNext();) {
			Variable v = it.next();
			sb.append(v + "->" + subst.get(v));
			if (it.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public boolean add(Variable x, Constant c) {
		Constant c1 = get(x);
		if (c1 != null && !c.equals(c1)) {
			return false;
		}
		this.subst.put(x, c);
		return true;
	}

}
