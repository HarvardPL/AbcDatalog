package edu.harvard.seas.pl.abcdatalog.ast;

/**
 * A utility class for accessing the head of a clause.
 *
 */
public final class HeadHelpers {

	private HeadHelpers() {
		// Cannot be instantiated.
	}

	public static PositiveAtom forcePositiveAtom(Head head) {
		return (PositiveAtom) head;
	}
}
