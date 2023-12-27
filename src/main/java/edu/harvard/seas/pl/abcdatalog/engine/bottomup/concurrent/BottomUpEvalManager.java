package edu.harvard.seas.pl.abcdatalog.engine.bottomup.concurrent;

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
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidationException;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator;
import edu.harvard.seas.pl.abcdatalog.ast.validation.UnstratifiedProgram;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.AnnotatedAtom;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.ClauseEvaluator;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.EvalManager;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator.SemiNaiveClause;
import edu.harvard.seas.pl.abcdatalog.util.ExecutorServiceCounter;
import edu.harvard.seas.pl.abcdatalog.util.Utilities;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.ConcurrentFactTrie;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.FactIndexer;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.FactIndexerFactory;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.IndexableFactCollection;
import edu.harvard.seas.pl.abcdatalog.util.substitution.ClauseSubstitution;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

/**
 * An evaluation manager that implements a saturation algorithm similar to semi-naive evaluation. It
 * supports explicit unification.
 */
public class BottomUpEvalManager implements EvalManager {

  protected final Map<PredicateSym, Set<ClauseEvaluator>> predToEvalMap = new HashMap<>();
  protected final ExecutorServiceCounter exec =
      new ExecutorServiceCounter(
          new ForkJoinPool(
              Utilities.concurrency, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true));
  protected final FactIndexer facts = FactIndexerFactory.createConcurrentQueueFactIndexer();
  protected final Set<PositiveAtom> initialFacts = Utilities.createConcurrentSet();
  protected final ConcurrentFactTrie trie = new ConcurrentFactTrie();

  @Override
  public synchronized void initialize(Set<Clause> program) throws DatalogValidationException {
    UnstratifiedProgram prog =
        (new DatalogValidator())
            .withBinaryDisunificationInRuleBody()
            .withBinaryUnificationInRuleBody()
            .validate(program);
    initialFacts.addAll(prog.getInitialFacts());

    SemiNaiveClauseAnnotator annotator = new SemiNaiveClauseAnnotator(prog.getIdbPredicateSyms());
    // set up map from predicate sym to rules. this depends on the first
    // atom in the annotated rule body being the "delta" atom
    for (SemiNaiveClause cl : annotator.annotate(prog.getRules())) {
      Utilities.getSetFromMap(this.predToEvalMap, cl.getFirstAtom().getPred())
          .add(new ClauseEvaluator(cl, this::newFact, this::getFacts));
    }
  }

  @Override
  public synchronized IndexableFactCollection eval() {
    this.facts.addAll(this.initialFacts);
    for (PositiveAtom fact : this.initialFacts) {
      this.trie.add(fact);
    }
    this.processInitialFacts(this.initialFacts);
    this.exec.blockUntilFinished();
    this.exec.shutdownAndAwaitTermination();
    return this.facts;
  }

  protected void processInitialFacts(Set<PositiveAtom> facts) {
    for (PositiveAtom fact : facts) {
      this.processNewFact(fact);
    }
  }

  protected void processNewFact(PositiveAtom newFact) {
    Set<ClauseEvaluator> evals = this.predToEvalMap.get(newFact.getPred());
    if (evals != null) {
      for (ClauseEvaluator ce : evals) {
        Runnable task =
            new Runnable() {

              @Override
              public void run() {
                ce.evaluate(newFact);
              }
            };
        this.exec.submitTask(task);
      }
    }
  }

  protected Iterable<PositiveAtom> getFacts(AnnotatedAtom atom, ClauseSubstitution s) {
    return facts.indexInto(atom.asUnannotatedAtom(), s);
  }

  protected void newFact(PositiveAtom atom, ClauseSubstitution s) {
    if (trie.add(atom, s)) {
      PositiveAtom f = atom.applySubst(s);
      facts.add(f);
      processNewFact(f);
    }
  }
}
