/*******************************************************************************
 * This file is part of the AbcDatalog project.
 *
 * Copyright (c) 2016, Harvard University
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the BSD License which accompanies this distribution.
 *
 * The development of the AbcDatalog project has been supported by the 
 * National Science Foundation under Grant Nos. 1237235 and 1054172.
 *
 * See README for contributors.
 ******************************************************************************/
package abcdatalog.ast;

import java.util.Arrays;

import abcdatalog.ast.visitors.HeadVisitor;
import abcdatalog.ast.visitors.PremiseVisitor;
import abcdatalog.util.substitution.SimpleConstSubstitution;
import abcdatalog.util.substitution.Substitution;

/**
 * A non-negated atom; i.e., a predicate symbol, and a sequence of terms.
 *
 */
public class PositiveAtom implements Premise, Head {
	/**
	 * Predicate symbol of this atom.
	 */
	protected final PredicateSym pred;
	/**
	 * Arguments of this atom.
	 */
	protected final Term[] args;
	/**
	 * Is the atom ground (i.e., all arguments are constants)?
	 */
	protected volatile Boolean isGround;
	
	/**
	 * A static factory method for the creation of atoms. Returns an atom with
	 * the provided predicate symbol and arguments. The argument array becomes
	 * "owned" by this atom and should not be modified.
	 * 
	 * @param pred
	 *            the predicate symbol
	 * @param args
	 *            the arguments
	 * @return an atom with the provided predicate symbol and arguments
	 */
	public static PositiveAtom create(final PredicateSym pred, final Term[] args) {
		return new PositiveAtom(pred, args);
	}
	
	/**
	 * Constructs an atom from a predicate symbol and a list of arguments.
	 * 
	 * @param pred
	 *            predicate symbol
	 * @param args
	 *            arguments
	 */
	protected PositiveAtom(final PredicateSym pred, final Term[] args) {
		this.pred = pred;
		this.args = args;
		if (pred.getArity() != args.length) {
			throw new IllegalArgumentException("Arity of predicate symbol \""
					+ pred + "\" is " + pred.getArity() + " but given "
					+ args.length + " argument(s).");
		}
	}

	public Term[] getArgs() {
		return this.args;
	}

	public PredicateSym getPred() {
		return this.pred;
	}

	public boolean isGround() {
		Boolean isGround;
		if ((isGround = this.isGround) == null) {
			// This might do redundant work since we do not synchronize, but
			// it's still sound, and it's probably cheap enough that
			// synchronizing might be more expensive.
			boolean b = true;
			for (Term t : args) {
				b &= t instanceof Constant;
			}
			this.isGround = isGround = Boolean.valueOf(b);
		}
		return isGround;
	}
	
	/**
	 * Attempts to unify this atom with a fact (i.e., a ground atom).
	 * 
	 * @param fact
	 *            the fact
	 * @return a substitution, or null if the atoms do not unify
	 */
	public Substitution unify(PositiveAtom fact) {
		assert fact.isGround();
		if (!this.getPred().equals(fact.getPred())) {
			return null;
		}
		return SimpleConstSubstitution.unify(this.args, fact.args);
	}

	/**
	 * Apply a substitution to the terms in this atom.
	 * 
	 * @param subst
	 *            the substitution
	 * @return a new atom with the substitution applied
	 */
	public PositiveAtom applySubst(Substitution subst) {
		return create(this.pred, subst.apply(this.args));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.pred);
		if (this.args.length != 0) {
			sb.append('(');
			for (int i = 0; i < this.args.length; ++i) {
				sb.append(this.args[i]);
				if (i < this.args.length - 1) {
					sb.append(", ");
				}
			}
			sb.append(')');
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(args);
		result = prime * result + pred.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PositiveAtom other = (PositiveAtom) obj;
		// This check relies on isGround being set to one of the static
		// attributes Boolean.TRUE or Boolean.FALSE.
		if (isGround != null && other.isGround != null && isGround != other.isGround)
				return false;
		if (!pred.equals(other.pred))
			return false;
		if (!Arrays.equals(args, other.args))
			return false;
		return true;
	}

	@Override
	public <I, O> O accept(PremiseVisitor<I, O> visitor, I state) {
		return visitor.visit(this, state);
	}

	@Override
	public <I, O> O accept(HeadVisitor<I, O> visitor, I state) {
		return visitor.visit(this, state);
	}
}
