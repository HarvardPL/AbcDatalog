package edu.harvard.seas.pl.abcdatalog.engine.bottomup.concurrent;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.harvard.seas.pl.abcdatalog.engine.bottomup.BottomUpEngineFrame;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.EvalManager;
import edu.harvard.seas.pl.abcdatalog.engine.testing.ConjunctiveQueryTests;
import edu.harvard.seas.pl.abcdatalog.engine.testing.CoreTests;
import edu.harvard.seas.pl.abcdatalog.engine.testing.ExplicitUnificationTests;

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
public class ConcurrentBottomUpEngine extends BottomUpEngineFrame<EvalManager> {

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
