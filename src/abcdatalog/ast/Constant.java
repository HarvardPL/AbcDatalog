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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import abcdatalog.ast.visitors.TermVisitor;
import abcdatalog.util.substitution.Substitution;

/**
 * A zero-ary function symbol (i.e., a constant in Datalog).
 *
 */
public class Constant implements Term {
	/**
	 * Identifier of the constant.
	 */
	private final String name;

	/**
	 * A map for memoization.
	 */
	private static final ConcurrentMap<String, Constant> memo = new ConcurrentHashMap<>();

	/**
	 * Returns a constant with the given string identifier.
	 * 
	 * @param name
	 *            the string identifier
	 * @return the constant
	 */
	public static Constant create(String name) {
		Constant c = memo.get(name);
		if (c != null) {
			return c;
		}
		// try creating it
		c = new Constant(name);
		Constant existing = memo.putIfAbsent(name, c);
		if (existing != null) {
			return existing;
		}
		return c;
	}
	
	/**
	 * Constructs a constant with the given name.
	 * 
	 * @param name
	 *            name
	 */
	private Constant(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return this.getName();
	}

	@Override
	public <I, O> O accept(TermVisitor<I, O> visitor, I state) {
		return visitor.visit(this, state);
	}

	@Override
	public Term applySubst(Substitution subst) {
		return this;
	}

}
