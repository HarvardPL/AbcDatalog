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
package abcdatalog.ast.visitors;

import abcdatalog.ast.Constant;
import abcdatalog.ast.Variable;

public class DefaultTermVisitor<I, O> implements TermVisitor<I, O> {

	@Override
	public O visit(Variable t, I state) {
		return null;
	}

	@Override
	public O visit(Constant t, I state) {
		return null;
	}

}
