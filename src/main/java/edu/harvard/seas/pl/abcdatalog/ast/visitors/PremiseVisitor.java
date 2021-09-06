package edu.harvard.seas.pl.abcdatalog.ast.visitors;

import edu.harvard.seas.pl.abcdatalog.ast.BinaryDisunifier;
import edu.harvard.seas.pl.abcdatalog.ast.BinaryUnifier;
import edu.harvard.seas.pl.abcdatalog.ast.NegatedAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.AnnotatedAtom;

public interface PremiseVisitor<I, O> {
	public O visit(PositiveAtom atom, I state);
	
	public O visit(AnnotatedAtom atom, I state);
	
	public O visit(BinaryUnifier u, I state);
	
	public O visit(BinaryDisunifier u, I state);
	
	public O visit(NegatedAtom atom, I state);
}
