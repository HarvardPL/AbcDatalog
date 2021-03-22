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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import abcdatalog.ast.Constant;
import abcdatalog.ast.Term;
import abcdatalog.ast.Variable;

/**
 * A mapping from variables to terms. Implemented using a union-find data
 * structure.
 * 
 */
public class UnionFindBasedUnifier implements TermUnifier {
	/**
	 * Map implementation of a union-find (also known as a disjoint-set) data
	 * structure. This represents a substitution in that a variable is treated
	 * as bound to the representative element of the set it is in. If there is a
	 * constant in the set, the representative element will be a constant.
	 */
	private final Map<Term, Term> uf;

	/**
	 * Constructs an empty substitution.
	 */
	public UnionFindBasedUnifier() {
		this.uf = new LinkedHashMap<>();
	}

	/**
	 * Constructs a substitution from another substitution.
	 * 
	 * @param other
	 *            the other substitution
	 */
	public UnionFindBasedUnifier(UnionFindBasedUnifier other) {
		this.uf = new LinkedHashMap<>(other.uf);
	}

	/**
	 * Retrieves the mapping of a variable.
	 * 
	 * @param x
	 *            the variable
	 * @return the term that the variable is bound to, or null if the variable
	 *         is not in the substitution
	 */
	@Override
	public Term get(Variable x) {
		return this.find(x);
	}

	@Override
	public Term[] apply(Term[] original) {
		Term[] r = new Term[original.length];
		for (int i = 0; i < original.length; ++i) {
			Term t = original[i];
			if (t instanceof Variable) {
				Term s = this.get((Variable) t);
				if (s != null) {
					t = s;
				}
			}
			r[i] = t;
		}
		return r;
	}

	/**
	 * Creates a substitution from unifying two lists of terms.
	 * 
	 * @param xs
	 *            the first list
	 * @param ys
	 *            the second list
	 * @return the substitution, or null if the two lists do not unify
	 */
	public static UnionFindBasedUnifier fromTerms(List<Term> xs, List<Term> ys) {
		// Lists of different sizes cannot be unified.
		if (xs.size() != ys.size()) {
			return null;
		}

		// Initialize union-find data structure so that each term from both
		// lists is in a singleton set.
		UnionFindBasedUnifier r = new UnionFindBasedUnifier();
		for (Term x : xs) {
			r.uf.put(x, x);
		}
		for (Term y : ys) {
			r.uf.put(y, y);
		}

		// Generate substitution by iterating through both atoms concurrently.
		Iterator<Term> xiter = xs.iterator();
		Iterator<Term> yiter = ys.iterator();
		while (xiter.hasNext()) {
			Term x = xiter.next();
			Term y = yiter.next();

			Term xroot = r.find(x);
			Term yroot = r.find(y);

			// Already in same set.
			if (xroot == yroot)
				continue;

			// Two constants cannot be unified.
			if (xroot instanceof Constant && yroot instanceof Constant) {
				return null;
			}

			r.union(xroot, yroot);
		}

		return r;
	}
	
	/**
	 * Creates a substitution from unifying two arrays of terms.
	 * 
	 * @param xs
	 *            the first array
	 * @param ys
	 *            the second array
	 * @return the substitution, or null if the two lists do not unify
	 */
	public static Substitution fromTerms(Term[] elts, Term[] elts2) {
		// TODO this is inefficient
		return fromTerms(Arrays.asList(elts), Arrays.asList(elts2));
	}

	/**
	 * Adds a mapping from a variable to a term. Throws an
	 * IllegalArgumentException if doing so would result in a variable mapping
	 * to multiple constants.
	 * 
	 * @param v
	 *            the variable
	 * @param t
	 *            the term
	 * @throws IllegalArgumentException
	 *             If the variable is already mapped to a different term
	 */
	public void put(Variable v, Term t) {
		Term vroot = this.find(v);
		if (vroot == null) {
			vroot = v;
		}

		Term troot = this.find(t);
		if (troot == null) {
			troot = t;
		}

		if (vroot.equals(troot))
			return;

		if (vroot instanceof Constant && troot instanceof Constant) {
			throw new IllegalArgumentException("Variable " + v.getName()
					+ " already mapped to " + ((Constant) vroot).getName()
					+ "; cannot remap to " + ((Constant) troot).getName() + ".");
		}

		this.uf.put(v, vroot);
		this.uf.put(t, troot);
		union(v, t);
	}

	/**
	 * Unions the sets of two terms in the union-find data structure.
	 * 
	 * @param x
	 *            the first term
	 * @param y
	 *            the second term
	 */
	private void union(Term x, Term y) {
		Term xroot = this.find(x);
		Term yroot = this.find(y);
		assert xroot != null && yroot != null;

		// Keep constants at root of tree.
		if (xroot instanceof Constant) {
			this.uf.put(yroot, xroot);
		} else {
			this.uf.put(xroot, yroot);
		}
	}

	/**
	 * Retrieves the representative element of the set that contains a certain
	 * term in the union-find data structure.
	 * 
	 * @param x
	 *            the term
	 * @return the representative element, or null if the provided term is not
	 *         in the data structure
	 */
	private Term find(Term x) {
		Term child = x;
		Term parent = this.uf.get(child);
		if (parent == null) {
			return null;
		}

		// When the child is equal to the parent, we have reached the root.
		while (!child.equals(parent)) {
			Term grandparent = this.uf.get(parent);
			// Simple path compression.
			this.uf.put(child, grandparent);
			child = grandparent;
			parent = this.uf.get(child);
		}
		return parent;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (Iterator<Term> it = this.uf.keySet().iterator(); it.hasNext();) {
			Term key = it.next();
			if (key instanceof Variable) {
				sb.append(key + "->" + find(key));
				if (it.hasNext()) {
					sb.append(", ");
				}
			}
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public boolean unify(Variable u, Term v) {
		Term uroot = this.find(u);
		if (uroot == null) {
			uroot = u;
		}

		Term vroot = this.find(v);
		if (vroot == null) {
			vroot = v;
		}

		if (uroot.equals(vroot)) {
			return true;
		}
		
		if (vroot instanceof Constant && uroot instanceof Constant) {
			return false;
		}
		
		this.uf.put(v, vroot);
		this.uf.put(u, uroot);
		union(v, u);
		return true;
	}

}
