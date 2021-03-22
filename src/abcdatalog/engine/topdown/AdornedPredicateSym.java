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

import java.util.List;

import abcdatalog.ast.PredicateSym;

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
