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
