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

import abcdatalog.ast.Term;

/**
 * An adorned atom (i.e., an atom where every argument is marked as either bound
 * or free).
 *
 */
public class AdornedAtom {
	private final AdornedPredicateSym pred;
	private final Term[] args;
	
	/**
	 * Constructs an adorned atom with the given predicate symbol and arguments.
	 * 
	 * @param pred
	 *            adorned predicate symbol
	 * @param args
	 *            arguments
	 */
	public AdornedAtom(AdornedPredicateSym pred, Term[] args) {
		this.pred = pred;
		this.args = args;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(pred);
		if (this.args.length > 0) {
			sb.append("(");
			for (int i = 0; i < args.length; ++i) {
				sb.append(args[i]);
				if (i < args.length - 1) {
					sb.append(", ");
				}
			}
			sb.append(")");
		}
		return sb.toString();
	}
	
	public AdornedPredicateSym getPred() {
		return pred;
	}
	
	public Term[] getArgs() {
		return args;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((args == null) ? 0 : args.hashCode());
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
		AdornedAtom other = (AdornedAtom) obj;
		if (args == null) {
			if (other.args != null)
				return false;
		} else if (!args.equals(other.args))
			return false;
		if (pred == null) {
			if (other.pred != null)
				return false;
		} else if (!pred.equals(other.pred))
			return false;
		return true;
	}

}
