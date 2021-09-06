package edu.harvard.seas.pl.abcdatalog.ast;

/*-
 * #%L
 * AbcDatalog
 * %%
 * Copyright (C) 2016 - 2021 President and Fellows of Harvard College
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the President and Fellows of Harvard College nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

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
