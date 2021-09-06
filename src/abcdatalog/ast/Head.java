package abcdatalog.ast;

import abcdatalog.ast.visitors.HeadVisitor;
import abcdatalog.util.substitution.Substitution;

/**
 * The head of a clause. This interface is under-specified to allow the addition
 * of new language features.
 *
 */
public interface Head {
	<I, O> O accept(HeadVisitor<I, O> visitor, I state);
	
	Head applySubst(Substitution subst);
}
