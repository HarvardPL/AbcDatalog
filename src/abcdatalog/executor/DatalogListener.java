package abcdatalog.executor;

import abcdatalog.ast.PositiveAtom;

/**
 * A callback that is registered with a Datalog executor and is invoked during
 * evaluation.
 *
 */
public interface DatalogListener {
	/**
	 * Is invoked when a relevant new fact is derived during Datalog evaluation.
	 * Note that fact.isGround() will be true (i.e., a fact is a ground atom).
	 * 
	 * @param fact
	 *            the new fact
	 */
	void newFactDerived(PositiveAtom fact);
}
