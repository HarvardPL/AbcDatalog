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
package abcdatalog.ast.validation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.ast.validation.DatalogValidator.ValidClause;

/**
 * A class for validating that an unstratified program can be successfully
 * stratified for negation.
 *
 */
public final class StratifiedNegationValidator {

	private StratifiedNegationValidator() {
		// Cannot be instantiated.
	}

	/**
	 * Validates that the given unstratified program can be stratified for
	 * negation and returns a witness stratified program.
	 * 
	 * @param prog
	 *            the unstratified program
	 * @return the stratified program
	 * @throws DatalogValidationException
	 *             if the given program cannot be stratified for negation
	 */
	public static StratifiedProgram validate(UnstratifiedProgram prog) throws DatalogValidationException {
		StratifiedNegationGraph g = StratifiedNegationGraph.create(prog);
		return new StratifiedProgram() {

			@Override
			public Set<ValidClause> getRules() {
				return prog.getRules();
			}

			@Override
			public Set<PositiveAtom> getInitialFacts() {
				return prog.getInitialFacts();
			}

			@Override
			public Set<PredicateSym> getEdbPredicateSyms() {
				return prog.getEdbPredicateSyms();
			}

			@Override
			public Set<PredicateSym> getIdbPredicateSyms() {
				return prog.getIdbPredicateSyms();
			}

			@Override
			public List<Set<PredicateSym>> getStrata() {
				return g.getStrata();
			}

			@Override
			public Map<PredicateSym, Integer> getPredToStratumMap() {
				return g.getPredToStratumMap();
			}

		};
	}

}
