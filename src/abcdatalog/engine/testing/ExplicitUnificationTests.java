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

public class ExplicitUnificationTests extends AbstractTests {

	public ExplicitUnificationTests(Supplier<DatalogEngine> engineFactory) {
		super(engineFactory);
	}
	
	@Test
	public void testRulesWithBinaryUnifiers() {
		String program = "tc(X,Y) :- edge(X,Y). tc(X,Y) :- edge(X,Z), tc(Z,Y)."
				+ "edge(a,b). edge(b,c). edge(c,c). edge(c,d)."
				+ "cycle(X) :- X = Y, tc(X,Y)."
				+ "beginsAtC(X,Y) :- tc(X,Y), c = X.";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("cycle(X)?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("cycle(c).")));
		
		rs = engine.query(parseQuery("beginsAtC(X,Y)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs.containsAll(parseFacts("beginsAtC(c,c). beginsAtC(c,d).")));
	}
	
	@Test
	public void testRulesWithBinaryDisunifiers() {
		String program = "tc(X,Y) :- edge(X,Y). tc(X,Y) :- edge(X,Z), tc(Z,Y)."
				+ "edge(a,b). edge(b,c). edge(c,c). edge(c,d)."
				+ "noncycle(X,Y) :- X != Y, tc(X,Y)."
				+ "beginsNotAtC(X,Y) :- tc(X,Y), c != X."
				+ "noC(X,Y) :- edge(X,Y), X != c, Y != c."
				+ "noC(X,Y) :- noC(X,Z), noC(Z,Y).";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("noncycle(X,Y)?"));
		assertEquals(rs.size(), 6);
		assertTrue(rs.containsAll(parseFacts("noncycle(a,b). noncycle(a,c)."
				+ "noncycle(a,d). noncycle(b,c). noncycle(b,d). noncycle(c,d).")));
		
		rs = engine.query(parseQuery("beginsNotAtC(X,Y)?"));
		assertEquals(rs.size(), 5);
		assertTrue(rs.containsAll(parseFacts("beginsNotAtC(a,b). beginsNotAtC(a,c)."
				+ "beginsNotAtC(a,d). beginsNotAtC(b,c). beginsNotAtC(b,d).")));
		
		rs = engine.query(parseQuery("noC(X,Y)?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("noC(a,b).")));
	}
	
	@Test
	public void testBinaryUnificationNoAtom() throws DatalogValidationException {
		String program = "p(X,b) :- X=a. p(b,Y) :- Y=a. p(X,Y) :- X=c, Y=d. p(X,X) :- X=c. p(X,Y) :- X=d, Y=X. p(X,Y) :- X=Y, X=e.";
		String expected = "p(a,b). p(b,a). p(c,d). p(c,c). p(d,d). p(e,e).";
		test(program, "p(X,Y)?", expected);
		
		program += "q(X,Y) :- p(X,Y).";
		expected = "q(a,b). q(b,a). q(c,d). q(c,c). q(d,d). q(e,e).";
		test(program, "q(X,Y)?", expected);
	}
	
	@Test(expected = DatalogValidationException.class)
	public void testUselessBinaryUnification() throws DatalogValidationException {
		test("p(X) :- q(X), X=Y. q(a). p(b) :- X=_.", "p(X)?", "p(a). p(b).");
	}
	
	public void testImpossibleBinaryUnification1() throws DatalogValidationException {
		test("p :- a=b.", "p?", "");
	}
	
	public void testImpossibleBinaryUnification2() throws DatalogValidationException {
		test("p :- Z=b, X=Y, a=X, Z=Y.", "p?", "");
	}
	
	@Test
	public void testBinaryDisunificationNoAtom() throws DatalogValidationException {
		String program = "p :- a!=b. q :- X!=Y, X=a, Y=b.";
		test(program, "p?", "p.");
		test(program, "q?", "q.");
	}
	
	public void testImpossibleBinaryDisunification1() throws DatalogValidationException {
		test("p :- a!=a.", "p?", "");
	}
	
	public void testImpossibleBinaryDisunification2() throws DatalogValidationException {
		test("p :- Z=a, X!=Y, a=X, Z=Y.", "p?", "");
	}
	
	public void testImpossibleBinaryDisunification3() throws DatalogValidationException {
		test("p :- q(X), X!=X.", "p?", "");
	}
	
	@Test(expected = DatalogValidationException.class)
	public void testBinaryDisunificationFail1() throws DatalogValidationException {
		test("p :- not q(a,b), X!=_.", "p?", "");
	}
	
	@Test(expected = DatalogValidationException.class)
	public void testBinaryDisunificationFail2() throws DatalogValidationException {
		test("p(X) :- q(X), Y!=_. q(a).", "p(X)?", "");
		throw new DatalogValidationException();
	}

}
