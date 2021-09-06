package abcdatalog.util.substitution;

import abcdatalog.ast.Term;
import abcdatalog.ast.Variable;

/**
 * A substitution that allows a variable to be mapped to (i.e., unified with)
 * multiple terms, as long as no two of those terms are constants.
 *
 */
public interface TermUnifier extends Substitution {
	/**
	 * Attempts to unify a variable with a term. Returns a boolean representing
	 * whether the unification was successful. Unification fails if it would
	 * lead to a variable being unified with two distinct constants.
	 * 
	 * @param u
	 *            the variable
	 * @param v
	 *            the term
	 * @return whether the unification was successful
	 */
	public boolean unify(Variable u, Term v);
}
