package abcdatalog.ast.visitors;

import abcdatalog.ast.PositiveAtom;

public class CrashHeadVisitor<I, O> implements HeadVisitor<I, O> {

	@Override
	public O visit(PositiveAtom atom, I state) {
		throw new UnsupportedOperationException();
	}

}
