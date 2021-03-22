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
package abcdatalog.engine.bottomup;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import abcdatalog.ast.BinaryDisunifier;
import abcdatalog.ast.BinaryUnifier;
import abcdatalog.ast.Clause;
import abcdatalog.ast.Constant;
import abcdatalog.ast.NegatedAtom;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.ast.Premise;
import abcdatalog.ast.Term;
import abcdatalog.ast.TermHelpers;
import abcdatalog.ast.Variable;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.ast.validation.DatalogValidator;
import abcdatalog.ast.validation.UnstratifiedProgram;
import abcdatalog.ast.visitors.CrashHeadVisitor;
import abcdatalog.ast.visitors.CrashPremiseVisitor;
import abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator.SemiNaiveClause;
import abcdatalog.util.substitution.ClauseSubstitution;

/**
 * This class provides a way to derive all the new facts that are derivable from
 * a given rule, given an initial fact that unifies with the first atom in the
 * body of the clause. It is the workhorse of the bottom-up evaluation engines.
 *
 */
public class ClauseEvaluator {
	// TODO We can make this smarter by using fact that ahead of time we know
	// which terms are going to be variables and which are going to be constant,
	// so we can skip checks.
	private final BiConsumer<PositiveAtom, ClauseSubstitution> newFact;
	private final BiFunction<AnnotatedAtom, ClauseSubstitution, Iterable<PositiveAtom>> getFacts;
	private final ClauseSubstitution substTemplate;
	private final Consumer<PositiveAtom> firstAction;

	public ClauseEvaluator(SemiNaiveClause cl, BiConsumer<PositiveAtom, ClauseSubstitution> newFact,
			BiFunction<AnnotatedAtom, ClauseSubstitution, Iterable<PositiveAtom>> getFacts) {
		assert !cl.getBody().isEmpty();
		this.newFact = newFact;
		this.getFacts = getFacts;
		this.substTemplate = new ClauseSubstitution(cl);

		Consumer<ClauseSubstitution> secondAction = makeAction(cl, 1);
		this.firstAction = cl.getBody().get(0).accept(new CrashPremiseVisitor<Void, Consumer<PositiveAtom>>() {
			@Override
			public Consumer<PositiveAtom> visit(AnnotatedAtom atom, Void nothing) {
				return fact -> {
					ClauseSubstitution s = substTemplate.getCleanCopy();
					if (unifyAtomWithFact(atom.asUnannotatedAtom(), fact, s)) {
						secondAction.accept(s);
					}
				};
			}
		}, null);
	}

	private Consumer<ClauseSubstitution> makeAction(SemiNaiveClause cl, int i) {
		if (i == cl.getBody().size()) {
			return cl.getHead().accept(new CrashHeadVisitor<Void, Consumer<ClauseSubstitution>>() {
				@Override
				public Consumer<ClauseSubstitution> visit(PositiveAtom head, Void nothing) {
					return s -> newFact.accept(head, s);
				}
			}, null);
		}

		Consumer<ClauseSubstitution> nextAction = makeAction(cl, i + 1);

		return cl.getBody().get(i).accept(new CrashPremiseVisitor<Integer, Consumer<ClauseSubstitution>>() {
			@Override
			public Consumer<ClauseSubstitution> visit(AnnotatedAtom atom, Integer i) {
				return s -> {
					s.resetState(i); // TODO is this necessary?
					Iterator<PositiveAtom> iter = getFacts.apply(atom, s).iterator();
					while (iter.hasNext()) {
						s.resetState(i);
						PositiveAtom fact = iter.next();
						if (unifyAtomWithFact(atom.asUnannotatedAtom(), fact, s)) {
							nextAction.accept(s);
						}
					}
				};
			}

			@Override
			public Consumer<ClauseSubstitution> visit(NegatedAtom atom, Integer i) {
				return s -> {
					Iterator<PositiveAtom> iter = getFacts
							.apply(new AnnotatedAtom(atom.asPositiveAtom(), AnnotatedAtom.Annotation.IDB), s)
							.iterator();
					while (iter.hasNext()) {
						s.resetState(i);
						PositiveAtom fact = iter.next();
						if (unifyAtomWithFact(atom.asPositiveAtom(), fact, s)) {
							return;
						}
					}
					nextAction.accept(s);
				};
			}

			@Override
			public Consumer<ClauseSubstitution> visit(BinaryUnifier u, Integer i) {
				return s -> {
					if (TermHelpers.unify(u.getLeft(), u.getRight(), s)) {
						nextAction.accept(s);
					}
				};
			}

			@Override
			public Consumer<ClauseSubstitution> visit(BinaryDisunifier u, Integer i) {
				return s -> {
					if (!TermHelpers.unify(u.getLeft(), u.getRight(), s)) {
						nextAction.accept(s);
					}
				};
			}
		}, i);
	}

	private boolean unifyAtomWithFact(PositiveAtom atom, PositiveAtom fact, ClauseSubstitution s) {
		assert atom.getPred().equals(fact.getPred());
		Term[] atomArgs = atom.getArgs();
		Term[] factArgs = fact.getArgs();
		for (int i = 0; i < atomArgs.length; ++i) {
			// if (!unifyTerms(atomArgs[i], factArgs[i], s)) {
			if (!TermHelpers.unify(atomArgs[i], factArgs[i], s)) {
				return false;
			}
		}
		return true;
	}

	public void evaluate(PositiveAtom newFact) {
		this.firstAction.accept(newFact);
	}

	public static void main(String[] args) {
		Constant a = Constant.create("a");
		Constant b = Constant.create("b");
		Variable x = Variable.create("X");
		Variable y = Variable.create("Y");
		Variable z = Variable.create("Z");
		Variable w = Variable.create("W");

		PredicateSym p = PredicateSym.create("p", 2);
		PredicateSym q = PredicateSym.create("q", 2);

		PositiveAtom qab = PositiveAtom.create(q, new Term[] { a, b });
		PositiveAtom qba = PositiveAtom.create(q, new Term[] { b, a });
		Iterable<PositiveAtom> facts = Arrays.asList(qab, qba);
		PositiveAtom qXY = PositiveAtom.create(q, new Term[] { x, y });
		PositiveAtom qZW = PositiveAtom.create(q, new Term[] { z, w });
		PositiveAtom qYW = PositiveAtom.create(q, new Term[] { y, w });
		PositiveAtom pXW = PositiveAtom.create(p, new Term[] { x, w });

		System.out.println("Database consists of: ");
		for (PositiveAtom fact : facts) {
			System.out.println(fact);
			;
		}
		System.out.print("\n\"Input\" fact is: ");
		System.out.println(qab);
		System.out.println("\n----------");

		Consumer<List<Premise>> test = body -> {
			System.out.print("\nTesting: ");
			Clause cl = new Clause(pXW, body);
			UnstratifiedProgram prog = null;
			try {
				prog = (new DatalogValidator()).withBinaryUnificationInRuleBody().withBinaryDisunificationInRuleBody()
						.validate(Collections.singleton(cl));
			} catch (DatalogValidationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			SemiNaiveClauseAnnotator annotator = new SemiNaiveClauseAnnotator(prog.getIdbPredicateSyms());
			SemiNaiveClause ordered = annotator.annotate(prog.getRules().iterator().next()).iterator().next();
			System.out.println(ordered);
			ClauseEvaluator eval = new ClauseEvaluator(ordered, (fact, s) -> System.out.println(fact.applySubst(s)),
					(atom, s) -> facts);
			eval.evaluate(qab);
		};

		test.accept(Arrays.asList(qXY, qZW));
		test.accept(Arrays.asList(qXY, qYW));
		BinaryUnifier uZY = new BinaryUnifier(z, y);
		test.accept(Arrays.asList(qXY, qZW, uZY));
		BinaryUnifier uXZ = new BinaryUnifier(x, z);
		test.accept(Arrays.asList(qXY, qZW, uXZ));
		BinaryUnifier uXY = new BinaryUnifier(x, y);
		test.accept(Arrays.asList(qXY, qZW, uXY));
		BinaryDisunifier dZY = new BinaryDisunifier(z, y);
		test.accept(Arrays.asList(qXY, qZW, dZY));
		BinaryDisunifier dWY = new BinaryDisunifier(w, y);
		test.accept(Arrays.asList(qXY, qZW, dWY));
	}
}
