package edu.harvard.seas.pl.abcdatalog.ast.visitors;

import edu.harvard.seas.pl.abcdatalog.ast.Constant;
import edu.harvard.seas.pl.abcdatalog.ast.Variable;

public interface TermVisitor<I, O> {
	public O visit(Variable t, I state);
	
	public O visit(Constant t, I state);
}
