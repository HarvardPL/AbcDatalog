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
package abcdatalog.ast;

import abcdatalog.ast.visitors.HeadVisitor;

/**
 * The head of a clause. This interface is under-specified to allow the addition
 * of new language features.
 *
 */
public interface Head {
	public <I, O> O accept(HeadVisitor<I, O> visitor, I state);
}
