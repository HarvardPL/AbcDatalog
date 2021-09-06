package edu.harvard.seas.pl.abcdatalog.engine.topdown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.runners.Suite;

import edu.harvard.seas.pl.abcdatalog.ast.Constant;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.ast.Variable;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator.ValidClause;
import edu.harvard.seas.pl.abcdatalog.engine.testing.ConjunctiveQueryTests;
import edu.harvard.seas.pl.abcdatalog.engine.testing.CoreTests;

import org.junit.runner.RunWith;

/**
 * A Datalog evaluation engine that uses a recursive version of the
 * query-subquery top-down technique.
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	RecursiveQsqEngine.MyCoreTests.class,
	RecursiveQsqEngine.MyConjunctiveQueryTests.class
})
public class RecursiveQsqEngine extends AbstractQsqEngine {

	/**
	 * A container for tracking global information passed back and forth between
	 * recursion frames.
	 *
	 */
	private class QSQRState {
		/**
		 * Tracks the answer tuples generated for each adorned predicate.
		 */
		private final Map<AdornedPredicateSym, Relation> ans;
		/**
		 * Tracks which input tuples have been used for each rule.
		 */
		private final Map<AdornedClause, Relation> inputByRule;
		/**
		 * Holds all the adorned rules for a given adorned predicate.
		 */
		private final Map<AdornedPredicateSym, Set<AdornedClause>> adornedRules;
		/**
		 * Holds all the unadorned rules for a given predicate.
		 */
		private final Map<PredicateSym, Set<ValidClause>> unadornedRules;
		/**
		 * Tracks the total number of input tuples that have been tried.
		 */
		private int inputCount;
		/**
		 * Tracks the total number of answer tuples that have been generated.
		 */
		private int ansCount;

		/**
		 * Initializes state with a set of all unadorned rules for the program.
		 * 
		 * @param unadornedRules
		 *            set of unadorned rules
		 */
		public QSQRState(Map<PredicateSym, Set<ValidClause>> unadornedRules) {
			this.ans = new LinkedHashMap<>();
			this.inputByRule = new LinkedHashMap<>();
			this.adornedRules = new LinkedHashMap<>();
			this.unadornedRules = unadornedRules;
			this.inputCount = 0;
			this.ansCount = 0;
		}

		/**
		 * Return relevant rules for adorned predicate p, generating them if needed.
		 * 
		 * @param p
		 *            adorned predicate
		 * @return set of adorned rules
		 */
		public Set<AdornedClause> getAdornedRules(AdornedPredicateSym p) {
			Set<AdornedClause> rules = this.adornedRules.get(p);
			// Lazily create adorned rules.
			if (rules == null) {
				rules = new LinkedHashSet<>();
				Set<ValidClause> unadornedRules = this.unadornedRules.get(p
						.getUnadorned());
				// No applicable rules for predicate...
				if (unadornedRules == null) {
					return null;
				}
				for (ValidClause c : unadornedRules) {
					AdornedClause adornedRule = AdornedClause.fromClause(p.getAdornment(), c);
					rules.add(adornedRule);
					this.inputByRule.put(adornedRule, new Relation(p.getBound()));
				}
				adornedRules.put(p, rules);
			}
			return rules;
		}

		/**
		 * Get the current input count.
		 * 
		 * @return current input count
		 */
		public int getInputCount() {
			return this.inputCount;
		}

		/**
		 * Get the current answer count.
		 * 
		 * @return current answer count
		 */
		public int getAnsCount() {
			return this.ansCount;
		}

		/**
		 * Given a rule and a relation, returns a subset of that relation containing
		 * the tuples that have not yet been processed for this rule.
		 * 
		 * @param rule
		 *            rule
		 * @param newTuples
		 *            relation
		 * @return subset of input relation
		 */
		public Relation filterNewInput(AdornedClause rule, Relation newTuples) {
			Relation delta = new Relation(newTuples);
			delta.removeAll(this.getInput(rule));
			return delta;
		}

		/**
		 * Adds tuples in delta to the extant input relation for this rule,
		 * increasing the input count by the number of new tuples in delta.
		 * 
		 * @param rule
		 *            rule
		 * @param delta
		 *            input relation
		 * @return number of new tuples added
		 */
		public int addToInput(AdornedClause rule, Relation delta) {
			Relation input = this.getInput(rule);
			int oldSize = input.size();
			input.addAll(delta);
			int diff = input.size() - oldSize;
			this.inputCount += diff;
			return diff;
		}

		/**
		 * Retrieves the current input relation for rule.
		 * 
		 * @param rule
		 *            rule
		 * @return input relation
		 */
		public Relation getInput(AdornedClause rule) {
			Relation r = this.inputByRule.get(rule);
			if (r == null) {
				r = new Relation(rule.getHead().getPred().getBound());
				this.inputByRule.put(rule, r);
			}
			return r;
		}

		/**
		 * Adds the tuples in supplied relation to the answer relation for the
		 * adorned predicate p, increasing the answer count for that predicate by
		 * the number of new tuples.
		 * 
		 * @param p
		 *            adorned predicate
		 * @param newTuples
		 *            relation
		 */
		public void updateAns(AdornedPredicateSym p, Relation newTuples) {
			Relation answer = this.getAns(p);
			int oldSize = answer.size();
			answer.addAll(newTuples);
			this.ansCount += answer.size() - oldSize;
		}

		/**
		 * Adds the supplied tuple to the answer relation for the adorned predicate
		 * p, increasing the answer count for that predicate if the tuple is new.
		 * 
		 * @param p
		 *            adorned predicate
		 * @param newTuple
		 *            tuple
		 */
		public void updateAns(AdornedPredicateSym p, Tuple newTuple) {
			if (this.getAns(p).add(newTuple)) {
				++this.ansCount;
			}
		}

		/**
		 * Retrieves the current answer relation for the adorned predicate p.
		 * 
		 * @param p
		 *            adorned predicate
		 * @return answer relation
		 */
		public Relation getAns(AdornedPredicateSym p) {
			Relation r = this.ans.get(p);
			if (r == null) {
				r = new Relation(p.getArity());
				this.ans.put(p, r);
			}
			return r;
		}
	}
	
	@Override
	public Set<PositiveAtom> query(PositiveAtom q) {
		// Is the query for EDB facts?
		Set<PositiveAtom> edbFacts = checkIfEdbQuery(q);
		if (edbFacts != null) {
			return edbFacts;
		}

		// Figure out adornment from query.
		List<Boolean> adornment = new ArrayList<>();
		List<Term> input = new ArrayList<>();
		for (Term t : q.getArgs()) {
			if (t instanceof Constant) {
				adornment.add(true);
				input.add(t);
			} else {
				adornment.add(false);
			}
		}

		// Create initial input for QSQR algorithm.
		AdornedPredicateSym p = new AdornedPredicateSym(q.getPred(), adornment);
		Tuple t = new Tuple(input);
		Relation r = new Relation(input.size());
		r.add(t);
		QSQRState state = new QSQRState(this.idbRules);

		qsqr(p, r, state);

		Set<PositiveAtom> results = new LinkedHashSet<>();
		for (Tuple fact : state.getAns(p)) {
			if (fact.unify(new Tuple(q.getArgs())) != null) {
				results.add(PositiveAtom.create(q.getPred(), fact.elts));
			}
		}
		return results;
	}

	/**
	 * Evaluates the query represented by the adorned predicate p and the
	 * relation newInput.
	 * 
	 * @param p
	 *            adorned predicate of query
	 * @param newInput
	 *            input tuples
	 * @param state
	 *            current state of evaluation-wide variables
	 */
	private void qsqr(AdornedPredicateSym p, Relation newInput, QSQRState state) {
		Set<AdornedClause> rules = state.getAdornedRules(p);
		if (rules == null) {
			return;
		}

		// Calculate the new input per rule.
		Map<AdornedClause, Relation> newInputByRule = new LinkedHashMap<>();
		for (AdornedClause rule : rules) {
			Relation delta = state.filterNewInput(rule, newInput);
			newInputByRule.put(rule, delta);
		}

		int oldInputSize;
		int oldAnsSize;
		boolean firstTime = true;
		do {
			oldInputSize = state.getInputCount();
			oldAnsSize = state.getAnsCount();

			for (AdornedClause rule : rules) {
				Relation input = newInputByRule.get(rule);
				// Record that the rule has been called with the new input.
				if (firstTime) {
					// Adjust the record of the old input size to take into
					// account the new input we just added.
					oldAnsSize += state.addToInput(rule, input);
				}
				qsqrSubroutine(rule, input, state);
			}
			firstTime = false;
		} while (oldInputSize != state.getInputCount()
				|| oldAnsSize != state.getAnsCount());
	}

	/**
	 * Evaluates the supplied rule using the input tuples newInput.
	 * 
	 * @param rule
	 *            rule
	 * @param newInput
	 *            input tuples
	 * @param state
	 *            current state of evaluation-wide variables
	 */
	private void qsqrSubroutine(AdornedClause rule, Relation newInput,
			QSQRState state) {
		// See which input tuples actually unify with the head.
		Tuple headTuple = new Tuple(rule.getHead().getArgs());
		Relation sup = newInput.filter(t -> applyBoundArgs(headTuple,
				rule.getHead().getPred().getAdornment(), t).unify(headTuple) != null);

		// No new tuples to test, so exit.
		if (sup.isEmpty()) {
			return;
		}

		// Handles special case of explicit IDB fact. Note that we are currently
		// skipping the dependency stuff below.
		if (rule.getBody().isEmpty()) {
			state.updateAns(rule.getHead().getPred(), headTuple);
			return;
		}

		// Want projection only of bound *variables*. This handles the case
		// where a rule has a constant in a bound position.
		List<Boolean> isBoundVar = new ArrayList<>();
		for (int i = 0; i < rule.getHead().getPred().getArity(); ++i) {
			if (rule.getHead().getPred().getAdornment().get(i)) {
				isBoundVar.add(rule.getHead().getArgs()[i] instanceof Variable);
			}
		}
		sup = sup.project(isBoundVar);

		QsqTemplate templ = new QsqTemplate(rule);
		sup.renameAttributes(templ.get(0));

		// Process rule one atom/supplemental relation at a time.
		for (int i = 1; i < templ.size(); ++i) {
			AdornedAtom a = rule.getBody().get(i - 1);
			Relation facts = this.edbRelations.get(a.getPred().getUnadorned());
			if (facts != null) {
				// We have an EDB predicate.
				// TODO clunky
				facts.renameAttributes(new TermSchema(Arrays.asList(a.getArgs())));
				facts = facts.filter(t -> t.unify(new Tuple(a.getArgs())) != null);
				sup = sup.joinAndProject(facts, templ.get(i));
			} else {
				// We have an IDB predicate.
				Relation input = sup.applyTuplesAsSubstitutions(new Tuple(
						a.getArgs()));
				input = input.project(a.getPred().getAdornment());

				// Recurse down subquery.
				qsqr(a.getPred(), input, state);

				Relation answers = state.getAns(a.getPred());
				// TODO clunky
				answers.renameAttributes(new TermSchema(Arrays.asList(a.getArgs())));
				sup = answers.joinAndProject(sup, templ.get(i));
			}
		}

		// Create a substitution from the final supplementary relation.
		state.updateAns(rule.getHead().getPred(),
				sup.applyTuplesAsSubstitutions(new Tuple(rule.getHead().getArgs())));

	}
	
	public static class MyCoreTests extends CoreTests {
		
		public MyCoreTests() {
			super(() -> new RecursiveQsqEngine());
		}
		
	}
	
	public static class MyConjunctiveQueryTests extends ConjunctiveQueryTests {

		public MyConjunctiveQueryTests() {
			super(() -> new RecursiveQsqEngine());
		}

	}

}
