package edu.harvard.seas.pl.abcdatalog.ast;

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

import edu.harvard.seas.pl.abcdatalog.ast.visitors.TermVisitor;
import edu.harvard.seas.pl.abcdatalog.util.substitution.ConstOnlySubstitution;
import edu.harvard.seas.pl.abcdatalog.util.substitution.TermUnifier;

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
