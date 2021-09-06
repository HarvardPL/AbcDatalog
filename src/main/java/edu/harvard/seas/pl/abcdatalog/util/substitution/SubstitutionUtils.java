package edu.harvard.seas.pl.abcdatalog.util.substitution;

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
