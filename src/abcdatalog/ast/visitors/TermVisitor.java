package abcdatalog.ast.visitors;

import abcdatalog.ast.Constant;
import abcdatalog.ast.Variable;

public interface TermVisitor<I, O> {
	public O visit(Variable t, I state);
	
	public O visit(Constant t, I state);
}
