package edu.harvard.seas.pl.abcdatalog.engine.bottomup;

import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.Premise;
import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.util.substitution.Substitution;

/**
 * An annotated atom. In certain evaluation algorithms (such as semi-naive
 * evaluation) it is helpful to have an annotation associated with an atom.
 *
 */
public class AnnotatedAtom implements Premise {
	private final PositiveAtom atom;
	private final Annotation anno;

	public AnnotatedAtom(PositiveAtom atom, Annotation anno) {
		this.atom = atom;
		this.anno = anno;
	}

	public enum Annotation {
		IDB, IDB_PREV, EDB, DELTA;
	}

	public Term[] getArgs() {
		return atom.getArgs();
	}
	
	public PredicateSym getPred() {
		return atom.getPred();
	}
	
	public PositiveAtom asUnannotatedAtom() {
		return atom;
	}

	public Annotation getAnnotation() {
		return anno;
	}
	
	@Override
	public <I, O> O accept(PremiseVisitor<I, O> visitor, I state) {
		return visitor.visit(this, state);
	}

	@Override
	public String toString() {
		String a = "";
		switch (anno) {
		case IDB:
			a = "IDB";
			break;
		case IDB_PREV:
			a = "IDB_PREV";
			break;
		case EDB:
			a = "EDB";
			break;
		case DELTA:
			a = "DELTA";
			break;
		default:
			assert false;
		}
		return atom + "<" + a + ">";
	}

	@Override
	public Premise applySubst(Substitution subst) {
		return new AnnotatedAtom(atom.applySubst(subst), anno);
	}

}
