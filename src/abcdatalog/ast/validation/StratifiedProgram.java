package abcdatalog.ast.validation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import abcdatalog.ast.PredicateSym;

/**
 * A Datalog program that has been stratified; for instance, to support
 * stratified negation.
 *
 */
public interface StratifiedProgram extends UnstratifiedProgram {
	List<Set<PredicateSym>> getStrata();

	Map<PredicateSym, Integer> getPredToStratumMap();
}
