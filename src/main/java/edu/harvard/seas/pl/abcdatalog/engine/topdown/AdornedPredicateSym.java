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

import java.util.List;

import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;

/**
 * An adorned predicate symbol. Each argument of any atom formed from this
 * predicate symbol will be marked as bound or free in accordance with the
 * predicate's adornment.
 *
 */
public final class AdornedPredicateSym {
	/**
	 * The unadorned predicate symbol.
	 */
	private PredicateSym pred;
	/**
	 * The adornment of the predicate symbol. A true value means that the
	 * corresponding term in the arguments of an atom formed from this predicate
	 * symbol is considered bound; a false value implies that the argument is
	 * free.
	 */
	private final List<Boolean> adornment;
	/**
	 * Total number of bound terms in the adornment.
	 */
	private final int bound;

	/**
	 * Constructs an adorned predicate symbol from a predicate symbol and an
	 * adornment.
	 * 
	 * @param p
	 *            predicate symbol
	 * @param adornment
	 *            adornment
	 */
	public AdornedPredicateSym(PredicateSym p, List<Boolean> adornment) {
		this.pred = p;
		if (adornment.size() != p.getArity()) {
			throw new IllegalArgumentException("Predicate symbol \"" + p.getSym()
					+ "\" has an arity of " + p.getArity()
					+ " but supplied an adornment of length " + adornment + ".");
		}
		this.adornment = adornment;

		int n = 0;
		for (int i = 0; i < adornment.size(); ++i) {
			if (adornment.get(i)) {
				++n;
			}
		}
		this.bound = n;
	}
	
	public String getSym() {
		return this.pred.getSym();
	}

	public int getArity() {
		return this.pred.getArity();
	}

	/**
	 * Creates a new predicate symbol that has the same symbol and arity as this
	 * one, but no adornment.
	 * 
	 * @return the unadorned predicate symbol
	 */
	public PredicateSym getUnadorned() {
		return this.pred;
	}
	
	/**
	 * Returns the adornment for this predicate symbol.
	 * 
	 * @return the adornment
	 */
	public List<Boolean> getAdornment() {
		return adornment;
	}

	/**
	 * Returns the number of arguments bound in the adornment of this predicate
	 * symbol.
	 * 
	 * @return the number of bound arguments
	 */
	public int getBound() {
		return bound;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(pred + "<");
		for (Boolean b : this.adornment) {
			sb.append((b) ? "b" : "f");
		}
		sb.append(">");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((getAdornment() == null) ? 0 : getAdornment().hashCode());
		result = prime * result + getBound();
		result = prime * result + ((pred == null) ? 0 : pred.hashCode());
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
		AdornedPredicateSym other = (AdornedPredicateSym) obj;
		if (getAdornment() == null) {
			if (other.getAdornment() != null)
				return false;
		} else if (!getAdornment().equals(other.getAdornment()))
			return false;
		if (getBound() != other.getBound())
			return false;
		if (pred == null) {
			if (other.pred != null)
				return false;
		} else if (!pred.equals(other.pred))
			return false;
		return true;
	}

}
