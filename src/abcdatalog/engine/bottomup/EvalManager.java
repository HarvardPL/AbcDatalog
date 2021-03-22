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
package abcdatalog.engine.bottomup;

import java.util.Set;

import abcdatalog.ast.Clause;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.util.datastructures.IndexableFactCollection;

/**
 * The saturating evaluation manager for a bottom-up Datalog evaluation engine.
 *
 */
public interface EvalManager {
	/**
	 * Initialize this manager with a program.
	 * 
	 * @param program
	 *            the program
	 * @throws DatalogValidationException
	 *             if the program is invalid
	 */
	void initialize(Set<Clause> program) throws DatalogValidationException;

	/**
	 * Saturate all facts derivable from the program with which this manager has
	 * been initialized.
	 * 
	 * @param program
	 *            the program
	 * @return the facts
	 */
	IndexableFactCollection eval();
}
