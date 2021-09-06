package edu.harvard.seas.pl.abcdatalog.engine;

import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;

/**
 * A Datalog evaluation engine that retains fact provenance. Datalog engines are
 * initialized with a set of clauses that represent initial facts and rules that
 * can be used to derive new facts. After initialization, clients can query
 * about whether certain facts are derivable.
 * 
 * This engine also supports why-provenance queries; that is, querying for the
 * justification for why a fact was derived.
 *
 */
public interface DatalogEngineWithProvenance extends DatalogEngine {

	/**
	 * Return the last rule used in the justification of the given atom. The
	 * returned rule should be ground (variable-free). Return null if the given atom
	 * is not a fact or was not derived. <br>
	 * <br>
	 * A client can recursively invoke this method on the premises of the returned
	 * clause to build a provenance tree.
	 * 
	 * @param fact the fact
	 * @return the last rule used to justify that fact
	 */
	Clause getJustification(PositiveAtom fact);

}
