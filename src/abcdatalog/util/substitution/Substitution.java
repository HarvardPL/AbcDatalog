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
package abcdatalog.util.substitution;

import abcdatalog.ast.Term;
import abcdatalog.ast.Variable;

/**
 * A mapping from variables to terms.
 *
 */
public interface Substitution {
	/**
	 * Apply this substitution to a list of terms, creating a new list.
	 * 
	 * @param original
	 *            the original list
	 * @return the new list
	 */
	Term[] apply(Term[] original);

	/**
	 * Retrieves the mapping of a variable.
	 * 
	 * @param x
	 *            the variable
	 * @return the term that the variable is bound to, or null if the variable
	 *         is not in the substitution
	 */
	Term get(Variable x);
}
