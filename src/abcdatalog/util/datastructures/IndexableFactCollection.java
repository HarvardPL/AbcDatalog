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
package abcdatalog.util.datastructures;

import java.util.Set;

import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.util.substitution.ConstOnlySubstitution;

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
