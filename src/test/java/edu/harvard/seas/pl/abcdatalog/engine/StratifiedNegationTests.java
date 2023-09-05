package edu.harvard.seas.pl.abcdatalog.engine;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.function.Supplier;

import org.junit.Ignore;
import org.junit.Test;

import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidationException;

public abstract class StratifiedNegationTests extends AbstractTests {

	public StratifiedNegationTests(Supplier<DatalogEngine> engineFactory) {
		super(engineFactory);
	}

	@Test(expected = DatalogValidationException.class)
	public void TestUnstratifiable1() throws DatalogValidationException {
		@SuppressWarnings("unused")
		DatalogEngine e = initEngineUnsafe("p :- not q. q :- not p.");
	}

	@Test(expected = DatalogValidationException.class)
	public void TestUnstratifiable2() throws DatalogValidationException {
		@SuppressWarnings("unused")
		DatalogEngine e = initEngineUnsafe("p(X) :- not q(X), edb(X). q(X) :- not p(X), edb(X). edb(a).");
	}

	public void TestStratifiable() {
		String program = "edge(a,b). edge(b,c). tc(a,c). tc(X,Y) :- edge(X,Y). tc(X,Y) :- tc(X,Z), tc(Z,Y)."
				+ "node(X) :- edge(X,_). node(X) :- edge(_,X). not_tc(X,Y) :- node(X), node(Y), not tc(X,Y).";
		DatalogEngine e = initEngine(program);

		Set<PositiveAtom> rs = e.query(parseQuery("tc(X,Y)?"));
		assertEquals(rs.size(), 3);
		assertTrue(rs.containsAll(parseFacts("tc(a,b). tc(b,c). tc(a,c).")));

		rs = e.query(parseQuery("not_tc(X,Y)?"));
		assertEquals(rs.size(), 3);
		assertTrue(rs.containsAll(
				parseFacts("not_tc(a,a). not_tc(b,b). not_tc(b,a). not_tc(c,c). not_tc(c, a). not_tc(c, b).")));
	}

	@Test(expected = DatalogValidationException.class)
	public void testNegatedUnboundVariable1() throws DatalogValidationException {
		test("p :- not q(X,a).", "p?", "");
	}

	@Test(expected = DatalogValidationException.class)
	public void testNegatedUnboundVariable2() throws DatalogValidationException {
		test("p(X) :- not q(X,a).", "p?", "");
	}

	@Test(expected = DatalogValidationException.class)
	public void testNegatedUnboundVariable3() throws DatalogValidationException {
		test("p :- not q(X,a), not r(X).", "p?", "");
	}

	@Test
	public void testMultiLayeredNegation() throws DatalogValidationException {
		test("p :- not q. q :- not r. r :- not s.", "p?", "p.");
	}

	// These next three tests assume that the engine being tested also supports
	// explicit unification.
	@Test
	public void testExplicitUnification1() throws DatalogValidationException {
		test("p :- not q(X,b), X=a.", "p?", "p.");
	}

	@Test(expected = DatalogValidationException.class)
	public void testExplicitUnification2() throws DatalogValidationException {
		test("p :- not q(X,b), X!=a.", "p?", "");
	}

	@Test
	public void testNegatedNoPositiveAtom() throws DatalogValidationException {
		test("p(a) :- not q(a). p(b) :- not q(X), X=b.", "p(X)?", "p(a). p(b).");
	}

	@Test
	public void testNegatedAtomFirst() throws DatalogValidationException {
		test("n(a). n(b). n(c). e(a, b). e(b, c). r(X,Y) :- e(X,Y). r(X,Y) :- e(X,Z), r(Z,Y)."
				+ "unreach(X,Y) :- not r(X, Y), n(X), n(Y).", "unreach(X,Y)?",
				"unreach(b,a). unreach(c,b). unreach(c,a). unreach(a,a). unreach(b,b). unreach(c,c).");
	}

}
