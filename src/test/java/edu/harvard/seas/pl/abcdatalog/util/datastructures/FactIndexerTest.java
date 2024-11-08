package edu.harvard.seas.pl.abcdatalog.util.datastructures;

/*-
 * #%L
 * AbcDatalog
 * %%
 * Copyright (C) 2016 - 2024 President and Fellows of Harvard College
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

import edu.harvard.seas.pl.abcdatalog.ast.*;
import edu.harvard.seas.pl.abcdatalog.engine.AbstractTests;
import java.util.function.Supplier;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  FactIndexerTest.SetTests.class,
  FactIndexerTest.ConcurrentLinkedBagTests.class
})
public class FactIndexerTest {
  public static class SetTests extends AbstractFactIndexerTests {
    public SetTests() {
      super(FactIndexerFactory::createConcurrentSetFactIndexer);
    }
  }

  public static class ConcurrentLinkedBagTests extends AbstractFactIndexerTests {
    public ConcurrentLinkedBagTests() {
      super(FactIndexerFactory::createConcurrentQueueFactIndexer);
    }
  }

  public abstract static class AbstractFactIndexerTests extends AbstractTests {
    private final Supplier<FactIndexer> factIndexerFactory;

    public AbstractFactIndexerTests(Supplier<FactIndexer> factIndexerFactory) {
      super(
          () -> {
            throw new Error("Tests do not use engine=");
          });
      this.factIndexerFactory = factIndexerFactory;
    }

    @Test
    public void testSmallestFactSetIsReturnedFromFineIndex() {
      FactIndexer indexer = factIndexerFactory.get();
      indexer.addAll(parseFacts("f(a,x,b). f(b,x,b). f(c,x1,b). f(c,x2,b). f(c,x,d)."));

      Iterable<PositiveAtom> result = indexer.indexInto(parseQuery("f(c,_,d)?"));
      int size = 0;
      for (PositiveAtom ignored : result) {
        ++size;
      }
      Assert.assertEquals(1, size);
    }
  }
}
