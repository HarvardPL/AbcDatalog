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

import java.util.Arrays;
import java.util.List;

import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.util.substitution.Substitution;
import edu.harvard.seas.pl.abcdatalog.util.substitution.UnionFindBasedUnifier;

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
