package abcdatalog.engine.testing;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import abcdatalog.engine.bottomup.concurrent.ConcurrentBottomUpEngine;
import abcdatalog.engine.bottomup.concurrent.ConcurrentChunkedBottomUpEngine;
import abcdatalog.engine.bottomup.concurrent.ConcurrentStratifiedNegationBottomUpEngine;
import abcdatalog.engine.bottomup.sequential.SemiNaiveEngine;
import abcdatalog.engine.topdown.IterativeQsqEngine;
import abcdatalog.engine.topdown.MstEngine;
import abcdatalog.engine.topdown.RecursiveQsqEngine;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	ConcurrentBottomUpEngine.class,
	ConcurrentChunkedBottomUpEngine.class,
	ConcurrentStratifiedNegationBottomUpEngine.class,
	SemiNaiveEngine.class,
	RecursiveQsqEngine.class,
	IterativeQsqEngine.class,
	MstEngine.class
})
public class RunAllTests {

	public RunAllTests() {
		
	}

}
