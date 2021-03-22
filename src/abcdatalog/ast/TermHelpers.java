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
package abcdatalog.ast;

import abcdatalog.ast.visitors.TermVisitor;
import abcdatalog.util.substitution.ConstOnlySubstitution;
import abcdatalog.util.substitution.TermUnifier;

/**
 * A utility class for common operations on terms.
 *
 */
public final class TermHelpers {
	private TermHelpers() {
		
	}
	
	public static <T> T fold(Iterable<Term> terms, TermVisitor<T, T> tv, T init) {
		T acc = init;
		for (Term t : terms) {
			acc = t.accept(tv, acc);
		}
		return acc;
	}

	public static <T> T fold(Term[] terms, TermVisitor<T, T> tv, T init) {
		T acc = init;
		for (Term t : terms) {
			acc = t.accept(tv, acc);
		}
		return acc;
	}
	
	public static boolean unify(Term u, Term v, ConstOnlySubstitution s) {	
		if (u instanceof Variable) {
			Constant c = s.get((Variable) u);
			if (c != null) {
				u = c;
			}
		}
		
		if (v instanceof Variable) {
			Constant c = s.get((Variable) v);
			if (c != null) {
				v = c;
			}
		}
		
		boolean uVar = u instanceof Variable;
		boolean vVar = v instanceof Variable;

		if (uVar && vVar) {
			throw new IllegalArgumentException("Cannot unify two variables.");
		} else if (uVar) {
			assert v instanceof Constant;
			s.add((Variable) u, (Constant) v); 
		} else if (vVar) {
			assert u instanceof Constant;
			s.add((Variable) v, (Constant) u);
		} else {
			assert u instanceof Constant && v instanceof Constant;
			return u.equals(v);
		}
		return true;
	}
	
	public static boolean unify(Term u, Term v, TermUnifier s) {
		if (u instanceof Variable) {
			Term t = s.get((Variable) u);
			if (t != null) {
				u = t;
			}
		}
		
		if (v instanceof Variable) {
			Term t = s.get((Variable) v);
			if (t != null) {
				v = t;
			}
		}
		
		boolean uVar = u instanceof Variable;
		boolean vVar = v instanceof Variable;

		if (uVar) {
			return s.unify((Variable) u, v); 
		} else if (vVar) {
			assert u instanceof Constant;
			return s.unify((Variable) v, u);
		} else {
			assert u instanceof Constant && v instanceof Constant;
			return u.equals(v);
		}
	}
}
