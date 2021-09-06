package edu.harvard.seas.pl.abcdatalog.ast.visitors;

import edu.harvard.seas.pl.abcdatalog.ast.Constant;
import edu.harvard.seas.pl.abcdatalog.ast.Variable;

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
