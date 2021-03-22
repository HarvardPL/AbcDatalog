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

import abcdatalog.ast.PositiveAtom;

/**
 * A callback that is registered with a Datalog executor and is invoked during
 * evaluation.
 *
 */
public interface DatalogListener {
	/**
	 * Is invoked when a relevant new fact is derived during Datalog evaluation.
	 * Note that fact.isGround() will be true (i.e., a fact is a ground atom).
	 * 
	 * @param fact
	 *            the new fact
	 */
	void newFactDerived(PositiveAtom fact);
}
