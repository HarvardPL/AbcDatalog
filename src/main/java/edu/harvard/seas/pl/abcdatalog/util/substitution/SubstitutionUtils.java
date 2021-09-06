package edu.harvard.seas.pl.abcdatalog.util.substitution;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.Head;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.Premise;

public final class SubstitutionUtils {

	private SubstitutionUtils() {
		throw new AssertionError("impossible");
	}

	/**
	 * Apply a substitution to the given positive atoms, adding the resulting atoms
	 * to the provided collection (in order).
	 * 
	 * @param subst the substitution
	 * @param atoms the atoms
	 * @param acc   the collection to add the atoms to
	 */
	public static void applyToPositiveAtoms(Substitution subst, Iterable<PositiveAtom> atoms,
			Collection<PositiveAtom> acc) {
		for (PositiveAtom atom : atoms) {
			acc.add(atom.applySubst(subst));
		}
	}

	/**
	 * Apply a substitution to the given positive atoms, returning a list of the
	 * resulting atoms (in order).
	 * 
	 * @param subst the substitution
	 * @param atoms the atoms
	 * @return a list of the atoms that result from applying the substitution
	 */
	public static List<PositiveAtom> applyToPositiveAtoms(Substitution subst, Iterable<PositiveAtom> atoms) {
		List<PositiveAtom> ret = new ArrayList<>();
		applyToPositiveAtoms(subst, atoms, ret);
		return ret;
	}

	public static Clause applyToClause(Substitution subst, Clause cl) {
		Head newHead = cl.getHead().applySubst(subst);
		List<Premise> newBody = new ArrayList<>();
		for (Premise p : cl.getBody()) {
			newBody.add(p.applySubst(subst));
		}
		return new Clause(newHead, newBody);
	}

}
