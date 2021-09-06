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

import java.util.Set;

import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidationException;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.concurrent.ExtensibleBottomUpEvalManager;

/**
 * A Datalog executor that runs the actual Datalog evaluation concurrently in
 * separate threads.
 *
 */
public class DatalogParallelExecutor implements DatalogExecutor {
	/**
	 * Whether the runner thread has been started. This is guarded by this
	 * executor's inherent lock.
	 */
	private volatile boolean isInitialized = false, isRunning = false;
	/**
	 * Whether executor has been initialized.
	 */
	/**
	 * The set of EDB relations that can be dynamically extended.
	 */
	private volatile Set<PredicateSym> extensibleEdbPreds;
	
	private volatile ExtensibleBottomUpEvalManager eval;

	@Override
	public synchronized void initialize(Set<Clause> program,
			Set<PredicateSym> extensibleEdbPreds) throws DatalogValidationException {
		if (this.isRunning) {
			throw new IllegalStateException(
					"Cannot initialize an executor that is already running).");
		}
		if (this.isInitialized) {
			throw new IllegalStateException("Executor already initialized.");
		}
		this.eval = new ExtensibleBottomUpEvalManager(extensibleEdbPreds);
		this.eval.initialize(program);
		this.extensibleEdbPreds = extensibleEdbPreds;
		this.isInitialized = true;
	}

	@Override
	public synchronized void start() {
		if (this.isRunning) {
			throw new IllegalStateException("Executor is already running.");
		}
		if (!this.isInitialized) {
			throw new IllegalStateException(
					"Executor has not been initialized.");
		}
		this.eval.eval();
		this.isRunning = true;
	}

	@Override
	public void shutdown() {
		this.eval.finishAsynchronousEval();
	}
	
	@Override
	public void addFactAsynchronously(PositiveAtom edbFact) {
		if (!this.isInitialized) {
			throw new IllegalStateException(
					"Executor must be initialized before adding facts.");
		}
		if (!edbFact.isGround()) {
			throw new IllegalArgumentException("Atom is not ground.");
		}
		if (!this.extensibleEdbPreds.contains(edbFact.getPred())) {
			throw new IllegalArgumentException(
					"Atom is not part of an extendible EDB relation.");
		}

		this.eval.addFact(edbFact);
	}

	@Override
	public synchronized void registerListener(PredicateSym p,
			DatalogListener listener) {
		this.eval.addListener(p, listener);
	}

}
