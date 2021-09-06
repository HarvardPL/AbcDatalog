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

import edu.harvard.seas.pl.abcdatalog.ast.visitors.TermVisitor;
import edu.harvard.seas.pl.abcdatalog.util.substitution.Substitution;

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
