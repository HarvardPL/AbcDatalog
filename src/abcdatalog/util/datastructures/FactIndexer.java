package abcdatalog.util.datastructures;

import abcdatalog.ast.PositiveAtom;

public interface FactIndexer extends IndexableFactCollection {
	/**
	 * Adds a fact to the FactIndexer.
	 * 
	 * @param fact
	 *            a fact
	 */
	public void add(PositiveAtom fact);

	/**
	 * Adds some number of facts to the FactIndexer.
	 * 
	 * @param facts
	 *            some facts
	 */
	public void addAll(Iterable<PositiveAtom> facts);
}
