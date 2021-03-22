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

import abcdatalog.ast.Constant;
import abcdatalog.ast.Variable;

/**
 * A mapping from variables to constants. This is a restriction of a more
 * general substitution, which is from variables to terms.
 *
 */
public interface ConstOnlySubstitution extends Substitution {
	/**
	 * Retrieves the mapping of a variable.
	 * 
	 * @param x
	 *            the variable
	 * @return the constant that the variable is bound to, or null if the
	 *         variable is not in the substitution
	 */
	@Override
	Constant get(Variable x);

	/**
	 * Attempts to add a mapping to the substitution. Returns true if the
	 * mapping was made successfully (i.e., if the variable was not already
	 * mapped to another constant).
	 * 
	 * @param x
	 *            the variable
	 * @param c
	 *            the constant
	 * @return whether the mapping was successfully added
	 */
	boolean add(Variable x, Constant c);
}
