package edu.harvard.seas.pl.abcdatalog.util.datastructures;

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

import java.util.Set;

import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.util.substitution.ConstOnlySubstitution;

/**
 * A fixed collection of facts that only allows a basic query operation: return
 * the atoms in the collection that might match a given atom.
 *
 */
public interface IndexableFactCollection {
	/**
	 * Returns the atoms in the collection that potentially "match" the provided
	 * atom. There is no guarantee that the returned atoms can actually be
	 * unified with the provided atom.
	 * 
	 * @param atom
	 *            the atom to match
	 * @return the matching facts
	 */
	public Iterable<PositiveAtom> indexInto(PositiveAtom atom);

	/**
	 * Returns the atoms in the collection that potentially "match" the provided
	 * atom, after the given substitution has been applied. There is no
	 * guarantee that the returned atoms can actually be unified with the
	 * provided atom.
	 * 
	 * @param atom
	 *            the atom to match
	 * @return the matching facts
	 */
	public Iterable<PositiveAtom> indexInto(PositiveAtom atom,
			ConstOnlySubstitution subst);

	/**
	 * Returns the atoms in the collection with the given predicate symbol.
	 * 
	 * @param pred
	 *            the predicate symbol
	 * @return the matching facts
	 */
	public Iterable<PositiveAtom> indexInto(PredicateSym pred);

	/**
	 * Returns whether the collection is empty.
	 * 
	 * @return whether the collection is empty
	 */
	public boolean isEmpty();

	/**
	 * Returns the set of the predicate symbols represented in this collection.
	 * 
	 * @return the predicate symbols
	 */
	Set<PredicateSym> getPreds();
}
