package edu.harvard.seas.pl.abcdatalog.example;

import java.io.Reader;
import java.io.StringReader;
import java.util.Set;

import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.ast.Variable;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidationException;
import edu.harvard.seas.pl.abcdatalog.engine.DatalogEngine;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.sequential.SemiNaiveEngine;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogParseException;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogParser;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogTokenizer;

public final class Example {

	private Example() {
		throw new AssertionError("impossible");
	}
	
	private static Reader readInProgram() {
		// Normally, you'd return a FileReader here if you're reading from a file. For
		// the sake of this example, we'll construct our Datalog program here instead.
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
		return new StringReader(sb.toString());
	}
	
	private static PositiveAtom makeQuery() throws DatalogParseException {
		PositiveAtom q;
		// We can construct a query by parsing text...
		String s = "tc(X, Y)?";
		DatalogTokenizer t = new DatalogTokenizer(new StringReader(s));
		q = DatalogParser.parseQuery(t);
		// Or we can construct it programmatically by building an AST.
		Variable x = Variable.create("X");
		Variable y = Variable.create("Y");
		PredicateSym p = PredicateSym.create("tc", 2);
		q = PositiveAtom.create(p, new Term[] { x, y });
		return q;
	}

	public static void main(String[] args) throws DatalogParseException, DatalogValidationException {
		Reader r = readInProgram();
		DatalogTokenizer t = new DatalogTokenizer(r);
		Set<Clause> prog = DatalogParser.parseProgram(t);
		// You can choose what sort of engine you want here.
		DatalogEngine e = SemiNaiveEngine.newEngine();
		e.init(prog);
		PositiveAtom q = makeQuery();
		Set<PositiveAtom> results = e.query(q);
		for (PositiveAtom result : results) {
			System.out.println(result);
		}
	}

}
