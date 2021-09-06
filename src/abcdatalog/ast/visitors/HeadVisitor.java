package abcdatalog.ast.visitors;

import abcdatalog.ast.PositiveAtom;

public interface HeadVisitor<I, O> {
	public O visit(PositiveAtom atom, I state);
}
