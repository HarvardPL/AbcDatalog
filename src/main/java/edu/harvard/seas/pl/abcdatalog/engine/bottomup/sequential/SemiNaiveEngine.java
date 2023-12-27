package edu.harvard.seas.pl.abcdatalog.engine.bottomup.sequential;

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

import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.Premise;
import edu.harvard.seas.pl.abcdatalog.engine.DatalogEngine;
import edu.harvard.seas.pl.abcdatalog.engine.DatalogEngineWithProvenance;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.BottomUpEngineFrameWithProvenance;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogParser;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogTokenizer;
import java.io.Reader;
import java.io.StringReader;
import java.util.Set;

/**
 * A Datalog engine that implements the classic semi-naive bottom-up evaluation algorithm. It
 * supports explicit unification and stratified negation.
 */
public class SemiNaiveEngine extends BottomUpEngineFrameWithProvenance {

  public static DatalogEngine newEngine() {
    return new SemiNaiveEngine(false);
  }

  public static DatalogEngineWithProvenance newEngineWithProvenance() {
    return new SemiNaiveEngine(true);
  }

  public SemiNaiveEngine(boolean collectProv) {
    super(new SemiNaiveEvalManager(collectProv));
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
