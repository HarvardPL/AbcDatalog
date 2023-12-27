package edu.harvard.seas.pl.abcdatalog.ast.validation;

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
import edu.harvard.seas.pl.abcdatalog.ast.NegatedAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.Premise;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator.ValidClause;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.DefaultConjunctVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.HeadVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.HeadVisitorBuilder;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogParseException;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogParser;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogTokenizer;
import edu.harvard.seas.pl.abcdatalog.util.graph.Digraph;
import edu.harvard.seas.pl.abcdatalog.util.graph.DirectedEdge;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

class StratifiedNegationGraph {
  private static class AnnotatedEdge implements DirectedEdge<PredicateSym> {
    private final PredicateSym source;
    private final PredicateSym dest;
    private final boolean isNegated;

    public AnnotatedEdge(PredicateSym source, PredicateSym dest, boolean isNegated) {
      this.source = source;
      this.dest = dest;
      this.isNegated = isNegated;
    }

    @Override
    public PredicateSym getSource() {
      return this.source;
    }

    @Override
    public PredicateSym getDest() {
      return this.dest;
    }

    public boolean isNegated() {
      return this.isNegated;
    }

    public AnnotatedEdge reverse() {
      return new AnnotatedEdge(this.dest, this.source, this.isNegated);
    }
  }

  public static StratifiedNegationGraph create(UnstratifiedProgram program)
      throws DatalogValidationException {
    // Build dependency graph...
    Digraph<PredicateSym, AnnotatedEdge> graph = new Digraph<>();
    HeadVisitor<Void, PredicateSym> getHeadPred =
        (new HeadVisitorBuilder<Void, PredicateSym>())
            .onPositiveAtom((atom, nothing) -> atom.getPred())
            .orCrash();
    PremiseVisitor<PredicateSym, Void> addEdge =
        new DefaultConjunctVisitor<PredicateSym, Void>() {
          @Override
          public Void visit(PositiveAtom atom, PredicateSym headPred) {
            if (!program.getEdbPredicateSyms().contains(atom.getPred())) {
              graph.addEdge(new AnnotatedEdge(atom.getPred(), headPred, false));
            }
            return null;
          }

          @Override
          public Void visit(NegatedAtom atom, PredicateSym headPred) {
            if (!program.getEdbPredicateSyms().contains(atom.getPred())) {
              graph.addEdge(new AnnotatedEdge(atom.getPred(), headPred, true));
            }
            return null;
          }
        };
    for (ValidClause cl : program.getRules()) {
      PredicateSym pred = cl.getHead().accept(getHeadPred, null);
      graph.addVertex(pred);
      for (Premise c : cl.getBody()) {
        c.accept(addEdge, pred);
      }
    }

    List<Set<PredicateSym>> strata = graph.getStronglyConnectedComponents(e -> e.reverse());
    Map<PredicateSym, Integer> predToStratumMap = new HashMap<>();
    Iterator<Set<PredicateSym>> iter = strata.iterator();
    for (int i = 0; iter.hasNext(); ++i) {
      for (PredicateSym pred : iter.next()) {
        predToStratumMap.put(pred, i);
      }
    }

    iter = strata.iterator();
    for (int i = 0; iter.hasNext(); ++i) {
      for (PredicateSym pred : iter.next()) {
        for (AnnotatedEdge edge : graph.getOutgoingEdges(pred)) {
          if (edge.isNegated() && predToStratumMap.get(edge.getDest()) == i) {
            throw new DatalogValidationException("Program cannot be stratified.");
          }
        }
      }
    }

    return new StratifiedNegationGraph(strata, predToStratumMap);
  }

  private final List<Set<PredicateSym>> strata;
  private final Map<PredicateSym, Integer> predToStratumMap;

  private StratifiedNegationGraph(
      List<Set<PredicateSym>> strata, Map<PredicateSym, Integer> predToStratumMap) {
    this.strata = strata;
    this.predToStratumMap = predToStratumMap;
  }

  public List<Set<PredicateSym>> getStrata() {
    return strata;
  }

  public Map<PredicateSym, Integer> getPredToStratumMap() {
    return predToStratumMap;
  }

  public static void main(String[] args) throws DatalogParseException {
    Consumer<String> test =
        program -> {
          DatalogTokenizer t = new DatalogTokenizer(new StringReader(program));
          Set<Clause> ast = null;
          try {
            ast = DatalogParser.parseProgram(t);
          } catch (DatalogParseException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          }

          System.out.println("Program: ");
          for (Clause cl : ast) {
            System.out.println("\t" + cl);
          }
          try {
            UnstratifiedProgram v =
                (new DatalogValidator()).withAtomNegationInRuleBody().validate(ast);

            StratifiedNegationGraph g = StratifiedNegationGraph.create(v);
            System.out.print("Stratification:\n\t" + g);
          } catch (DatalogValidationException e) {
            System.out.println("No stratification possible.");
          }

          System.out.println();
        };

    String program =
        "reachable(X,Y) :- edge(X,Y)."
            + "reachable(X,Y) :- edge(X,Z), reachable(Z,Y)."
            + "not_reachable(X,Y) :- node(X), node(Y), not reachable(X,Y)."
            + "node(X) :- edge(X,_). node(X) :- edge(_,X).";
    test.accept(program);
    test.accept("p :- q, not r. q :- r. r :- q.");
    test.accept("p :- not q. q :- not p.");
    test.accept("tc :- edge.");
    test.accept("p :- not q.");
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < this.strata.size(); ++i) {
      sb.append("[ " + i + ": ");
      for (PredicateSym pred : this.strata.get(i)) {
        sb.append(pred + " ");
      }
      sb.append("] ");
    }
    return sb.toString();
  }
}
