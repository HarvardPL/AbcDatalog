package abcdatalog.ast.visitors;

import abcdatalog.ast.BinaryDisunifier;
import abcdatalog.ast.BinaryUnifier;
import abcdatalog.ast.NegatedAtom;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.engine.bottomup.AnnotatedAtom;

public class DefaultConjunctVisitor<I, O> implements PremiseVisitor<I, O> {

	@Override
	public O visit(PositiveAtom atom, I state) {
		return null;
	}

	@Override
	public O visit(AnnotatedAtom atom, I state) {
		return null;
	}

	@Override
	public O visit(BinaryUnifier u, I state) {
		return null;
	}

	@Override
	public O visit(BinaryDisunifier u, I state) {
		return null;
	}

	@Override
	public O visit(NegatedAtom atom, I state) {
		return null;
	}

}
