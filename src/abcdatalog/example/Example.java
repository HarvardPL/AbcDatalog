package abcdatalog.example;

import java.io.Reader;
import java.io.StringReader;
import java.util.Set;

import abcdatalog.ast.Clause;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.ast.Term;
import abcdatalog.ast.Variable;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.engine.DatalogEngine;
import abcdatalog.engine.bottomup.sequential.SemiNaiveEngine;
import abcdatalog.parser.DatalogParseException;
import abcdatalog.parser.DatalogParser;
import abcdatalog.parser.DatalogTokenizer;

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
