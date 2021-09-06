package edu.harvard.seas.pl.abcdatalog.engine.bottomup.sequential;

import java.io.Reader;
import java.io.StringReader;
import java.util.Set;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.Premise;
import edu.harvard.seas.pl.abcdatalog.engine.DatalogEngine;
import edu.harvard.seas.pl.abcdatalog.engine.DatalogEngineWithProvenance;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.BottomUpEngineFrameWithProvenance;
import edu.harvard.seas.pl.abcdatalog.engine.testing.ConjunctiveQueryTests;
import edu.harvard.seas.pl.abcdatalog.engine.testing.CoreTests;
import edu.harvard.seas.pl.abcdatalog.engine.testing.ExplicitUnificationTests;
import edu.harvard.seas.pl.abcdatalog.engine.testing.StratifiedNegationTests;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogParser;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogTokenizer;

/**
 * A Datalog engine that implements the classic semi-naive bottom-up evaluation
 * algorithm. It supports explicit unification and stratified negation.
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	SemiNaiveEngine.MyCoreTests.class,
	SemiNaiveEngine.MyUnificationTests.class,
	SemiNaiveEngine.MyNegationTests.class,
	SemiNaiveEngine.MyConjunctiveQueryTests.class
})
public class SemiNaiveEngine extends BottomUpEngineFrameWithProvenance {

	public static DatalogEngine newEngine() {
		return new SemiNaiveEngine(false);
	}
	
	public static DatalogEngineWithProvenance newEngineWithProvenance() {
		return new SemiNaiveEngine(true);
	}
	
	SemiNaiveEngine(boolean collectProv) {
		super(new SemiNaiveEvalManager(collectProv));
	}

	public static class MyCoreTests extends CoreTests {

		public MyCoreTests() {
			super(() -> new SemiNaiveEngine(true));
		}

	}

	public static class MyUnificationTests extends ExplicitUnificationTests {

		public MyUnificationTests() {
			super(() -> new SemiNaiveEngine(true));
		}

	}

	public static class MyNegationTests extends StratifiedNegationTests {

		public MyNegationTests() {
			super(() -> new SemiNaiveEngine(true));
		}

	}

	public static class MyConjunctiveQueryTests extends ConjunctiveQueryTests {

		public MyConjunctiveQueryTests() {
			super(() -> new SemiNaiveEngine(true));
		}

	}

	public static void main(String[] args) throws Exception {
		String[] lines = {
				"edge(a, b).",
				"edge(b, c).",
				"edge(c, d).",
				"edge(d, c).",
				"tc(X, Y) :- edge(X, Y).",
				"tc(X, Y) :- tc(X, Z), tc(Z, Y)."
		};
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			sb.append(line);
		}
		Reader r = new StringReader(sb.toString());
		DatalogTokenizer t = new DatalogTokenizer(r);
		Set<Clause> prog = DatalogParser.parseProgram(t);
		DatalogEngineWithProvenance e = SemiNaiveEngine.newEngineWithProvenance();
		e.init(prog);
		t = new DatalogTokenizer(new StringReader("tc(c, c)?"));
		PositiveAtom q = DatalogParser.parseQuery(t);
		Clause lastCl = e.getJustification(q);
		System.out.println(lastCl);
		for (Premise p : lastCl.getBody()) {
			if (p instanceof PositiveAtom) {
				Clause secondLastCl = e.getJustification((PositiveAtom) p);
				System.out.println(secondLastCl);
				for (Premise p2 : secondLastCl.getBody()) {
					System.out.println(e.getJustification((PositiveAtom) p2));
				}
			}
		}
	}

}
