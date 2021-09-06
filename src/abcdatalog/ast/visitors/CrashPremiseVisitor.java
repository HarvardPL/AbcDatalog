package abcdatalog.ast.visitors;

import abcdatalog.ast.BinaryDisunifier;
import abcdatalog.ast.BinaryUnifier;
import abcdatalog.ast.NegatedAtom;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.engine.bottomup.AnnotatedAtom;

public class CrashPremiseVisitor<I, O> implements PremiseVisitor<I, O> {

	@Override
	public O visit(PositiveAtom atom, I state) {
		throw new UnsupportedOperationException();
	}

	@Override
	public O visit(AnnotatedAtom atom, I state) {
		throw new UnsupportedOperationException();
	}

	@Override
	public O visit(BinaryUnifier u, I state) {
		throw new UnsupportedOperationException();
	}

	@Override
	public O visit(BinaryDisunifier u, I state) {
		throw new UnsupportedOperationException();
	}

	@Override
	public O visit(NegatedAtom atom, I state) {
		throw new UnsupportedOperationException();
	}

}
