package edu.harvard.seas.pl.abcdatalog.ast;

import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.util.substitution.Substitution;

/**
 * A premise in the body of a clause. This interface is under-specified to allow
 * the addition of new language features.
 *
 */
public interface Premise {
	<I, O> O accept(PremiseVisitor<I, O> visitor, I state);
	
	Premise applySubst(Substitution subst);
}
