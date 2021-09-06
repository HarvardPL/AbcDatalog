package edu.harvard.seas.pl.abcdatalog.example;

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
