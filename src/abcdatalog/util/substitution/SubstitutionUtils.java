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
package abcdatalog.util.substitution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import abcdatalog.ast.PositiveAtom;

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

}
