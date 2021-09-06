package edu.harvard.seas.pl.abcdatalog.engine.bottomup.concurrent;

/*-
 * #%L
 * AbcDatalog
 * %%
 * Copyright (C) 2016 - 2021 President and Fellows of Harvard College
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the President and Fellows of Harvard College nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

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
