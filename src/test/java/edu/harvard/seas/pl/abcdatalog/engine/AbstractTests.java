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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidationException;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogParseException;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogParser;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogTokenizer;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

public abstract class AbstractTests {
  private final Supplier<DatalogEngine> engineFactory;

  public AbstractTests(Supplier<DatalogEngine> engineFactory) {
    this.engineFactory = engineFactory;
  }

  /**
   * Instantiates an engine with the given program. This wrapper makes it easy to switch engine
   * implementations.
   *
   * @param program the program
   * @return the engine
   */
  protected DatalogEngine initEngine(String program) {
    DatalogEngine e = engineFactory.get();
    try {
      e.init(parseCode(program));
    } catch (DatalogValidationException e1) {
      fail("Validation error: " + e1.getMessage());
    }
    return e;
  }

  protected DatalogEngine initEngineUnsafe(String program) throws DatalogValidationException {
    DatalogEngine e = engineFactory.get();
    e.init(parseCode(program));
    return e;
  }

  protected DatalogEngine initEngineUnsafe(Reader program) throws DatalogValidationException {
    DatalogEngine e = engineFactory.get();
    e.init(parseCode(program));
    return e;
  }

  /**
   * Builds a Datalog AST out of the string representation of the source code.
   *
   * @param src the source code
   * @return the AST representation of the code
   */
  protected Set<Clause> parseCode(String src) {
    return parseCode(new StringReader(src));
  }

  protected Set<Clause> parseCode(Reader reader) {
    DatalogTokenizer t = new DatalogTokenizer(reader);
    try {
      return DatalogParser.parseProgram(t);
    } catch (DatalogParseException e) {
      fail("Parsing error: " + e.getMessage());
      // Never reached.
      return null;
    }
  }

  /**
   * Builds a query AST out of the string representation of the query.
   *
   * @param q the string representation of the query
   * @return the AST representation of the query
   */
  protected PositiveAtom parseQuery(String q) {
    DatalogTokenizer t = new DatalogTokenizer(new StringReader(q));
    try {
      return DatalogParser.parseQuery(t);
    } catch (DatalogParseException e) {
      fail("Parsing error: " + e.getMessage());
      // Never reached.
      return null;
    }
  }

  /**
   * Builds an AST representation of a set of facts (i.e. atoms) out of a string representation of
   * Datalog facts.
   *
   * @param facts the string representation of the facts
   * @return the AST representation of the facts
   */
  protected Set<PositiveAtom> parseFacts(String facts) {
    DatalogTokenizer t = new DatalogTokenizer(new StringReader(facts));
    Set<PositiveAtom> r = new LinkedHashSet<>();
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

  protected void test(String program, String query, String expected)
      throws DatalogValidationException {
    DatalogEngine e = initEngineUnsafe(program);
    Set<PositiveAtom> rs = e.query(parseQuery(query));
    Set<PositiveAtom> facts = parseFacts(expected);
    assertEquals(rs.size(), facts.size());
    assertTrue(rs.containsAll(facts));
  }

  protected void testFile(String file, String query, String expected)
      throws DatalogValidationException {
    InputStream is = getClass().getClassLoader().getResourceAsStream(file);
    DatalogEngine e = initEngineUnsafe(new InputStreamReader(is));
    Set<PositiveAtom> rs = e.query(parseQuery(query));
    Set<PositiveAtom> facts = parseFacts(expected);
    assertEquals(rs.size(), facts.size());
    assertTrue(rs.containsAll(facts));
  }
}
