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
package abcdatalog.util.datastructures;

import abcdatalog.ast.PositiveAtom;

public interface FactIndexer extends IndexableFactCollection {
	/**
	 * Adds a fact to the FactIndexer.
	 * 
	 * @param fact
	 *            a fact
	 */
	public void add(PositiveAtom fact);

	/**
	 * Adds some number of facts to the FactIndexer.
	 * 
	 * @param facts
	 *            some facts
	 */
	public void addAll(Iterable<PositiveAtom> facts);
}
