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

import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.util.substitution.Substitution;

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

	@Override
	public Premise applySubst(Substitution subst) {
		return new NegatedAtom(atom.applySubst(subst));
	}

}
