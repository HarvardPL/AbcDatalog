package edu.harvard.seas.pl.abcdatalog.util.datastructures;

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
