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

/**
 * A interface to an ongoing Datalog evaluation that allows for callbacks to be
 * registered that are invoked when relevant new facts are derived and for new
 * EDB facts to be added in the midst of evaluation.
 *
 */
public interface DatalogExecutor {
	/**
	 * Initializes the Datalog engine with a program and specifies which EDB
	 * relations can be extended (with DatalogExecutor.addFactAsynchronously())
	 * during evaluation. This should only be called once.
	 * 
	 * @param program
	 *            the program
	 * @param extendibleEdbPreds
	 *            the extendible EDB relations
	 * @throws DatalogValidationException
	 *             if program is invalid
	 */
	void initialize(Set<Clause> program, Set<PredicateSym> extendibleEdbPreds) throws DatalogValidationException;

	/**
	 * Starts the Datalog evaluation.
	 * 
	 * @throws IllegalStateException
	 *             if the executor has not been initialized or the evaluation
	 *             has already been started
	 */
	void start();

	/**
	 * Asynchronously adds a new EDB fact to the Datalog evaluation. The EDB
	 * fact must be part of a relation that is specified in
	 * DatalogExecutor.initialize() as being extendible. A fact is a ground atom
	 * (i.e., an atom without any variables).
	 * 
	 * @param edbFact
	 *            the new EDB fact
	 * 
	 * @throws IllegalStateException
	 *             if the executor has not been initialized
	 * @throws IllegalArgumentException
	 *             if the provided atom is not ground, or if it is not part of a
	 *             relation specified during initialization as being extendible
	 */
	void addFactAsynchronously(PositiveAtom edbFact);

	/**
	 * Associates a listener with a given predicate symbol, so that if any fact
	 * is derived during evaluation with that predicate symbol, the listener
	 * will be invoked with that fact. The listener can be executed in an
	 * arbitrary thread and should not block.
	 * 
	 * @param p
	 *            the predicate symbol
	 * @param listener
	 *            the listener
	 * @throws IllegalStateException
	 *             if the evaluation has already been started
	 */
	void registerListener(PredicateSym p, DatalogListener listener);

	/**
	 * Shuts down the executor, which cannot be reused.
	 * 
	 */
	void shutdown();
}
