package edu.harvard.seas.pl.abcdatalog.engine.testing;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.harvard.seas.pl.abcdatalog.engine.bottomup.concurrent.ConcurrentBottomUpEngine;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.concurrent.ConcurrentChunkedBottomUpEngine;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.concurrent.ConcurrentStratifiedNegationBottomUpEngine;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.sequential.SemiNaiveEngine;
import edu.harvard.seas.pl.abcdatalog.engine.topdown.IterativeQsqEngine;
import edu.harvard.seas.pl.abcdatalog.engine.topdown.MstEngine;
import edu.harvard.seas.pl.abcdatalog.engine.topdown.RecursiveQsqEngine;

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
