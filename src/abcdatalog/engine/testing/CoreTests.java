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

public abstract class CoreTests extends AbstractTests {
	
	public CoreTests(Supplier<DatalogEngine> engineFactory) {
		super(engineFactory);
	}
	
	@Test
	public void queryUndefinedPredicate() {
		DatalogEngine engine = initEngine("p.");
		Set<PositiveAtom> rs = engine.query(parseQuery("q?"));
		assertTrue(rs.isEmpty());
	}

	@Test
	public void queryEDBPredicate() {
		String program = "p. q(a,b,c,d,e). q(e,d,c,b,a).";
		DatalogEngine engine = initEngine(program);

		Set<PositiveAtom> rs = engine.query(parseQuery("p?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("p.")));

		rs = engine.query(parseQuery("q(V,W,X,Y,Z)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs.containsAll(parseFacts("q(a,b,c,d,e). q(e,d,c,b,a).")));

		rs = engine.query(parseQuery("q(W,b,X,Y,Z)?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("q(a,b,c,d,e).")));

		rs = engine.query(parseQuery("q(W,X,d,Y,Z)?"));
		assertTrue(rs.isEmpty());
	}

	@Test
	public void queryNonRecursiveIDBPredicate() {
		String program = "p(a,b). p(b,c). p(c,d). q(X,Y) :- p(X,Z), p(Z,Y).";
		DatalogEngine engine = initEngine(program);

		Set<PositiveAtom> rs = engine.query(parseQuery("q(X,Y)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs.containsAll(parseFacts("q(a,c). q(b,d).")));

		rs = engine.query(parseQuery("q(X,c)?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("q(a,c).")));

		rs = engine.query(parseQuery("q(x,b)?"));
		assertTrue(rs.isEmpty());
	}

	@Test
	public void queryLinearlyRecursiveIDBPredicate() {
		// Acyclic transitive closure.
		String program = "p(a,b). p(b,c). p(c,d). q(X,Y) :- p(X,Y). "
				+ "q(X,Y) :- p(X,Z), q(Z,Y).";
		DatalogEngine engine = initEngine(program);

		Set<PositiveAtom> rs = engine.query(parseQuery("q(X,Y)?"));
		assertEquals(rs.size(), 6);
		assertTrue(rs.containsAll(parseFacts("q(a,b). q(a,c). q(a,d). q(b,c). "
				+ "q(b,d). q(c,d).")));

		rs = engine.query(parseQuery("q(a,X)?"));
		assertEquals(rs.size(), 3);
		assertTrue(rs.containsAll(parseFacts("q(a,b). q(a,c). q(a,d).")));

		// Transitive closure with a cycle.
		program += "p(d,c).";
		engine = initEngine(program);

		rs = engine.query(parseQuery("q(X,Y)?"));
		assertEquals(rs.size(), 9);	
		assertTrue(rs.containsAll(parseFacts("q(a,b). q(a,c). q(a,d). q(b,c). "
				+ "q(b,d). q(c,c). q(c,d). q(d,c). q(d,d).")));
	}

	@Test
	public void queryNonLinearlyRecursiveIDBPredicate() {
		// Acyclic transitive closure.
		String program = "p(a,b). p(b,c). p(c,d). q(X,Y) :- p(X,Y). "
				+ "q(X,Y) :- q(X,Z), q(Z,Y).";
		DatalogEngine engine = initEngine(program);

		Set<PositiveAtom> rs = engine.query(parseQuery("q(X,Y)?"));
		assertEquals(rs.size(), 6);
		assertTrue(rs.containsAll(parseFacts("q(a,b). q(a,c). q(a,d). q(b,c). "
				+ "q(b,d). q(c,d).")));

		rs = engine.query(parseQuery("q(a,X)?"));
		assertEquals(rs.size(), 3);
		assertTrue(rs.containsAll(parseFacts("q(a,b). q(a,c). q(a,d).")));

		// Transitive closure with a cycle.
		program += "p(d,c).";
		engine = initEngine(program);

		rs = engine.query(parseQuery("q(X,Y)?"));
		assertEquals(rs.size(), 9);
		assertTrue(rs.containsAll(parseFacts("q(a,b). q(a,c). q(a,d). q(b,c). "
				+ "q(b,d). q(c,c). q(c,d). q(d,c). q(d,d).")));
	}

	@Test
	public void queryIDBPredicateWithUndefinedEDB() {
		String program = "q(X,Y) :- p(X,Y). r(a,b).";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("q(X,Y)?"));
		assertTrue(rs.isEmpty());
	}

	@Test
	public void queryIDBPredicateWithExplicitIDBFact() {
		// Note that q is defined by a rule, but we also give an explicit fact.
		String program = "q(X,Y) :- r(X,Y). r(a,b). q(b,c).";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("q(X,Y)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs.containsAll(parseFacts("q(a,b). q(b,c).")));
	}

	@Test
	public void queryZeroAryPredicates() {
		String program = "p :- q. r :- p, s. q. s.";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("q?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("q.")));

		rs = engine.query(parseQuery("p?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("p.")));

		rs = engine.query(parseQuery("s?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("s.")));

		rs = engine.query(parseQuery("r?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("r.")));
	}

	@Test
	public void queryMutuallyRecursivePredicates() {
		String program = "p(X,Y,Z) :- q(X,Y,Z). q(X,Y,Z) :- p(Z,Y,X). "
				+ " p(X,Y,Z) :- r(X,Y,Z). r(a,b,c).";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("p(X,Y,Z)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs.containsAll(parseFacts("p(a,b,c). p(c,b,a).")));

		rs = engine.query(parseQuery("p(X,b,Y)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs.containsAll(parseFacts("p(a,b,c). p(c,b,a).")));

		rs = engine.query(parseQuery("p(a,X,c)?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("p(a,b,c).")));

		rs = engine.query(parseQuery("p(X,c,Y)?"));
		assertTrue(rs.isEmpty());

		// Test a case where there are no facts.
		program = "p(X,Y,Z) :- q(X,Y,Z). q(X,Y,Z) :- p(X,Y,Z).";
		engine = initEngine(program);
		rs = engine.query(parseQuery("p(X,Y,Z)?"));
		assertTrue(rs.isEmpty());
		rs = engine.query(parseQuery("q(X,Y,Z)?"));
		assertTrue(rs.isEmpty());
	}

	@Test
	public void queryNestedPredicates() {
		String program = "1(X,Y,Z) :- 2(X,Y,Z). 2(X,Y,Z) :- 3(X,Y,Z)."
				+ "3(X,Y,Z) :- 4(X,Y,Z). 4(X,Y,Z) :- 5(X,Y,Z). 5(a,b,c).";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("1(X,Y,Z)?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("1(a,b,c).")));
	}

	@Test
	public void queryIDBPredicatesWithConstants() {
		String program = "p(a,X) :- q(X,X). q(a,a). q(b,b). q(c,d).";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("p(Z,W)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs.containsAll(parseFacts("p(a,a). p(a,b).")));

		rs = engine.query(parseQuery("p(a,X)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs.containsAll(parseFacts("p(a,a). p(a,b).")));

		program = "p(X,X) :- q(X,a). q(a,a). q(b,b). q(c,d).";
		engine = initEngine(program);

		rs = engine.query(parseQuery("p(a,X)?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("p(a,a).")));

		rs = engine.query(parseQuery("p(X,a)?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("p(a,a).")));
	}

	/**
	 * This test is primarily for exercising a corner case for engines that use
	 * a RuleBasedSubstitution. In this test case the rules do not have any
	 * variables, so various data structures in the substitution are empty.
	 */
	@Test
	public void testRulesWithNoVariables() {
		String program = "p :- q(a,b). q(a,b).";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("p?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("p.")));

		program = "p :- q(a,b), r(c,d). q(a,b). r(c,d).";
		engine = initEngine(program);

		rs = engine.query(parseQuery("p?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("p.")));
	}

	/**
	 * This test is primarily for exercising a corner case for engines that use
	 * a RuleBasedSubstitution. We want to make sure that even though a
	 * substitution might be completely computed (i.e., every variable in the
	 * rule has an acceptable mapping), the rest of the rule is also correctly
	 * processed.
	 */
	@Test
	public void testRuleThatEndsInAGroundAtom() {
		String program = "edge(0,1). edge(1,2). edge(2,3). tc(X,Y) :- edge(X,Y). "
				+ "tc(X,Y) :- edge(X,Z), tc(Z,Y), trigger.";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("tc(X,Y)?"));
		assertEquals(rs.size(), 3);
		assertTrue(rs.containsAll(parseFacts("tc(0,1). tc(1,2). tc(2,3).")));

		program += "trigger.";
		engine = initEngine(program);
		rs = engine.query(parseQuery("tc(X,Y)?"));
		assertEquals(rs.size(), 6);
		assertTrue(rs.containsAll(parseFacts("tc(0,1). tc(0,2). tc(0,3)."
				+ "tc(1,2). tc(1,3). tc(2,3).")));
	}

	/**
	 * This test is primarily for exercising a corner case for engines that use
	 * a RuleBasedSubstitution. We want to make sure that variables are
	 * consistently bound while an individual atom is being processed.
	 */
	@Test
	public void testRulesWithRepeatedVariablesInAnAtom() {
		String program = "p(X) :- q(X,X). q(a,a). q(a,b). q(b,b).";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("p(X)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs.containsAll(parseFacts("p(a). p(b).")));

		program = "p(X,Y) :- q(Y,X,Y,X). q(a,a,a,a). q(a,a,a,b)."
				+ "q(a,a,b,b). q(a,b,b,b). q(b,b,b,b).";
		engine = initEngine(program);
		rs = engine.query(parseQuery("p(X,Y)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs.containsAll(parseFacts("p(a,a). p(b,b).")));
	}
	
	@Test
	public void testRulesWithLongBodies() {
		String program = "p(A,B,C,D,E,F,G) :- a(A), b(B), c(C), d(D), e(E), f(F), g(G)."
				+ "a(1). b(1). c(1). d(1). d(2). e(1). f(1). g(1).";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("p(A,B,C,D,E,F,G)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs.containsAll(parseFacts("p(1,1,1,1,1,1,1). p(1,1,1,2,1,1,1).")));
		
		program = "p(A,B,C,D,E) :- a(A,X1,X2,X3,X5), b(X6,B,X7,X8,X9),"
				+ " c(X10,X11,C,X12,X13), d(X14,X15,X16,D,X17), e(X18,X19,X20,X21,E)."
				+ "a(foo1,2,3,4,5). b(6,foo2,8,9,10). c(11,12,foo3,14,15). "
				+ "d(16,17,18,foo4,20). e(21,22,23,24,foo5). c(foo1,foo2,bar,foo4,foo5).";
		engine = initEngine(program);
		rs = engine.query(parseQuery("p(A,B,C,D,E)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs
				.containsAll(parseFacts("p(foo1,foo2,foo3,foo4,foo5). p(foo1,foo2,bar,foo4,foo5).")));
	}
	
	@Test
	public void testRulesWithUnusedVariables1() {
		String program = "on(L) :- or(L,L1,X), on(L1). or(a,b,c). on(b).";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("on(a)?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("on(a).")));
		rs = engine.query(parseQuery("on(X)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs.containsAll(parseFacts("on(a). on(b).")));
	}
	
	@Test
	public void testRulesWithUnusedVariables2() {
		String program = "on(L) :- or(L,L1,X), on(L1). on(L) :- or(L,X,L1), on(L1). or(a,b,c). on(b).";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("on(a)?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("on(a).")));
		rs = engine.query(parseQuery("on(X)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs.containsAll(parseFacts("on(a). on(b).")));
	}
	
	@Test
	public void testRulesWithAnonymousVariables() {
		String program = "on(L) :- or(L,L1,_), on(L1). on(L) :- or(L,_,L1), on(L1). or(a,b,c). on(b).";
		DatalogEngine engine = initEngine(program);
		Set<PositiveAtom> rs = engine.query(parseQuery("on(a)?"));
		assertEquals(rs.size(), 1);
		assertTrue(rs.containsAll(parseFacts("on(a).")));
		rs = engine.query(parseQuery("on(_)?"));
		assertEquals(rs.size(), 2);
		assertTrue(rs.containsAll(parseFacts("on(a). on(b).")));
		
		program = "p :- q(_,_), r(_,_). q(a,b). r(c,d).";
		engine = initEngine(program);
		rs = engine.query(parseQuery("p?"));
		assertEquals(rs.size(), 1);
	}
	
	@Test(expected = DatalogValidationException.class)
	public void testUnboundVariable1() throws DatalogValidationException {
		test("p(X,b).", "p(X,Y)?", "");
	}
	
	@Test(expected = DatalogValidationException.class)
	public void testUnboundVariable2() throws DatalogValidationException {
		test("q(X,Y) :- p(X,b). p(a,b).", "q(X,Y)?", "");
	}
	
	@Test
	public void testRulesWithTrue() throws DatalogValidationException {
		test("false.", "true?", "");
		test("p(X) :- q(X). q(a).", "true?", "");
	}
	
	@Test
	public void testEmptyProgram() throws DatalogValidationException {
		test("", "anything?", "");
	}

}
