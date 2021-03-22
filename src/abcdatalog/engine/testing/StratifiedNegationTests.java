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
package abcdatalog.engine.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.function.Supplier;

import org.junit.Test;

import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.engine.DatalogEngine;

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

	@Test
	public void testRelated() throws DatalogValidationException {
		testFile("related.dtlg", "check(X,Y)?", "");
		testFile("related.dtlg", "siblings(X,Y)?",
				"siblings(carson, deidra). siblings(deidra, carson)."
				+ "siblings(harley, gonzalo). siblings(gonzalo, harley)."
				+ "siblings(eldon, fern). siblings(fern, eldon)."
				+ "siblings(noe, odell). siblings(odell, noe)."
				+ "siblings(reanna, sona). siblings(sona, reanna)."
				+ "siblings(terra, ursula). siblings(ursula, terra).");
		testFile("related.dtlg", "ancestor(X, Y)?",
				"ancestor(adria, carson). ancestor(adria, harley). ancestor(adria, gonzalo)."
				+ "ancestor(adria, deidra). ancestor(adria, eldon). ancestor(adria, fern)."
				+ "ancestor(barrett, carson). ancestor(barrett, harley). ancestor(barrett, gonzalo)."
				+ "ancestor(barrett, deidra). ancestor(barrett, eldon). ancestor(barrett, fern)."
				+ "ancestor(carson, harley). ancestor(carson, gonzalo)."
				+ "ancestor(deidra, eldon). ancestor(deidra, fern)."
				+ "ancestor(ignacia, lauretta). ancestor(ignacia, mayra)."
				+ "ancestor(ignacia, noe). ancestor(ignacia, reanna). ancestor(ignacia, sona)."
				+ "ancestor(ignacia, odell). ancestor(ignacia, terra). ancestor(ignacia, ursula)."
				+ "ancestor(ignacia, ursula)."
				+ "ancestor(ignacia, virgilio)."
				+ "ancestor(kati, lauretta). ancestor(kati, mayra)."
				+ "ancestor(kati, noe). ancestor(kati, reanna). ancestor(kati, sona)."
				+ "ancestor(kati, odell). ancestor(kati, terra). ancestor(kati, ursula)."
				+ "ancestor(kati, ursula)."
				+ "ancestor(kati, virgilio)."
				+ "ancestor(lauretta, mayra)."
				+ "ancestor(lauretta, noe). ancestor(lauretta, reanna). ancestor(lauretta, sona)."
				+ "ancestor(lauretta, odell). ancestor(lauretta, terra). ancestor(lauretta, ursula)."
				+ "ancestor(lauretta, ursula)."
				+ "ancestor(lauretta, virgilio)."
				+ "ancestor(mayra, noe). ancestor(mayra, reanna). ancestor(mayra, sona)."
				+ "ancestor(mayra, odell). ancestor(mayra, terra). ancestor(mayra, ursula)."
				+ "ancestor(mayra, ursula)."
				+ "ancestor(mayra, virgilio)."
				+ "ancestor(noe, reanna). ancestor(noe, sona)."
				+ "ancestor(odell, terra). ancestor(odell, ursula)."
				+ "ancestor(odell, ursula)."
				+ "ancestor(odell, virgilio)."
				+ "ancestor(ursula, virgilio).");
		testFile("related.dtlg", "same_generation(gonzalo, Y)?",
				"same_generation(gonzalo, harley). same_generation(gonzalo, eldon). same_generation(gonzalo, fern).");
		testFile("related.dtlg", "same_generation(X, reanna)?",
				"same_generation(sona, reanna). same_generation(terra, reanna). same_generation(ursula, reanna).");
		testFile("related.dtlg", "unrelated(adria, Y)?",
				"unrelated(adria, barrett)."
				+ "unrelated(adria, ignacia). unrelated(adria, kati)."
				+ "unrelated(adria, lauretta). unrelated(adria, mayra)."
				+ "unrelated(adria, noe). unrelated(adria, odell)."
				+ "unrelated(adria, reanna). unrelated(adria, sona)."
				+ "unrelated(adria, terra). unrelated(adria, ursula)."
				+ "unrelated(adria, virgilio).");
		testFile("related.dtlg", "unrelated(X, odell)?",
				"unrelated(adria, odell). unrelated(barrett, odell)."
				+ "unrelated(carson, odell). unrelated(harley, odell). unrelated(gonzalo, odell)."
				+ "unrelated(deidra, odell). unrelated(eldon, odell). unrelated(fern, odell).");
	}
	
	@Test
	public void testRelated2() throws DatalogValidationException {
		testFile("related2.dtlg", "check(X,Y)?", "");
		testFile("related2.dtlg", "siblings(X,Y)?",
				"siblings(carson, deidra). siblings(deidra, carson)."
				+ "siblings(harley, gonzalo). siblings(gonzalo, harley)."
				+ "siblings(eldon, fern). siblings(fern, eldon)."
				+ "siblings(noe, odell). siblings(odell, noe)."
				+ "siblings(reanna, sona). siblings(sona, reanna)."
				+ "siblings(terra, ursula). siblings(ursula, terra).");
		testFile("related2.dtlg", "ancestor(X, Y)?",
				"ancestor(adria, carson). ancestor(adria, harley). ancestor(adria, gonzalo)."
				+ "ancestor(adria, deidra). ancestor(adria, eldon). ancestor(adria, fern)."
				+ "ancestor(barrett, carson). ancestor(barrett, harley). ancestor(barrett, gonzalo)."
				+ "ancestor(barrett, deidra). ancestor(barrett, eldon). ancestor(barrett, fern)."
				+ "ancestor(carson, harley). ancestor(carson, gonzalo)."
				+ "ancestor(deidra, eldon). ancestor(deidra, fern)."
				+ "ancestor(ignacia, lauretta). ancestor(ignacia, mayra)."
				+ "ancestor(ignacia, noe). ancestor(ignacia, reanna). ancestor(ignacia, sona)."
				+ "ancestor(ignacia, odell). ancestor(ignacia, terra). ancestor(ignacia, ursula)."
				+ "ancestor(ignacia, ursula)."
				+ "ancestor(ignacia, virgilio)."
				+ "ancestor(kati, lauretta). ancestor(kati, mayra)."
				+ "ancestor(kati, noe). ancestor(kati, reanna). ancestor(kati, sona)."
				+ "ancestor(kati, odell). ancestor(kati, terra). ancestor(kati, ursula)."
				+ "ancestor(kati, ursula)."
				+ "ancestor(kati, virgilio)."
				+ "ancestor(lauretta, mayra)."
				+ "ancestor(lauretta, noe). ancestor(lauretta, reanna). ancestor(lauretta, sona)."
				+ "ancestor(lauretta, odell). ancestor(lauretta, terra). ancestor(lauretta, ursula)."
				+ "ancestor(lauretta, ursula)."
				+ "ancestor(lauretta, virgilio)."
				+ "ancestor(mayra, noe). ancestor(mayra, reanna). ancestor(mayra, sona)."
				+ "ancestor(mayra, odell). ancestor(mayra, terra). ancestor(mayra, ursula)."
				+ "ancestor(mayra, ursula)."
				+ "ancestor(mayra, virgilio)."
				+ "ancestor(noe, reanna). ancestor(noe, sona)."
				+ "ancestor(odell, terra). ancestor(odell, ursula)."
				+ "ancestor(odell, ursula)."
				+ "ancestor(odell, virgilio)."
				+ "ancestor(ursula, virgilio).");
		testFile("related2.dtlg", "same_generation(gonzalo, Y)?",
				"same_generation(gonzalo, harley). same_generation(gonzalo, eldon). same_generation(gonzalo, fern).");
		testFile("related2.dtlg", "same_generation(X, reanna)?",
				"same_generation(sona, reanna). same_generation(terra, reanna). same_generation(ursula, reanna).");
		testFile("related2.dtlg", "unrelated(adria, Y)?",
				"unrelated(adria, barrett)."
				+ "unrelated(adria, ignacia). unrelated(adria, kati)."
				+ "unrelated(adria, lauretta). unrelated(adria, mayra)."
				+ "unrelated(adria, noe). unrelated(adria, odell)."
				+ "unrelated(adria, reanna). unrelated(adria, sona)."
				+ "unrelated(adria, terra). unrelated(adria, ursula)."
				+ "unrelated(adria, virgilio).");
		testFile("related2.dtlg", "unrelated(X, odell)?",
				"unrelated(adria, odell). unrelated(barrett, odell)."
				+ "unrelated(carson, odell). unrelated(harley, odell). unrelated(gonzalo, odell)."
				+ "unrelated(deidra, odell). unrelated(eldon, odell). unrelated(fern, odell).");
	}
}
