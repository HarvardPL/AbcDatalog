package edu.harvard.seas.pl.abcdatalog.engine.topdown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.harvard.seas.pl.abcdatalog.ast.Constant;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.ast.Variable;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator.ValidClause;
import edu.harvard.seas.pl.abcdatalog.engine.testing.ConjunctiveQueryTests;
import edu.harvard.seas.pl.abcdatalog.engine.testing.CoreTests;

/**
 * A Datalog evaluation engine that uses an iterative version of the
 * query-subquery top-down technique.
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	IterativeQsqEngine.MyCoreTests.class,
	IterativeQsqEngine.MyConjunctiveQueryTests.class
})
public class IterativeQsqEngine extends AbstractQsqEngine {

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

		// Create initial input for QSQI algorithm.
		Map<AdornedClause, QsqSupRelation> supRelations = new LinkedHashMap<>();
		Map<AdornedPredicateSym, Set<AdornedClause>> adornedRules = new LinkedHashMap<>();
		Map<AdornedPredicateSym, Set<QsqSupRelation>> dependencies = new LinkedHashMap<>();

		AdornedPredicateSym p = new AdornedPredicateSym(q.getPred(), adornment);
		Set<AdornedClause> initialRules = generateAdornedRules(p);
		if (initialRules.isEmpty()) {
			// The query predicate is not recognized.
			return Collections.emptySet();
		}
		adornedRules.put(p, initialRules);
		for (AdornedClause rule : initialRules) {
			supRelations.put(rule, generateSupRelations(rule, dependencies));
		}

		Map<AdornedPredicateSym, Relation> answers = new LinkedHashMap<>();
		Stack<SidewaysInfo> newSupInfo = new Stack<>();
		Stack<SidewaysInfo> newAnsInfo = new Stack<>();
		Stack<TopDownInfo> newInput = new Stack<>();
		Relation r = new Relation(p.getBound());
		r.add(new Tuple(input));
		newInput.push(new TopDownInfo(p, r));

		// This is the main QSQI evaluation loop. New work items (i.e.
		// information to be processed) are added as previous work items are
		// handled. Once there are no longer any work items to process, the
		// Datalog evaluation has finished.
		while (true) {
			if (!newInput.isEmpty()) {
				// Case 1: there is new top-down input.
				handleNewInput(newInput.pop(), newSupInfo, newAnsInfo, adornedRules, supRelations, answers,
						dependencies);
			} else if (!newSupInfo.isEmpty()) {
				// Case 2: New tuples are being passed to a supplementary
				// relation.
				handleNewSupInfo(newSupInfo.pop(), newSupInfo, newInput, newAnsInfo, answers, dependencies);
			} else if (!newAnsInfo.isEmpty()) {
				// Case 3: New answers have been generated for an atom.
				handleNewAnsInfo(newAnsInfo.pop(), newSupInfo);
			} else {
				// Case 4: Nothing left to do, so the evaluation is complete.
				break;
			}
		}

		Set<PositiveAtom> results = new LinkedHashSet<>();
		Relation ans = answers.get(p);
		if (ans != null) {
			for (Tuple fact : ans) {
				if (fact.unify(new Tuple(q.getArgs())) != null) {
					results.add(PositiveAtom.create(q.getPred(), fact.elts));
				}
			}
		}

		return results;
	}

	/**
	 * Handles the case when new information is being passed down into all the
	 * rules that define the adorned predicate symbol in info.
	 * 
	 * @param info
	 *            the new information
	 * @param newSupInfo
	 *            collects work items related to when information is passed onto
	 *            another supplemental relation
	 * @param newAnsInfo
	 *            collects work items related to when new answers have been
	 *            generated for an atom
	 * @param adornedRules
	 *            a map from an adorned predicate symbol to the rules that
	 *            define it
	 * @param supRelations
	 *            a map from an adorned rule to its first supplementary relation
	 * @param answers
	 *            all the answers that have been generated, mapped by adorned
	 *            predicate symbol
	 * @param dependencies
	 *            a map from an adorned predicate symbol to the supplementary
	 *            relations that follow atoms with that symbol
	 */
	private void handleNewInput(TopDownInfo info, Collection<SidewaysInfo> newSupInfo,
			Collection<SidewaysInfo> newAnsInfo, Map<AdornedPredicateSym, Set<AdornedClause>> adornedRules,
			Map<AdornedClause, QsqSupRelation> supRelations, Map<AdornedPredicateSym, Relation> answers,
			Map<AdornedPredicateSym, Set<QsqSupRelation>> dependencies) {
		Set<AdornedClause> rules = adornedRules.get(info.pred);

		// Lazily create adorned rules and supplementary relations.
		if (rules == null) {
			rules = generateAdornedRules(info.pred);
			adornedRules.put(info.pred, rules);
			for (AdornedClause rule : rules) {
				supRelations.put(rule, generateSupRelations(rule, dependencies));
			}
			answers.put(info.pred, new Relation(info.pred.getArity()));
		}

		// Predicate is not recognized.
		if (rules.isEmpty()) {
			return;
		}

		for (AdornedClause rule : rules) {
			// See which input tuples actually unify with the head.
			Tuple headTuple = new Tuple(rule.getHead().getArgs());
			Relation newIn = info.newInfo
					.filter(tup -> applyBoundArgs(headTuple, rule.getHead().getPred().getAdornment(), tup)
							.unify(headTuple) != null);

			// Handle special case of explicit IDB fact.
			if (rule.getBody().isEmpty()) {
				Relation ans = getAnswers(answers, rule.getHead().getPred());
				if (!ans.contains(headTuple)) {
					ans.add(headTuple);
					propagateToDependencies(rule.getHead().getPred(), info.newInfo, newAnsInfo, dependencies);
				}
				continue;
			}

			// Push tuples that unify with the head into the first
			// supplementary relation for that rule.
			if (!newIn.isEmpty()) {
				// Need to project only bound variables (this is an
				// issue if the rule head has a constant in a bound
				// position).
				List<Boolean> isBoundVar = new ArrayList<>();
				for (int i = 0; i < rule.getHead().getPred().getArity(); ++i) {
					if (rule.getHead().getPred().getAdornment().get(i)) {
						isBoundVar.add(rule.getHead().getArgs()[i] instanceof Variable);
					}
				}
				newIn = newIn.project(isBoundVar);

				newSupInfo.add(new SidewaysInfo(supRelations.get(rule), newIn));
			}
		}
	}

	/**
	 * Handles the case when the supplementary relation in info has been passed
	 * new tuples.
	 * 
	 * @param info
	 *            the new information
	 * @param newSupInfo
	 *            collects work items related to when information is passed onto
	 *            another supplemental relation
	 * @param newInput
	 *            collects work items related to when information is passed to
	 *            all the rules defining an adorned predicate symbol
	 * @param newAnsInfo
	 *            collects work items related to when new answers have been
	 *            generated for an atom
	 * @param answers
	 *            all the answers that have been generated, mapped by adorned
	 *            predicate symbol
	 * @param dependencies
	 *            a map from an adorned predicate symbol to the supplementary
	 *            relations that follow atoms with that symbol
	 */
	private void handleNewSupInfo(SidewaysInfo info, Collection<SidewaysInfo> newSupInfo,
			Collection<TopDownInfo> newInput, Collection<SidewaysInfo> newAnsInfo,
			Map<AdornedPredicateSym, Relation> answers, Map<AdornedPredicateSym, Set<QsqSupRelation>> dependencies) {
		info.newInfo.removeAll(info.sup);
		info.sup.addAll(info.newInfo);
		if (info.newInfo.isEmpty()) {
			return; // Nothing new.
		}

		// At the end of rule, so new tuples are answers to a subquery.
		if (info.sup.next == null) {
			Relation oldAnswers = answers.get(info.sup.nextAtom.getPred());
			
			AdornedAtom ruleHead = info.sup.nextAtom;
			Relation newInfo = info.newInfo.applyTuplesAsSubstitutions(new Tuple(ruleHead.getArgs()));
			
			if (oldAnswers != null) {
				newInfo.removeAll(oldAnswers);
			}

			if (!newInfo.isEmpty()) {
				// Note: by construction, info.sup.nextAtom points to the head
				// of the rule.
				getAnswers(answers, ruleHead.getPred()).addAll(newInfo);
				propagateToDependencies(ruleHead.getPred(), newInfo, newAnsInfo, dependencies);
			}
			return;
		}

		// Process how new tuples added to this supplementary relation
		// play out in the following atom.
		AdornedAtom a = info.sup.nextAtom;
		// TODO what's up with this??
		// Relation EDBs = this.edbRelations.get(new PredicateSym(a.pred));
		Relation EDBs = this.edbRelations.get(a.getPred().getUnadorned());
		if (EDBs != null) {
			// We have an EDB predicate.
			// TODO clunky
			EDBs.renameAttributes(new TermSchema(Arrays.asList(a.getArgs())));
			EDBs = EDBs.filter(tup -> tup.unify(new Tuple(a.getArgs())) != null);

			Relation newOut = info.sup.joinAndProject(EDBs, info.sup.next.getAttributes());
			newSupInfo.add(new SidewaysInfo(info.sup.next, newOut));
		} else {
			// We have an IDB predicate.
			Relation ans = answers.get(a.getPred());
			if (ans != null) {
				// Push information sideways into next supplementary relation.
				ans = ans.filter(tup -> tup.unify(new Tuple(a.getArgs())) != null);
				// TODO clunky
				ans.renameAttributes(new TermSchema(Arrays.asList(a.getArgs())));

				Relation out = info.newInfo.joinAndProject(ans, info.sup.next.getAttributes());
				if (!out.isEmpty()) {
					newSupInfo.add(new SidewaysInfo(info.sup.next, out));
				}
			}

			// Push the new tuples into the next atom (i.e. top-down
			// information passing).
			info.newInfo.renameAttributes(new TermSchema(info.sup.getAttributes()));
			Relation newTopDownInput = new Relation(a.getPred().getArity());
			for (Tuple tup : info.newInfo.applyTuplesAsSubstitutions(new Tuple(a.getArgs()))) {
				newTopDownInput.add(tup);
			}

			newTopDownInput = newTopDownInput.project(a.getPred().getAdornment());

			if (!newTopDownInput.isEmpty()) {
				newInput.add(new TopDownInfo(a.getPred(), newTopDownInput));
			}
		}
	}

	/**
	 * Handles the case when an atom has new answers. The supplementary relation
	 * in info immediately precedes the atom. The new answers for the atom are
	 * joined with all the tuples in the supplementary relation to determine
	 * which new tuples to pass onto the supplementary relation that follows the
	 * atom.
	 * 
	 * @param info
	 *            the new info
	 * @param newSupInfo
	 *            collects work items related to when information is passed onto
	 *            another supplementary relation
	 */
	private void handleNewAnsInfo(SidewaysInfo info, Collection<SidewaysInfo> newSupInfo) {
		AdornedAtom a = info.sup.nextAtom;
		Relation ans = new Relation(info.newInfo);
		ans.filter(tup -> tup.unify(new Tuple(a.getArgs())) != null);
		
		// TODO clunky
		ans.renameAttributes(new TermSchema(Arrays.asList(a.getArgs())));
		Relation out = info.sup.joinAndProject(ans, info.sup.next.getAttributes());
		
		if (!out.isEmpty()) {
			newSupInfo.add(new SidewaysInfo(info.sup.next, out));
		}
	}

	/**
	 * Alert all the supplementary relations that precede an atom with the
	 * adorned predicate p that that atom has new answers.
	 * 
	 * @param p
	 *            the adorned predicate
	 * @param newInfo
	 *            the new answers
	 * @param newAnsInfo
	 *            collects work items related to when new answers have been
	 *            generated for an atom
	 * @param dependencies
	 *            a map from an adorned predicate symbol to the supplementary
	 *            relations that follow atoms with that symbol
	 */
	private void propagateToDependencies(AdornedPredicateSym p, Relation newInfo, Collection<SidewaysInfo> newAnsInfo,
			Map<AdornedPredicateSym, Set<QsqSupRelation>> dependencies) {
		Set<QsqSupRelation> d = dependencies.get(p);
		if (d != null) {
			for (QsqSupRelation sup : d) {
				newAnsInfo.add(new SidewaysInfo(sup, new Relation(newInfo)));
			}
		}
	}

	/**
	 * Gets the answer relation for the adorned predicate symbol p, creating a
	 * new relation if necessary.
	 * 
	 * @param answers
	 *            a map from an adorned predicate symbol to answer relation
	 * @param p
	 *            the adorned predicate symbol
	 * @return the answer relation
	 */
	private Relation getAnswers(Map<AdornedPredicateSym, Relation> answers, AdornedPredicateSym p) {
		Relation ans = answers.get(p);
		if (ans == null) {
			ans = new Relation(p.getArity());
			answers.put(p, ans);
		}
		return ans;
	}

	/**
	 * Generates all the adorned rules that define an adorned predicate symbol.
	 * 
	 * @param pred
	 *            the predicate symbol
	 * @return the rules, or null if there are no relevant rules
	 */
	private Set<AdornedClause> generateAdornedRules(AdornedPredicateSym pred) {
		Set<AdornedClause> rules = new LinkedHashSet<>();
		Set<ValidClause> unadornedRules = this.idbRules.get(pred.getUnadorned());
		if (unadornedRules != null) {
			for (ValidClause c : unadornedRules) {
				rules.add(AdornedClause.fromClause(pred.getAdornment(), c));
			}
		}
		return rules;
	}

	/**
	 * Given an adorned rule, generates a linked list of supplementary relations
	 * for that rule, and updates the dependencies map so that the adorned
	 * predicate symbol of each atom in the body of the rule points to the
	 * supplementary relation that precedes it.
	 * 
	 * @param rule
	 *            the adorned rule
	 * @param dependencies
	 *            the dependencies map
	 * @return the linked list of supplementary relations
	 */
	private QsqSupRelation generateSupRelations(AdornedClause rule,
			Map<AdornedPredicateSym, Set<QsqSupRelation>> dependencies) {
		// Handle special case of explicit IDB facts (i.e. body-less rules with
		// an IDB predicate in the head).
		if (rule.getBody().isEmpty()) {
			QsqSupRelation sup = new QsqSupRelation(new TermSchema(new ArrayList<>()));
			sup.next = null;
			sup.nextAtom = rule.getHead();
			return sup;
		}

		QsqSupRelation first = null;
		QsqTemplate templ = new QsqTemplate(rule);
		QsqSupRelation prev = null;
		for (int i = 0; i < templ.size(); ++i) {
			QsqSupRelation sup = new QsqSupRelation(templ.get(i));
			if (prev != null) {
				prev.next = sup;
			}

			if (i == 0) {
				first = sup;
			}
			if (i < templ.size() - 1) {
				// Supplemental relation should point to the atom that follows,
				// and the dependency map needs to be updated accordingly.
				sup.nextAtom = rule.getBody().get(i);
				Set<QsqSupRelation> d = dependencies.get(sup.nextAtom.getPred());
				if (d == null) {
					d = new LinkedHashSet<>();
					dependencies.put(sup.nextAtom.getPred(), d);
				}
				d.add(sup);
			} else {
				// The last supplementary relation points back to the head of
				// the rule, which allows us to easily identify which rule is
				// being processed.
				sup.next = null;
				sup.nextAtom = rule.getHead();
			}
			prev = sup;
		}
		return first;
	}

	/**
	 * Information (i.e. new tuples) that is being passed top-down (i.e. from a
	 * supplementary relation into the head of the rules that define an adorned
	 * predicate).
	 *
	 */
	private class TopDownInfo {
		/**
		 * The adorned predicate symbol of the rules that are being passed the
		 * information.
		 */
		public AdornedPredicateSym pred;
		/**
		 * The new information that is being passed.
		 */
		public Relation newInfo;

		/**
		 * Constructs an object representing that the rules that define the
		 * adorned predicate symbol pred are being passed the tuples in newInfo.
		 * 
		 * @param pred
		 *            the adorned predicate symbol
		 * @param newInfo
		 *            the new information
		 */
		public TopDownInfo(AdornedPredicateSym pred, Relation newInfo) {
			if (pred.getBound() != newInfo.arity) {
				throw new IllegalArgumentException(
						"Arity of input relation must equal number of bindings " + "in predicate adornment.");
			}
			this.pred = pred;
			this.newInfo = newInfo;
		}
	}

	/**
	 * Information (i.e. new tuples) that is being passed sideways across
	 * supplementary relations and the intervening atom.
	 *
	 */
	private class SidewaysInfo {
		/**
		 * The supplementary relation that is receiving the new tuples, or the
		 * supplementary relation previous to the atom that is being passed the
		 * new tuples.
		 */
		public QsqSupRelation sup;
		/**
		 * The new information.
		 */
		public Relation newInfo;

		/**
		 * Constructs an object representing that the supplementary relation sup
		 * (or the atom following it) is being passed the tuples in newInfo.
		 * 
		 * @param sup
		 *            the supplementary relation
		 * @param newInfo
		 *            the new information
		 */
		public SidewaysInfo(QsqSupRelation sup, Relation newInfo) {
			this.sup = sup;
			this.newInfo = newInfo;
		}
	}

	public static class MyCoreTests extends CoreTests {

		public MyCoreTests() {
			super(() -> new IterativeQsqEngine());
		}

	}
	
	public static class MyConjunctiveQueryTests extends ConjunctiveQueryTests {

		public MyConjunctiveQueryTests() {
			super(() -> new IterativeQsqEngine());
		}

	}

}
