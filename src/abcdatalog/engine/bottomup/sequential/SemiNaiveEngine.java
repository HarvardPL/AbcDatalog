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
package abcdatalog.engine.bottomup.sequential;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import abcdatalog.engine.bottomup.BottomUpEngineFrame;
import abcdatalog.engine.testing.ConjunctiveQueryTests;
import abcdatalog.engine.testing.CoreTests;
import abcdatalog.engine.testing.ExplicitUnificationTests;
import abcdatalog.engine.testing.StratifiedNegationTests;

/**
 * A Datalog engine that implements the classic semi-naive bottom-up evaluation
 * algorithm. It supports explicit unification and stratified negation.
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	SemiNaiveEngine.MyCoreTests.class,
	SemiNaiveEngine.MyUnificationTests.class,
	SemiNaiveEngine.MyNegationTests.class,
	SemiNaiveEngine.MyConjunctiveQueryTests.class
})
public class SemiNaiveEngine extends BottomUpEngineFrame {

	public SemiNaiveEngine() {
		super(new SemiNaiveEvalManager());
	}

	public static class MyCoreTests extends CoreTests {

		public MyCoreTests() {
			super(() -> new SemiNaiveEngine());
		}

	}

	public static class MyUnificationTests extends ExplicitUnificationTests {

		public MyUnificationTests() {
			super(() -> new SemiNaiveEngine());
		}

	}

	public static class MyNegationTests extends StratifiedNegationTests {

		public MyNegationTests() {
			super(() -> new SemiNaiveEngine());
		}

	}

	public static class MyConjunctiveQueryTests extends ConjunctiveQueryTests {

		public MyConjunctiveQueryTests() {
			super(() -> new SemiNaiveEngine());
		}

	}

}
