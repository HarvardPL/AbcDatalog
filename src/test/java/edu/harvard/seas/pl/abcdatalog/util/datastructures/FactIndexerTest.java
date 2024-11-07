package edu.harvard.seas.pl.abcdatalog.util.datastructures;

import edu.harvard.seas.pl.abcdatalog.ast.*;
import edu.harvard.seas.pl.abcdatalog.engine.AbstractTests;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.util.function.Supplier;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  FactIndexerTest.SetTests.class,
  FactIndexerTest.ConcurrentLinkedBagTests.class
})
public class FactIndexerTest {
    public static class SetTests extends AbstractFactIndexerTests {
        public SetTests() { super(FactIndexerFactory::createConcurrentSetFactIndexer); }
    }

    public static class ConcurrentLinkedBagTests extends AbstractFactIndexerTests {
        public ConcurrentLinkedBagTests() { super(FactIndexerFactory::createConcurrentQueueFactIndexer); }
    }

    public static abstract class AbstractFactIndexerTests extends AbstractTests {
        private final Supplier<FactIndexer> factIndexerFactory;

        public AbstractFactIndexerTests(Supplier<FactIndexer> factIndexerFactory) {
            super(() -> { throw new Error("Tests do not use engine="); });
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