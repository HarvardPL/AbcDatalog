package abcdatalog.engine;

import java.util.List;
import java.util.Set;

import abcdatalog.ast.Clause;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.util.substitution.ConstOnlySubstitution;

/**
 * A Datalog evaluation engine. Datalog engines are initialized with a set of
 * clauses that represent initial facts and rules that can be used to derive new
 * facts. After initialization, clients can query about whether certain facts
 * are derivable.
 *
 */
public interface DatalogEngine {
	/**
	 * Initializes engine with a Datalog program, including EDB facts. The set
	 * that is passed into this method should include rules for deriving new
	 * facts as well as the initial facts, which can be encoded as clauses with
	 * empty bodies.
	 * 
	 * @param program
	 *            program to evaluate
	 * @throws DatalogValidationException
	 * @throws IllegalStateException
	 *             if this engine has already been initialized
	 * @throws DatalogValidationException
	 *             if the given program is invalid
	 */
	void init(Set<Clause> program) throws DatalogValidationException;

	/**
	 * Returns all facts that 1) can be derived from the rules and initial facts
	 * that were used to initialize this engine and 2) unify with the query.
	 * 
	 * @param q
	 *            the query
	 * @return facts
	 * @throws IllegalStateException
	 *             if this engine has not been initialized with a program
	 */
	Set<PositiveAtom> query(PositiveAtom q);
	
	/**
	 * Returns the set of all (minimal) substitutions that 1) ground the given
	 * conjunctive query, and 2) make it true with respect to the Datalog program
	 * backing this engine. To get a concrete solution to the conjunctive query,
	 * apply one of the returned substitutions to the list representing the query
	 * (using, e.g.,
	 * {@link abcdatalog.util.substitution.SubstitutionUtils#applyToPositiveAtoms(Substitution, Iterable)
	 * SubstitutionUtils.applyToPositiveAtoms}).
	 * 
	 * @param query the conjunctive query
	 * @return the set of minimal satisfying substitutions
	 * @throws IllegalStateException
	 *             if this engine has not been initialized with a program
	 */
	default Set<ConstOnlySubstitution> query(List<PositiveAtom> query) {
		return ConjunctiveQueryHelper.query(this, query);
	}
}
