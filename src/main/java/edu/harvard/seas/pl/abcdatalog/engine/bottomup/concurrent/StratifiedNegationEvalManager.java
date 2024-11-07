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
import edu.harvard.seas.pl.abcdatalog.ast.Premise;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidationException;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator;
import edu.harvard.seas.pl.abcdatalog.ast.validation.StratifiedNegationValidator;
import edu.harvard.seas.pl.abcdatalog.ast.validation.StratifiedProgram;
import edu.harvard.seas.pl.abcdatalog.ast.validation.UnstratifiedProgram;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.HeadVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.HeadVisitorBuilder;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitorBuilder;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.AnnotatedAtom;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.ClauseEvaluator;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.EvalManager;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator.SemiNaiveClause;
import edu.harvard.seas.pl.abcdatalog.util.ExecutorServiceCounter;
import edu.harvard.seas.pl.abcdatalog.util.Utilities;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.ConcurrentFactIndexer;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.ConcurrentFactTrie;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.ConcurrentLinkedBag;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.IndexableFactCollection;
import edu.harvard.seas.pl.abcdatalog.util.substitution.ClauseSubstitution;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class StratifiedNegationEvalManager implements EvalManager {
  private final ExecutorServiceCounter handlerExecService =
      new ExecutorServiceCounter(Executors.newCachedThreadPool());
  private final ForkJoinPool saturationPool =
      new ForkJoinPool(
          Utilities.concurrency, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

  private final ConcurrentFactIndexer<ConcurrentLinkedBag<PositiveAtom>> facts =
      new ConcurrentFactIndexer<>(
          () -> new ConcurrentLinkedBag<>(),
          (bag, atom) -> bag.add(atom),
          () -> ConcurrentLinkedBag.emptyBag(),
          (bag) -> bag.size());
  private final ConcurrentFactTrie trie = new ConcurrentFactTrie();

  private final Map<PredicateSym, Set<Integer>> relevantStrataByPred = new HashMap<>();

  private final List<StratumHandler> handlers = new ArrayList<>();

  private StratifiedProgram stratProg;

  private static final int EDB_STRATUM = -1;

  @Override
  public void initialize(Set<Clause> program) throws DatalogValidationException {
    UnstratifiedProgram prog =
        (new DatalogValidator())
            .withBinaryDisunificationInRuleBody()
            .withBinaryUnificationInRuleBody()
            .withAtomNegationInRuleBody()
            .validate(program);
    stratProg = StratifiedNegationValidator.validate(prog);

    Map<PredicateSym, Integer> stratumByPred = new HashMap<>(stratProg.getPredToStratumMap());
    for (PredicateSym p : this.stratProg.getEdbPredicateSyms()) {
      stratumByPred.put(p, EDB_STRATUM);
    }

    PremiseVisitor<Void, PredicateSym> getPred =
        (new PremiseVisitorBuilder<Void, PredicateSym>())
            .onAnnotatedAtom((atom, nothing) -> atom.getPred())
            .orCrash();
    HeadVisitor<Void, PredicateSym> getHeadPred =
        (new HeadVisitorBuilder<Void, PredicateSym>())
            .onPositiveAtom((atom, nothing) -> atom.getPred())
            .orCrash();

    int nstrata = stratProg.getStrata().size();
    @SuppressWarnings("unchecked")
    Set<SemiNaiveClause>[] relevantRulesByStratum = new HashSet[nstrata];
    for (int i = 0; i < nstrata; ++i) {
      relevantRulesByStratum[i] = new HashSet<>();
    }
    SemiNaiveClauseAnnotator annotator =
        new SemiNaiveClauseAnnotator(stratProg.getIdbPredicateSyms());
    for (SemiNaiveClause rule : annotator.annotate(this.stratProg.getRules())) {
      PredicateSym headPred = rule.getHead().accept(getHeadPred, null);
      int stratum = stratumByPred.get(headPred);
      relevantRulesByStratum[stratum].add(rule);

      PredicateSym firstPred = rule.getBody().get(0).accept(getPred, null);
      Utilities.getSetFromMap(this.relevantStrataByPred, firstPred).add(stratum);
    }

    for (int i = 0; i < nstrata; ++i) {
      this.handlers.add(new StratumHandler(i, relevantRulesByStratum[i], stratumByPred));
    }
  }

  private void propagateStratumCompletion(int stratum) {
    for (StratumHandler handler : this.handlers) {
      handler.reportCompletedStratum(stratum);
    }
  }

  @Override
  public IndexableFactCollection eval() {
    for (PositiveAtom fact : this.stratProg.getInitialFacts()) {
      this.trie.add(fact);
      this.facts.add(fact);
      this.propagateNewFact(fact);
    }

    for (StratumHandler handler : handlers) {
      this.handlerExecService.submitTask(handler);
    }

    this.propagateStratumCompletion(EDB_STRATUM);
    this.handlerExecService.blockUntilFinished();
    this.handlerExecService.shutdownAndAwaitTermination();

    this.saturationPool.shutdown();
    boolean finished = false;
    do {
      try {
        finished = this.saturationPool.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } while (!finished);

    return this.facts;
  }

  private void propagateNewFact(PositiveAtom fact) {
    Set<Integer> relevantStrata = this.relevantStrataByPred.get(fact.getPred());
    if (relevantStrata != null) {
      for (Integer stratum : relevantStrata) {
        this.handlers.get(stratum).reportFact(fact);
      }
    }
  }

  private class StratumHandler implements Runnable {
    private final Set<Integer> posDependencies;
    private final Set<Integer> negDependencies;
    private final Map<PredicateSym, Set<ClauseEvaluator>> clauseEvaluatorsByFirstPred;
    private final int stratum;
    private volatile boolean running;
    private final Queue<PositiveAtom> queuedFacts = new ConcurrentLinkedQueue<>();
    private final ExecutorServiceCounter exec = new ExecutorServiceCounter(saturationPool);
    private final BlockingQueue<Integer> completedStrataFeed = new LinkedBlockingQueue<>();

    public StratumHandler(
        int stratum, Set<SemiNaiveClause> relevantRules, Map<PredicateSym, Integer> stratumByPred) {
      this.stratum = stratum;

      this.posDependencies = new HashSet<>();
      this.negDependencies = new HashSet<>();

      PremiseVisitor<Void, Boolean> addPred =
          (new PremiseVisitorBuilder<Void, Boolean>())
              .onAnnotatedAtom(
                  (atom, nothing) -> this.posDependencies.add(stratumByPred.get(atom.getPred())))
              .onNegatedAtom(
                  (atom, nothing) -> this.negDependencies.add(stratumByPred.get(atom.getPred())))
              .orNull();
      for (SemiNaiveClause rule : relevantRules) {
        for (Premise c : rule.getBody()) {
          c.accept(addPred, null);
        }
      }

      // Account for recursive rules.
      this.posDependencies.remove(this.stratum);

      this.clauseEvaluatorsByFirstPred = new HashMap<>();
      BiFunction<AnnotatedAtom, ClauseSubstitution, Iterable<PositiveAtom>> getFacts =
          (atom, s) -> facts.indexInto(atom.asUnannotatedAtom(), s);
      BiConsumer<PositiveAtom, ClauseSubstitution> newFact =
          (atom, s) -> {
            if (trie.add(atom, s)) {
              PositiveAtom f = atom.applySubst(s);
              facts.add(f);
              propagateNewFact(f);
            }
          };

      PremiseVisitor<Void, PredicateSym> getPred =
          (new PremiseVisitorBuilder<Void, PredicateSym>())
              .onAnnotatedAtom((atom, nothing) -> atom.getPred())
              .orCrash();
      for (SemiNaiveClause cl : relevantRules) {
        PredicateSym bodyPred = cl.getBody().get(0).accept(getPred, null);
        ClauseEvaluator ce = new ClauseEvaluator(cl, newFact, getFacts);
        Utilities.getSetFromMap(this.clauseEvaluatorsByFirstPred, bodyPred).add(ce);
      }
    }

    @Override
    public void run() {
      while (!this.negDependencies.isEmpty()) {
        try {
          int n = this.completedStrataFeed.take();
          this.negDependencies.remove(n);
          this.posDependencies.remove(n);
        } catch (InterruptedException e) {
          // do nothing
        }
      }

      this.running = true;

      while (!this.queuedFacts.isEmpty()) {
        this.evaluateWithNewFact(this.queuedFacts.remove());
      }

      while (!this.posDependencies.isEmpty()) {
        try {
          this.posDependencies.remove(this.completedStrataFeed.take());
        } catch (InterruptedException e) {
          // do nothing
        }
      }

      this.exec.blockUntilFinished();

      propagateStratumCompletion(this.stratum);
    }

    private void evaluateWithNewFact(PositiveAtom fact) {
      Set<ClauseEvaluator> ces = this.clauseEvaluatorsByFirstPred.get(fact.getPred());
      assert ces != null;
      for (ClauseEvaluator ce : ces) {
        this.exec.submitTask(
            new Runnable() {

              @Override
              public void run() {
                ce.evaluate(fact);
              }
            });
      }
    }

    public void reportFact(PositiveAtom fact) {
      if (!this.running) {
        this.queuedFacts.add(fact);
      }

      if (this.running) {
        this.evaluateWithNewFact(fact);
      }
    }

    public void reportCompletedStratum(int stratum) {
      try {
        this.completedStrataFeed.put(stratum);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}
