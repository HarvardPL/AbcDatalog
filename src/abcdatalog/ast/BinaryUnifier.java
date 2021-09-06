package abcdatalog.ast;

import java.util.Arrays;

import abcdatalog.ast.visitors.PremiseVisitor;
import abcdatalog.util.substitution.Substitution;

/**
 * This premise explicitly unifies two terms and is visually represented as the
 * operator {@code =}. For example, if {@code X=a}, then the variable {@code X}
 * is bound to the constant {@code a}.
 *
 */
public class BinaryUnifier implements Premise {
	private final Term left, right;

	public BinaryUnifier(Term left, Term right) {
		this.left = left;
		this.right = right;
	}

	public Term getLeft() {
		return this.left;
	}

	public Term getRight() {
		return this.right;
	}

	public Iterable<Term> getArgsIterable() {
		return Arrays.asList(this.left, this.right);
	}

	@Override
	public <I, O> O accept(PremiseVisitor<I, O> visitor, I state) {
		return visitor.visit(this, state);
	}

	@Override
	public String toString() {
		return this.left + " = " + this.right;
	}

	@Override
	public Premise applySubst(Substitution subst) {
		return new BinaryUnifier(left.applySubst(subst), right.applySubst(subst));
	}

}
