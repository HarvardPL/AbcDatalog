package edu.harvard.seas.pl.abcdatalog.engine.bottomup.concurrent;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.harvard.seas.pl.abcdatalog.engine.bottomup.BottomUpEngineFrame;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.EvalManager;
import edu.harvard.seas.pl.abcdatalog.engine.testing.ConjunctiveQueryTests;
import edu.harvard.seas.pl.abcdatalog.engine.testing.CoreTests;
import edu.harvard.seas.pl.abcdatalog.engine.testing.ExplicitUnificationTests;
import edu.harvard.seas.pl.abcdatalog.engine.testing.StratifiedNegationTests;

/**
 * This class implements an experimental multi-threaded Datalog evaluation
 * algorithm that supports explicit unification and stratified negation.
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	ConcurrentStratifiedNegationBottomUpEngine.MyCoreTests.class,
	ConcurrentStratifiedNegationBottomUpEngine.MyUnificationTests.class,
	ConcurrentStratifiedNegationBottomUpEngine.MyNegationTests.class
})
public class ConcurrentStratifiedNegationBottomUpEngine extends BottomUpEngineFrame<EvalManager> {

	public ConcurrentStratifiedNegationBottomUpEngine() {
		super(new StratifiedNegationEvalManager());
	}

	public static class MyCoreTests extends CoreTests {

		public MyCoreTests() {
			super(() -> new ConcurrentStratifiedNegationBottomUpEngine());
		}

	}

	public static class MyUnificationTests extends ExplicitUnificationTests {

		public MyUnificationTests() {
			super(() -> new ConcurrentStratifiedNegationBottomUpEngine());
		}

	}

	public static class MyNegationTests extends StratifiedNegationTests {

		public MyNegationTests() {
			super(() -> new ConcurrentStratifiedNegationBottomUpEngine());
		}

	}
	
	public static class MyConjunctiveQueryTests extends ConjunctiveQueryTests {

		public MyConjunctiveQueryTests() {
			super(() -> new ConcurrentStratifiedNegationBottomUpEngine());
		}

	}

}
