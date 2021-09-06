package abcdatalog.engine.bottomup.concurrent;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import abcdatalog.engine.bottomup.BottomUpEngineFrame;
import abcdatalog.engine.bottomup.EvalManager;
import abcdatalog.engine.testing.ConjunctiveQueryTests;
import abcdatalog.engine.testing.CoreTests;
import abcdatalog.engine.testing.ExplicitUnificationTests;
import abcdatalog.engine.testing.StratifiedNegationTests;

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
