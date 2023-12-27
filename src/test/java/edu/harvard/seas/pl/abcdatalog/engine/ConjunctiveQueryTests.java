package edu.harvard.seas.pl.abcdatalog.engine;

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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogParseException;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogParser;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogTokenizer;
import edu.harvard.seas.pl.abcdatalog.util.substitution.ConstOnlySubstitution;
import edu.harvard.seas.pl.abcdatalog.util.substitution.SubstitutionUtils;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.Test;

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
    String[] expected = {""};
    assertTrue(testConjunctiveQuery(program, query, expected));
  }

  @Test
  public void singletonQuery() {
    String program = "p(a,b). p(b,c). p(c,a). q(X,Y) :- p(X,Z), p(Z,Y).";
    String query = "q(X,Y).";
    String[] expected = {"q(a,c).", "q(b,a).", "q(c,b)."};
    assertTrue(testConjunctiveQuery(program, query, expected));
  }

  @Test
  public void sizeTwoQuery1() {
    String program = "p(a,b). p(b,c). p(c,a). q(X,Y) :- p(X,Z), p(Z,Y).";
    String query = "q(X,Y). q(Y,Z).";
    String[] expected = {"q(a,c). q(c,b).", "q(b,a). q(a,c).", "q(c,b). q(b,a)."};
    assertTrue(testConjunctiveQuery(program, query, expected));
  }

  @Test
  public void sizeTwoQuery2() {
    String program = "p(a,b). p(b,c). p(c,a). q(X,Y) :- p(X,Z), p(Z,Y).";
    String query = "q(X,Y). q(Y,X).";
    String[] expected = {};
    assertTrue(testConjunctiveQuery(program, query, expected));
  }

  @Test
  public void sizeThreeQuery1() {
    String program = "p(a,b). p(b,c). p(c,a). q(X,Y) :- p(X,Z), p(Z,Y).";
    String query = "q(X,Y). q(Y,Z). q(Z,W).";
    String[] expected = {
      "q(a,c). q(c,b). q(b,a).", "q(b,a). q(a,c). q(c,b).", "q(c,b). q(b,a). q(a,c)."
    };
    assertTrue(testConjunctiveQuery(program, query, expected));
  }

  @Test
  public void sizeThreeQuery2() {
    String program = "p(a,b). p(b,c). p(c,a). q(X,Y) :- p(X,Z), p(Z,Y).";
    String query = "q(X,Y). q(Y,Z). q(Z,X).";
    String[] expected = {
      "q(a,c). q(c,b). q(b,a).", "q(b,a). q(a,c). q(c,b).", "q(c,b). q(b,a). q(a,c)."
    };
    assertTrue(testConjunctiveQuery(program, query, expected));
  }
}
