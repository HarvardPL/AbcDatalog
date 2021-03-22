/*******************************************************************************
 * This file is part of the AbcDatalog project.
 *
 * Copyright (c) 2016, Harvard University
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the BSD License which accompanies this distribution.
 *
 * The development of the AbcDatalog project has been supported by the 
 * National Science Foundation under Grant Nos. 1237235 and 1054172.
 *
 * See README for contributors.
 ******************************************************************************/
package abcdatalog.executor;

import java.util.Set;

import abcdatalog.ast.Clause;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.engine.bottomup.concurrent.ExtensibleBottomUpEvalManager;

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
