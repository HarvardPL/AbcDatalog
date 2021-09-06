package edu.harvard.seas.pl.abcdatalog.ast.validation;

import java.util.Set;

import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator.ValidClause;

/**
 * A Datalog program for which each rule and initial fact has been independently
 * validated, but the program as a whole has not been validated. That is, it
 * guarantees that each clause of a program is independently valid, but says
 * nothing about whether the clauses taken together make sense. This might be a
 * concern for language features such as negation, where certain dependencies
 * between clauses are undesirable.
 * 
 */
public interface UnstratifiedProgram {
	Set<ValidClause> getRules();

	Set<PositiveAtom> getInitialFacts();

	Set<PredicateSym> getEdbPredicateSyms();

	Set<PredicateSym> getIdbPredicateSyms();
}
