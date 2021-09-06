package edu.harvard.seas.pl.abcdatalog.engine.bottomup;

import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;

public interface EvalManagerWithProvenance extends EvalManager {

	/**
	 * Return the last rule used in the justification of the given atom. The
	 * returned rule should be ground (variable-free). Return null if the given atom
	 * is not a fact or was not derived.
	 * 
	 * @param fact the fact
	 * @return the last rule used to justify that fact
	 */
	Clause getJustification(PositiveAtom fact);

}
