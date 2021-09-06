package abcdatalog.ast;

import abcdatalog.ast.visitors.TermVisitor;
import abcdatalog.util.substitution.Substitution;

/**
 * A Datalog term (i.e., a constant or variable).
 *
 */
public interface Term {
	public <I, O> O accept(TermVisitor<I, O> visitor, I state);
	
	public Term applySubst(Substitution subst);
}
