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
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.ClauseEvaluator;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator.SemiNaiveClause;
import edu.harvard.seas.pl.abcdatalog.executor.DatalogListener;
import edu.harvard.seas.pl.abcdatalog.util.Utilities;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.IndexableFactCollection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An evaluation manager for a concurrent semi-naive engine that runs asynchronously in the
 * background. Facts can be added to the evaluation after the engine has started. Clients can also
 * register listeners with the manager, which are invoked when meaningful facts are derived.
 */
public class ExtensibleBottomUpEvalManager extends BottomUpEvalManager {

  private final ConcurrentMap<PredicateSym, Set<DatalogListener>> listenerMap =
      Utilities.createConcurrentMap();

  private final Set<PredicateSym> extensiblePreds;

  private volatile boolean isInitialized = false, isEvaluated = false, isFinishing = false;
  private final AtomicInteger ongoingAdds = new AtomicInteger();
  final Object lock = new Object();

  /**
   * Constructs a concurrent semi-naive evaluation manager that supports the explicit addition of
   * facts during evaluation. The argument marks the predicates of the relations that are allowed to
   * receive new facts.
   *
   * @param extensiblePreds the predicates of the relations that can be extended by new facts
   */
  public ExtensibleBottomUpEvalManager(Set<PredicateSym> extensiblePreds) {
    this.extensiblePreds = extensiblePreds;
  }

  @Override
  public synchronized void initialize(Set<Clause> program) throws DatalogValidationException {
    if (this.isInitialized) {
      throw new IllegalStateException("Cannot initialize an evaluation manager more than once.");
    }
    UnstratifiedProgram prog =
        (new DatalogValidator())
            .withBinaryDisunificationInRuleBody()
            .withBinaryUnificationInRuleBody()
            .validate(program);
    initialFacts.addAll(prog.getInitialFacts());
    Set<PredicateSym> idbPreds = new HashSet<>(prog.getIdbPredicateSyms());
    idbPreds.addAll(this.extensiblePreds);

    SemiNaiveClauseAnnotator annotator = new SemiNaiveClauseAnnotator(idbPreds);
    // set up map from predicate sym to rules. this depends on the first
    // atom in the annotated rule body being the "delta" atom
    for (SemiNaiveClause cl : annotator.annotate(prog.getRules())) {
      Utilities.getSetFromMap(this.predToEvalMap, cl.getFirstAtom().getPred())
          .add(new ClauseEvaluator(cl, this::newFact, this::getFacts));
    }

    this.isInitialized = true;
  }

  /**
   * Starts this manager running in the background. Returns a view into the facts derived during
   * evaluation. Note, however, that this view might have inconsistent state while the evaluation is
   * ongoing.
   */
  @Override
  public synchronized IndexableFactCollection eval() {
    if (!this.isInitialized) {
      throw new IllegalStateException("Evaluation manager must be initialized before evaluation.");
    }
    if (this.isEvaluated) {
      throw new IllegalStateException("Evaluation cannot be performed more than once.");
    }
    this.isEvaluated = true;

    for (PositiveAtom fact : this.initialFacts) {
      this.addFact(fact);
    }
    return this.facts;
  }

  /**
   * Blocks until the evaluation is complete and returns the set of facts derived during evaluation.
   * Once this method has been called, the evaluation manager cannot be reused.
   *
   * @return the facts
   * @throws IllegalStateException if the evaluation was never started, or if it has already been
   *     finished (or is in the process of finishing).
   */
  public synchronized IndexableFactCollection finishAsynchronousEval() {
    if (!this.isEvaluated) {
      throw new IllegalStateException("Evaluation has not started, and so cannot be finished.");
    }
    if (this.isFinishing) {
      throw new IllegalStateException("Evaluation has already been finished.");
    }
    this.isFinishing = true;

    synchronized (this.lock) {
      while (this.ongoingAdds.get() > 0) {
        // wait until the current adds are done
        try {
          this.lock.wait();
        } catch (InterruptedException e) {
          // do nothing
        }
      }
    }

    this.exec.blockUntilFinished();
    this.exec.shutdownAndAwaitTermination();
    return this.facts;
  }

  /**
   * Add a fact to this evaluation manager. If the evaluation manager has not yet started
   * evaluation, then this fact will be added to the evaluation once it begins. If the evaluation
   * has already been finished with this.finishAsynchrousEval, the fact is ignored.
   *
   * @param fact the fact
   * @throws IllegalArgumentException if the given fact does not have a predicate that was specified
   *     as "extensible" during construction.
   * @throws IllegalStateException if evaluation has already finished.
   */
  public void addFact(PositiveAtom fact) {
    if (!this.extensiblePreds.contains(fact.getPred())) {
      throw new IllegalArgumentException(
          "Predicate " + fact.getPred().getSym() + " is not marked as extensible.");
    }

    this.ongoingAdds.incrementAndGet();

    if (this.isFinishing) {
      if (this.ongoingAdds.decrementAndGet() == 0) {
        // this was the last outstanding ongoing add
        synchronized (this.lock) {
          this.lock.notifyAll();
        }
      }
      // FIXME what's going on here?
      throw new IllegalStateException();
    }

    if (!this.isEvaluated) {
      this.initialFacts.add(fact);
    }
    // We need this second condition to account for a race with this.eval,
    // in which the above condition is true, but the initialFacts are
    // processed in this.eval before the given fact is added to that set.
    if (this.isEvaluated && this.trie.add(fact)) {
      this.facts.add(fact);
      this.processNewFact(fact);
    }

    if (this.ongoingAdds.decrementAndGet() == 0) {
      synchronized (this.lock) {
        this.lock.notifyAll();
      }
    }
  }

  @Override
  protected void processNewFact(PositiveAtom newFact) {
    super.processNewFact(newFact);
    Set<DatalogListener> s = this.listenerMap.get(newFact.getPred());
    if (s != null) {
      for (DatalogListener l : s) {
        this.exec.submitTask(
            new Runnable() {

              @Override
              public void run() {
                l.newFactDerived(newFact);
              }
            });
      }
    }
  }

  /**
   * Registers a listener with this manager. When a fact with predicate p is derived during
   * evaluation, the manager invokes the listener with that fact. The listener can be called from an
   * arbitrary thread. If the listener is registered after evaluation is started, it will not be
   * invoked on any facts that have already been derived.
   *
   * @param p the predicate to listen for
   * @param listener the listener
   */
  public void addListener(PredicateSym p, DatalogListener listener) {
    Set<DatalogListener> s = this.listenerMap.get(p);
    if (s == null) {
      s = Utilities.createConcurrentSet();
      Set<DatalogListener> existing = this.listenerMap.putIfAbsent(p, s);
      if (existing != null) {
        s = existing;
      }
    }
    s.add(listener);
  }
}
