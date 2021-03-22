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

import abcdatalog.ast.visitors.PremiseVisitor;

/**
 * A negated atom. Typically a negated atom is considered to hold during
 * evaluation if the atom it negates is not provable.
 *
 */
public class NegatedAtom implements Premise {
	private final PositiveAtom atom;

	public NegatedAtom(PredicateSym pred, Term[] args) {
		this.atom = PositiveAtom.create(pred, args);
	}

	public NegatedAtom(PositiveAtom atom) {
		this.atom = atom;
	}

	@Override
	public <I, O> O accept(PremiseVisitor<I, O> visitor, I state) {
		return visitor.visit(this, state);
	}

	public Term[] getArgs() {
		return this.atom.getArgs();
	}

	public PredicateSym getPred() {
		return this.atom.getPred();
	}

	public boolean isGround() {
		return this.atom.isGround();
	}

	public PositiveAtom asPositiveAtom() {
		return this.atom;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((atom == null) ? 0 : atom.hashCode());
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
		NegatedAtom other = (NegatedAtom) obj;
		if (atom == null) {
			if (other.atom != null)
				return false;
		} else if (!atom.equals(other.atom))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "not " + atom;
	}

}
