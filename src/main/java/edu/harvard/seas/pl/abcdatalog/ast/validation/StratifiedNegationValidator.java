package edu.harvard.seas.pl.abcdatalog.ast.validation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator.ValidClause;

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
