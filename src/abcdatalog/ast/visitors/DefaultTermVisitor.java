package abcdatalog.ast.visitors;

import abcdatalog.ast.Constant;
import abcdatalog.ast.Variable;

public class DefaultTermVisitor<I, O> implements TermVisitor<I, O> {

	@Override
	public O visit(Variable t, I state) {
		return null;
	}

	@Override
	public O visit(Constant t, I state) {
		return null;
	}

}
