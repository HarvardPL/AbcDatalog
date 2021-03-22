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
package abcdatalog.engine.bottomup.concurrent;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import abcdatalog.engine.bottomup.BottomUpEngineFrame;
import abcdatalog.engine.testing.ConjunctiveQueryTests;
import abcdatalog.engine.testing.CoreTests;
import abcdatalog.engine.testing.ExplicitUnificationTests;

/**
 * A concurrent bottom-up Datalog engine that employs a saturation algorithm
 * similar to semi-naive evaluation. It supports explicit unification.
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	ConcurrentBottomUpEngine.MyCoreTests.class,
	ConcurrentBottomUpEngine.MyUnificationTests.class,
	ConcurrentBottomUpEngine.MyConjunctiveQueryTests.class
})
public class ConcurrentBottomUpEngine extends BottomUpEngineFrame {

	public ConcurrentBottomUpEngine() {
		super(new BottomUpEvalManager());
	}
	
	public static class MyCoreTests extends CoreTests {
		
		public MyCoreTests() {
			super(() -> new ConcurrentBottomUpEngine());
		}
		
	}
	
	public static class MyUnificationTests extends ExplicitUnificationTests {
		
		public MyUnificationTests() {
			super(() -> new ConcurrentBottomUpEngine());
		}
		
	}
	
	public static class MyConjunctiveQueryTests extends ConjunctiveQueryTests {

		public MyConjunctiveQueryTests() {
			super(() -> new ConcurrentBottomUpEngine());
		}

	}
}
