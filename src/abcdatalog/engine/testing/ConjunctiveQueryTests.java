package abcdatalog.engine.testing;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.Test;

import abcdatalog.ast.PositiveAtom;
import abcdatalog.engine.DatalogEngine;
import abcdatalog.parser.DatalogParseException;
import abcdatalog.parser.DatalogParser;
import abcdatalog.parser.DatalogTokenizer;
import abcdatalog.util.substitution.ConstOnlySubstitution;
import abcdatalog.util.substitution.SubstitutionUtils;

public abstract class ConjunctiveQueryTests extends AbstractTests {

	public ConjunctiveQueryTests(Supplier<DatalogEngine> engineFactory) {
		super(engineFactory);
	}

	List<PositiveAtom> parseConjunctiveQuery(String query) {
		DatalogTokenizer t = new DatalogTokenizer(new StringReader(query));
		List<PositiveAtom> r = new ArrayList<>();
		try {
			while (t.hasNext()) {
				r.add(DatalogParser.parseClauseAsPositiveAtom(t));
			}
			return r;
		} catch (DatalogParseException e) {
			fail("Parsing error: " + e.getMessage());
			// Never reached.
			return null;
		}
	}

	private boolean testConjunctiveQuery(String program, String query, String... expected) {
		DatalogEngine engine = initEngine(program);
		List<PositiveAtom> q = parseConjunctiveQuery(query);
		Set<ConstOnlySubstitution> s = engine.query(q);
		if (s.size() != expected.length) {
			return false;
		}
		Set<List<PositiveAtom>> r = new HashSet<>();
		for (ConstOnlySubstitution subst : s) {
			r.add(SubstitutionUtils.applyToPositiveAtoms(subst, q));
		}
		Set<List<PositiveAtom>> e = new HashSet<>();
		for (String x : expected) {
			e.add(parseConjunctiveQuery(x));
		}
		return r.containsAll(e);
	}

	@Test
	public void emptyQuery() {
		String program = "p(a,b). p(b,c). p(c,a). q(X,Y) :- p(X,Z), p(Z,Y).";
		String query = "";
		String[] expected = {
				""
				};
		assertTrue(testConjunctiveQuery(program, query, expected));
	}

	@Test
	public void singletonQuery() {
		String program = "p(a,b). p(b,c). p(c,a). q(X,Y) :- p(X,Z), p(Z,Y).";
		String query = "q(X,Y).";
		String[] expected = {
				"q(a,c).",
				"q(b,a).",
				"q(c,b)."
				};
		assertTrue(testConjunctiveQuery(program, query, expected));
	}

	@Test
	public void sizeTwoQuery1() {
		String program = "p(a,b). p(b,c). p(c,a). q(X,Y) :- p(X,Z), p(Z,Y).";
		String query = "q(X,Y). q(Y,Z).";
		String[] expected = {
				"q(a,c). q(c,b).",
				"q(b,a). q(a,c).",
				"q(c,b). q(b,a)."
				};
		assertTrue(testConjunctiveQuery(program, query, expected));
	}
	
	@Test
	public void sizeTwoQuery2() {
		String program = "p(a,b). p(b,c). p(c,a). q(X,Y) :- p(X,Z), p(Z,Y).";
		String query = "q(X,Y). q(Y,X).";
		String[] expected = {
				};
		assertTrue(testConjunctiveQuery(program, query, expected));
	}
	
	@Test
	public void sizeThreeQuery1() {
		String program = "p(a,b). p(b,c). p(c,a). q(X,Y) :- p(X,Z), p(Z,Y).";
		String query = "q(X,Y). q(Y,Z). q(Z,W).";
		String[] expected = {
				"q(a,c). q(c,b). q(b,a).",
				"q(b,a). q(a,c). q(c,b).",
				"q(c,b). q(b,a). q(a,c)."
				};
		assertTrue(testConjunctiveQuery(program, query, expected));
	}
	
	@Test
	public void sizeThreeQuery2() {
		String program = "p(a,b). p(b,c). p(c,a). q(X,Y) :- p(X,Z), p(Z,Y).";
		String query = "q(X,Y). q(Y,Z). q(Z,X).";
		String[] expected = {
				"q(a,c). q(c,b). q(b,a).",
				"q(b,a). q(a,c). q(c,b).",
				"q(c,b). q(b,a). q(a,c)."
				};
		assertTrue(testConjunctiveQuery(program, query, expected));
	}

}
