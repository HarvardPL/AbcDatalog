package edu.harvard.seas.pl.abcdatalog.ast;

import edu.harvard.seas.pl.abcdatalog.ast.visitors.TermVisitor;
import edu.harvard.seas.pl.abcdatalog.util.substitution.Substitution;

/**
 * A Datalog term (i.e., a constant or variable).
 *
 */
public interface Term {
	public <I, O> O accept(TermVisitor<I, O> visitor, I state);
	
	public Term applySubst(Substitution subst);
}
