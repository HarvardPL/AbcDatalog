package edu.harvard.seas.pl.abcdatalog.ast.visitors;

import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;

public interface HeadVisitor<I, O> {
	public O visit(PositiveAtom atom, I state);
}
