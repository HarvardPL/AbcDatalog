package edu.harvard.seas.pl.abcdatalog.ast.visitors;

import edu.harvard.seas.pl.abcdatalog.ast.BinaryDisunifier;
import edu.harvard.seas.pl.abcdatalog.ast.BinaryUnifier;
import edu.harvard.seas.pl.abcdatalog.ast.NegatedAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.AnnotatedAtom;

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
