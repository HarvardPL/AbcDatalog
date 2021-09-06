package abcdatalog.engine.bottomup;

import java.util.Set;

import abcdatalog.ast.Clause;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.util.datastructures.IndexableFactCollection;

/**
 * The saturating evaluation manager for a bottom-up Datalog evaluation engine.
 *
 */
public interface EvalManager {
	/**
	 * Initialize this manager with a program.
	 * 
	 * @param program
	 *            the program
	 * @throws DatalogValidationException
	 *             if the program is invalid
	 */
	void initialize(Set<Clause> program) throws DatalogValidationException;

	/**
	 * Saturate all facts derivable from the program with which this manager has
	 * been initialized.
	 * 
	 * @param program
	 *            the program
	 * @return the facts
	 */
	IndexableFactCollection eval();
}
