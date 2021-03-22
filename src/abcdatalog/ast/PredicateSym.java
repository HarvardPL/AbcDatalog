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

/**
 * A basic predicate symbol in Datalog.
 *
 */
public class PredicateSym {
	/**
	 * Identifier of the predicate symbol (i.e. the symbol itself).
	 */
	protected final String sym;
	/**
	 * Arity of any atom formed from this predicate symbol.
	 */
	protected final int arity;
	
	/**
	 * Map for memoization.
	 */
	private static final ConcurrentMap<String, ConcurrentMap<Integer, PredicateSym>> memo = new ConcurrentHashMap<>();

	/**
	 * Returns a predicate symbol with the given string identifier and arity.
	 * 
	 * @param sym
	 *            the string identifier
	 * @param arity
	 *            the arity
	 * @return the predicate symbol
	 */
	public static PredicateSym create(String sym, int arity) {
		ConcurrentMap<Integer, PredicateSym> byArity = memo.get(sym);
		if (byArity == null) {
			byArity = new ConcurrentHashMap<>();
			ConcurrentMap<Integer, PredicateSym> existing = memo.putIfAbsent(sym, byArity);
			if (existing != null) {
				byArity = existing;
			}
		}
		
		PredicateSym ps = byArity.get(arity);
		if (ps == null) {
			ps = new PredicateSym(sym, arity);
			PredicateSym existing = byArity.putIfAbsent(arity, ps);
			if (existing != null) {
				return existing;
			}
		}
		return ps;
	}
	
	/**
	 * Returns the string identifier of this predicate symbol.
	 * 
	 * @return the string identifier
	 */
	public String getSym() {
		return sym;
	}
	
	/**
	 * Returns the arity of this predicate symbol.
	 * 
	 * @return the arity
	 */
	public int getArity() {
		return arity;
	}
	
	/**
	 * Constructs a predicate symbol from an identifier and a non-negative
	 * arity.
	 * 
	 * @param sym
	 *            identifier
	 * @param arity
	 *            non-negative arity
	 */
	protected PredicateSym(String sym, int arity) {
		if (arity < 0) {
			throw new IllegalArgumentException("Arity cannot be negative, "
					+ "but predicate symbol \"" + sym
					+ "\" initialized with an arity of " + arity + ".");
		}
		this.sym = sym;
		this.arity = arity;
	}

	@Override
	public String toString() {
		return this.sym;
	}

}
