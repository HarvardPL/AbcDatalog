package edu.harvard.seas.pl.abcdatalog.executor;

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
import java.util.Set;

/**
 * A interface to an ongoing Datalog evaluation that allows for callbacks to be registered that are
 * invoked when relevant new facts are derived and for new EDB facts to be added in the midst of
 * evaluation.
 */
public interface DatalogExecutor {
  /**
   * Initializes the Datalog engine with a program and specifies which EDB relations can be extended
   * (with DatalogExecutor.addFactAsynchronously()) during evaluation. This should only be called
   * once.
   *
   * @param program the program
   * @param extendibleEdbPreds the extendible EDB relations
   * @throws DatalogValidationException if program is invalid
   */
  void initialize(Set<Clause> program, Set<PredicateSym> extendibleEdbPreds)
      throws DatalogValidationException;

  /**
   * Starts the Datalog evaluation.
   *
   * @throws IllegalStateException if the executor has not been initialized or the evaluation has
   *     already been started
   */
  void start();

  /**
   * Asynchronously adds a new EDB fact to the Datalog evaluation. The EDB fact must be part of a
   * relation that is specified in DatalogExecutor.initialize() as being extendible. A fact is a
   * ground atom (i.e., an atom without any variables).
   *
   * @param edbFact the new EDB fact
   * @throws IllegalStateException if the executor has not been initialized
   * @throws IllegalArgumentException if the provided atom is not ground, or if it is not part of a
   *     relation specified during initialization as being extendible
   */
  void addFactAsynchronously(PositiveAtom edbFact);

  /**
   * Associates a listener with a given predicate symbol, so that if any fact is derived during
   * evaluation with that predicate symbol, the listener will be invoked with that fact. The
   * listener can be executed in an arbitrary thread and should not block.
   *
   * @param p the predicate symbol
   * @param listener the listener
   * @throws IllegalStateException if the evaluation has already been started
   */
  void registerListener(PredicateSym p, DatalogListener listener);

  /** Shuts down the executor, which cannot be reused. */
  void shutdown();
}
