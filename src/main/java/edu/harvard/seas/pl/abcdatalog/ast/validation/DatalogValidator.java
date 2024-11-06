package edu.harvard.seas.pl.abcdatalog.ast.validation;

/*-
 * #%L
 * AbcDatalog
 * %%
 * Copyright (C) 2016 - 2021 President and Fellows of Harvard College
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the President and Fellows of Harvard College nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import edu.harvard.seas.pl.abcdatalog.ast.BinaryDisunifier;
import edu.harvard.seas.pl.abcdatalog.ast.BinaryUnifier;
import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.Constant;
import edu.harvard.seas.pl.abcdatalog.ast.Head;
import edu.harvard.seas.pl.abcdatalog.ast.HeadHelpers;
import edu.harvard.seas.pl.abcdatalog.ast.NegatedAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.Premise;
import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.ast.TermHelpers;
import edu.harvard.seas.pl.abcdatalog.ast.Variable;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.CrashPremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.HeadVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.HeadVisitorBuilder;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitorBuilder;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.TermVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.TermVisitorBuilder;
import edu.harvard.seas.pl.abcdatalog.util.Box;
import edu.harvard.seas.pl.abcdatalog.util.substitution.TermUnifier;
import edu.harvard.seas.pl.abcdatalog.util.substitution.UnionFindBasedUnifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A validator for a set of clauses. It converts a set of clauses to a program, which consists of a
 * set of initial facts and a set of rules for deriving new facts. The rules in a program are
 * guaranteed to be valid.
 */
public class DatalogValidator {
  private boolean allowBinaryUnification;
  private boolean allowBinaryDisunification;
  private boolean allowNegatedBodyAtom;

  public DatalogValidator withBinaryUnificationInRuleBody() {
    this.allowBinaryUnification = true;
    return this;
  }

  public DatalogValidator withBinaryDisunificationInRuleBody() {
    this.allowBinaryDisunification = true;
    return this;
  }

  public DatalogValidator withAtomNegationInRuleBody() {
    this.allowNegatedBodyAtom = true;
    return this;
  }

  public UnstratifiedProgram validate(Set<Clause> program) throws DatalogValidationException {
    return validate(program, false);
  }

  public UnstratifiedProgram validate(Set<Clause> program, boolean treatIdbFactsAsClauses)
      throws DatalogValidationException {
    Set<ValidClause> rewrittenClauses = new HashSet<>();
    for (Clause clause : program) {
      rewrittenClauses.add(checkRule(clause));
    }
    rewrittenClauses.add(new ValidClause(True.getTrueAtom(), Collections.emptyList()));

    Set<ValidClause> rules = new HashSet<>();
    Set<ValidClause> bodilessClauses = new HashSet<>();

    Set<PredicateSym> edbPredicateSymbols = new HashSet<>();
    Set<PredicateSym> idbPredicateSymbols = new HashSet<>();

    HeadVisitor<Void, PositiveAtom> getHeadAsAtom =
        (new HeadVisitorBuilder<Void, PositiveAtom>())
            .onPositiveAtom((atom, nothing) -> atom)
            .orCrash();
    PremiseVisitor<Void, Void> getBodyPred =
        (new PremiseVisitorBuilder<Void, Void>())
            .onPositiveAtom(
                (atom, nothing) -> {
                  edbPredicateSymbols.add(atom.getPred());
                  return null;
                })
            .onNegatedAtom(
                (atom, nothing) -> {
                  edbPredicateSymbols.add(atom.getPred());
                  return null;
                })
            .orNull();
    for (ValidClause cl : rewrittenClauses) {
      PositiveAtom head = cl.getHead().accept(getHeadAsAtom, null);
      List<Premise> body = cl.getBody();
      if (body.isEmpty()) {
        bodilessClauses.add(cl);
        edbPredicateSymbols.add(head.getPred());
      } else {
        idbPredicateSymbols.add(head.getPred());
        rules.add(cl);
        for (Premise c : body) {
          c.accept(getBodyPred, null);
        }
      }
    }

    Set<PositiveAtom> initialFacts = new HashSet<>();
    edbPredicateSymbols.removeAll(idbPredicateSymbols);
    for (ValidClause cl : bodilessClauses) {
      PositiveAtom head = HeadHelpers.forcePositiveAtom(cl.getHead());
      if (treatIdbFactsAsClauses && idbPredicateSymbols.contains(head.getPred())) {
        rules.add(cl);
      } else {
        initialFacts.add(head);
      }
    }

    return new Program(rules, initialFacts, edbPredicateSymbols, idbPredicateSymbols);
  }

  private ValidClause checkRule(Clause clause) throws DatalogValidationException {
    Set<Variable> boundVars = new HashSet<>();
    Set<Variable> possiblyUnboundVars = new HashSet<>();
    TermUnifier subst = new UnionFindBasedUnifier();

    TermVisitor<Set<Variable>, Set<Variable>> tv =
        (new TermVisitorBuilder<Set<Variable>, Set<Variable>>())
            .onVariable(
                (x, set) -> {
                  set.add(x);
                  return set;
                })
            .or((x, set) -> set);

    TermHelpers.fold(
        HeadHelpers.forcePositiveAtom(clause.getHead()).getArgs(), tv, possiblyUnboundVars);

    Box<Boolean> hasPositiveAtom = new Box<>(false);
    PremiseVisitor<DatalogValidationException, DatalogValidationException> cv =
        new CrashPremiseVisitor<DatalogValidationException, DatalogValidationException>() {

          @Override
          public DatalogValidationException visit(PositiveAtom atom, DatalogValidationException e) {
            TermHelpers.fold(atom.getArgs(), tv, boundVars);
            hasPositiveAtom.value = true;
            return e;
          }

          @Override
          public DatalogValidationException visit(BinaryUnifier u, DatalogValidationException e) {
            if (!allowBinaryUnification) {
              return new DatalogValidationException("Binary unification is not allowed: ");
            }
            TermHelpers.fold(u.getArgsIterable(), tv, possiblyUnboundVars);
            TermHelpers.unify(u.getLeft(), u.getRight(), subst);
            return e;
          }

          @Override
          public DatalogValidationException visit(
              BinaryDisunifier u, DatalogValidationException e) {
            if (!allowBinaryDisunification) {
              return new DatalogValidationException("Binary disunification is not allowed: ");
            }
            TermHelpers.fold(u.getArgsIterable(), tv, possiblyUnboundVars);
            return e;
          }

          @Override
          public DatalogValidationException visit(NegatedAtom atom, DatalogValidationException e) {
            if (!allowNegatedBodyAtom) {
              return new DatalogValidationException("Negated body atoms are not allowed: ");
            }
            TermHelpers.fold(atom.getArgs(), tv, possiblyUnboundVars);
            return e;
          }
        };

    for (Premise c : clause.getBody()) {
      DatalogValidationException e = c.accept(cv, null);
      if (e != null) {
        throw e;
      }
    }

    for (Variable v : new HashSet<>(boundVars)) {
      Term t = subst.get(v);
      if (t instanceof Variable) {
        boundVars.add((Variable) t);
      }
    }

    for (Variable x : possiblyUnboundVars) {
      Term t;
      if (!boundVars.contains(x) && !((t = subst.get(x)) instanceof Constant) && !(boundVars.contains(t))) {
        throw new DatalogValidationException(
            "Every variable in a rule must be bound, but "
                + x
                + " is not bound in the rule "
                + clause
                + " A variable X is bound if 1) it appears in a positive (non-negated) body atom, or 2) it is explicitly unified with a constant (e.g., X=a) or with a variable that is bound (e.g., X=Y, where Y is bound).");
      }
    }

    List<Premise> newBody = new ArrayList<>(clause.getBody());
    if (!hasPositiveAtom.value && !newBody.isEmpty()) {
      newBody.add(0, True.getTrueAtom());
    }

    return new ValidClause(clause.getHead(), newBody);
  }

  private static final class Program implements UnstratifiedProgram {
    private final Set<ValidClause> rules;
    private final Set<PositiveAtom> initialFacts;
    private final Set<PredicateSym> edbPredicateSymbols;
    private final Set<PredicateSym> idbPredicateSymbols;

    public Program(
        Set<ValidClause> rules,
        Set<PositiveAtom> initialFacts,
        Set<PredicateSym> edbPredicateSymbols,
        Set<PredicateSym> idbPredicateSymbols) {
      this.rules = rules;
      this.initialFacts = initialFacts;
      this.edbPredicateSymbols = edbPredicateSymbols;
      this.idbPredicateSymbols = idbPredicateSymbols;
    }

    public Set<ValidClause> getRules() {
      return this.rules;
    }

    public Set<PositiveAtom> getInitialFacts() {
      return this.initialFacts;
    }

    public Set<PredicateSym> getEdbPredicateSyms() {
      return this.edbPredicateSymbols;
    }

    public Set<PredicateSym> getIdbPredicateSyms() {
      return this.idbPredicateSymbols;
    }
  }

  public static final class True {

    private True() {
      // Cannot be instantiated.
    }

    private static class TruePred extends PredicateSym {

      protected TruePred() {
        super("true", 0);
      }
    }
    ;

    private static final PositiveAtom trueAtom = PositiveAtom.create(new TruePred(), new Term[] {});

    public static PositiveAtom getTrueAtom() {
      return trueAtom;
    }
  }

  public static final class ValidClause extends Clause {

    private ValidClause(Head head, List<Premise> body) {
      super(head, body);
    }
  }
}
