package edu.harvard.seas.pl.abcdatalog.executor;

/*-
 * #%L
 * AbcDatalog
 * %%
 * Copyright (C) 2016 - 2023 President and Fellows of Harvard College
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

import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidationException;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogParseException;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogParser;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogTokenizer;
import org.junit.Test;

import java.io.StringReader;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DatalogParallelExecutorTest {

    private static void waitAndAdd(DatalogExecutor ex, PositiveAtom fact) throws InterruptedException {
        Thread.sleep(10);
        ex.addFactAsynchronously(fact);
    }

    @Test
    public void testExample() throws DatalogParseException, DatalogValidationException, InterruptedException {
        String program = "edge(0,1). edge(1,2). tc(X,Y) :- edge(X,Y)."
                + "tc(X,Y) :- edge(X,Z), tc(Z,Y). cycle :- tc(X,X).";
        DatalogTokenizer t = new DatalogTokenizer(new StringReader(program));
        Set<Clause> ast = DatalogParser.parseProgram(t);
        PredicateSym edge = PredicateSym.create("edge", 2);
        PredicateSym tc = PredicateSym.create("tc", 2);
        PredicateSym cycle = PredicateSym.create("cycle", 0);

        // 1. Initialize the executor.

        DatalogParallelExecutor ex = new DatalogParallelExecutor();
        ex.initialize(ast, Collections.singleton(edge));

        // 2. Register listeners.

        // Every time a new tuple is added to the transitive closure relation,
        // print it out.
        AtomicInteger cnt = new AtomicInteger();
        ex.registerListener(tc, fact -> {
            cnt.incrementAndGet();
        });

        // Notify us if a cycle is detected.
        AtomicBoolean cycleFound = new AtomicBoolean();
        ex.registerListener(cycle, fact -> {
            cycleFound.set(true);
        });

        // 3. Start the executor.

        ex.start();

        Thread.sleep(50);
        assert(cnt.get() == 3);

        // 4. Add new facts to the executor.

        String newFacts = "edge(2,3). edge(3,4). edge(4,0).";
        t = new DatalogTokenizer(new StringReader(newFacts));
        while (t.hasNext()) {
            waitAndAdd(ex, DatalogParser.parseClauseAsPositiveAtom(t));
        }

        ex.shutdown();

        assert(cnt.get() == 25);
        assert(cycleFound.get());
    }
}
