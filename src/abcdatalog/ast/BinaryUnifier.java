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

import abcdatalog.ast.visitors.PremiseVisitor;

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

}
