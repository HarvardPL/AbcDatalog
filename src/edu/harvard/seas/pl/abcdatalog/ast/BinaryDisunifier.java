package edu.harvard.seas.pl.abcdatalog.ast;

import java.util.Arrays;

import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.util.substitution.Substitution;

/**
 * This premise explicitly disallows the unification of two terms and is represented by the operator {@code !=}. For example, if
 * {@code X!=a}, then the variable {@code X} cannot be unified with the constant {@code a}.
 *
 */
public class BinaryDisunifier implements Premise {
	private final Term left, right;

	public BinaryDisunifier(Term left, Term right) {
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
		return this.left + " != " + this.right;
	}
	
	@Override
	public Premise applySubst(Substitution subst) {
		return new BinaryDisunifier(left.applySubst(subst), right.applySubst(subst));
	}

}
