package edu.harvard.seas.pl.abcdatalog.engine.bottomup;

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
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidationException;
import edu.harvard.seas.pl.abcdatalog.engine.DatalogEngine;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.IndexableFactCollection;
import java.util.HashSet;
import java.util.Set;

/** A framework for a bottom-up Datalog engine. */
public class BottomUpEngineFrame<E extends EvalManager> implements DatalogEngine {
  /** The evaluation manager for this engine. */
  protected final E manager;

  /** The set of facts that can be derived from the current program. */
  private volatile IndexableFactCollection facts;

  /** Has the engine been initialized? */
  private volatile boolean isInitialized = false;

  /**
   * Constructs a bottom-up engine with the provided evaluation manager.
   *
   * @param manager the manager
   */
  public BottomUpEngineFrame(E manager) {
    this.manager = manager;
  }

  @Override
  public synchronized void init(Set<Clause> program) throws DatalogValidationException {
    if (this.isInitialized) {
      throw new IllegalStateException("Cannot initialize an engine more than once.");
    }

    this.manager.initialize(program);
    this.facts = this.manager.eval();
    this.isInitialized = true;
  }

  @Override
  public Set<PositiveAtom> query(PositiveAtom q) {
    if (!this.isInitialized) {
      throw new IllegalStateException("Engine must be initialized before it can be queried.");
    }

    Set<PositiveAtom> r = new HashSet<>();
    for (PositiveAtom a : this.facts.indexInto(q)) {
      if (q.unify(a) != null) {
        r.add(a);
      }
    }
    return r;
  }
}
