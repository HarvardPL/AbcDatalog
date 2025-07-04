package edu.harvard.seas.pl.abcdatalog.util.substitution;

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
import edu.harvard.seas.pl.abcdatalog.ast.NegatedAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.Premise;
import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.ast.TermHelpers;
import edu.harvard.seas.pl.abcdatalog.ast.Variable;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidationException;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator.ValidClause;
import edu.harvard.seas.pl.abcdatalog.ast.validation.UnstratifiedProgram;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.CrashPremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.TermVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.TermVisitorBuilder;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.AnnotatedAtom;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.SemiNaiveClauseAnnotator.SemiNaiveClause;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

/**
 * This is a substitution tailor-made for a particular clause. Mappings can only be added to the
 * substitution in the order in which the variables appear in the clause. The state of the
 * substitution can be reset (so that it "forgets" the most recent mappings).
 */
public class ClauseSubstitution implements ConstOnlySubstitution {
  private final Constant[] subst;
  private final Map<Variable, Integer> index;
  private final int[] indexByConj;
  private int pos = 0;
  private final int bodySize;

  public ClauseSubstitution(SemiNaiveClause c) {
    Map<Variable, Integer> idx = new HashMap<>();
    List<Integer> idxByConj = new ArrayList<>();

    TermVisitor<Integer, Integer> tv =
        (new TermVisitorBuilder<Integer, Integer>())
            .onVariable(
                (x, curCount) -> {
                  if (idx.get(x) == null) {
                    idx.put(x, curCount);
                    return curCount + 1;
                  }
                  return curCount;
                })
            .or((x, curCount) -> curCount);

    PremiseVisitor<Integer, Integer> varFinder =
        new CrashPremiseVisitor<Integer, Integer>() {
          @Override
          public Integer visit(AnnotatedAtom atom, Integer count) {
            idxByConj.add(count);
            return TermHelpers.fold(atom.getArgs(), tv, count);
          }

          @Override
          public Integer visit(BinaryUnifier u, Integer count) {
            idxByConj.add(count);
            return TermHelpers.fold(u.getArgsIterable(), tv, count);
          }

          @Override
          public Integer visit(BinaryDisunifier u, Integer count) {
            idxByConj.add(count);
            return TermHelpers.fold(u.getArgsIterable(), tv, count);
          }

          @Override
          public Integer visit(NegatedAtom atom, Integer count) {
            idxByConj.add(count);
            return TermHelpers.fold(atom.getArgs(), tv, count);
          }
        };

    int count = 0;
    for (Premise conj : c.getBody()) {
      count = conj.accept(varFinder, count);
    }

    this.subst = new Constant[count];
    this.index = idx;
    this.bodySize = c.getBody().size();
    this.indexByConj = new int[this.bodySize];
    Iterator<Integer> iter = idxByConj.iterator();
    for (int i = 0; i < this.bodySize; ++i) {
      this.indexByConj[i] = iter.next();
    }
  }

  private ClauseSubstitution(
      Constant[] subst, Map<Variable, Integer> index, int[] indexByConj, int pos, int bodySize) {
    this.subst = subst;
    this.index = index;
    this.indexByConj = indexByConj;
    this.pos = pos;
    this.bodySize = bodySize;
  }

  public ClauseSubstitution getCleanCopy() {
    return new ClauseSubstitution(
        new Constant[subst.length], this.index, this.indexByConj, 0, this.bodySize);
  }

  @Override
  public boolean add(Variable x, Constant c) {
    Integer idx = this.index.get(x);
    assert idx != null && idx == this.pos;
    this.subst[this.pos++] = c;
    return true;
  }

  @Override
  public Constant get(Variable x) {
    Integer idx = this.index.get(x);
    if (idx == null || idx >= this.pos) {
      return null;
    }
    return this.subst[idx];
  }

  public void resetState(int conj) {
    assert conj >= 0 && conj < this.bodySize;
    this.pos = this.indexByConj[conj];
  }

  @Override
  public String toString() {
    Set<Entry<Variable, Integer>> entries = this.index.entrySet();
    int curConj = 0;
    Variable[] orderedVars = new Variable[entries.size()];
    for (Entry<Variable, Integer> entry : entries) {
      orderedVars[entry.getValue()] = entry.getKey();
    }

    StringBuilder sb = new StringBuilder();
    sb.append("{ ");

    for (int i = 0; i < orderedVars.length; ++i) {
      if (this.pos == i) {
        sb.append("^ ");
      }
      while (curConj < this.bodySize && this.indexByConj[curConj] == i) {
        sb.append("<" + curConj++ + "> ");
      }
      sb.append(orderedVars[i]);
      sb.append(" -> ");
      Constant c = this.subst[i];
      if (c != null) {
        sb.append(c);
      } else {
        sb.append("None");
      }

      if (i < orderedVars.length - 1) {
        sb.append(", ");
      } else {
        sb.append(" ");
      }
    }

    sb.append("}");
    return sb.toString();
  }

  public static void main(String[] args) throws DatalogValidationException {
    PredicateSym p0 = PredicateSym.create("p", 0);
    PredicateSym q2 = PredicateSym.create("q", 2);
    PredicateSym r2 = PredicateSym.create("r", 2);
    PredicateSym s3 = PredicateSym.create("s", 3);

    Variable x, y, z;
    x = Variable.create("X");
    y = Variable.create("Y");
    z = Variable.create("Z");
    Constant a, b;
    a = Constant.create("a");
    b = Constant.create("b");

    Set<PredicateSym> idbPreds = new HashSet<>();
    idbPreds.add(q2);
    idbPreds.add(s3);

    PositiveAtom pAtom = PositiveAtom.create(p0, new Term[] {});
    Premise qAtom = PositiveAtom.create(q2, new Term[] {x, y});
    Premise rAtom = PositiveAtom.create(r2, new Term[] {x, x});
    Premise sAtom = PositiveAtom.create(s3, new Term[] {a, x, z});
    Premise u1 = new BinaryUnifier(x, b);
    Premise u2 = new BinaryUnifier(z, z);

    System.out.println("Testing creating substitutions...");
    Consumer<Set<SemiNaiveClause>> createAndPrint =
        clauses -> {
          for (SemiNaiveClause c : clauses) {
            System.out.println("Substitution for: " + c);
            System.out.print("> ");
            System.out.println(new ClauseSubstitution(c));
          }
        };
    for (PredicateSym pred : idbPreds) {
      System.out.println(pred);
    }
    DatalogValidator validator = (new DatalogValidator()).withBinaryUnificationInRuleBody();
    UnstratifiedProgram prog =
        validator.validate(
            Collections.singleton(new Clause(pAtom, Collections.singletonList(pAtom))));
    SemiNaiveClauseAnnotator annotator = new SemiNaiveClauseAnnotator(idbPreds);
    createAndPrint.accept(annotator.annotate(prog.getRules().iterator().next()));
    prog =
        validator.validate(
            Collections.singleton(new Clause(pAtom, Collections.singletonList(qAtom))));
    createAndPrint.accept(annotator.annotate(prog.getRules().iterator().next()));

    List<Premise> l = new ArrayList<>();
    l.add(qAtom);
    l.add(rAtom);
    ValidClause cl =
        validator
            .validate(Collections.singleton(new Clause(pAtom, l)))
            .getRules()
            .iterator()
            .next();
    createAndPrint.accept(annotator.annotate(cl));
    Collections.swap(l, 0, 1);
    createAndPrint.accept(annotator.annotate(cl));
    l.add(sAtom);
    createAndPrint.accept(annotator.annotate(cl));
    l.add(u1);
    createAndPrint.accept(annotator.annotate(cl));
    l.add(u2);
    createAndPrint.accept(annotator.annotate(cl));

    System.out.println("\nTesting adding...");
    l.clear();
    l.add(qAtom);
    l.add(sAtom);
    idbPreds.clear();
    cl =
        validator
            .validate(Collections.singleton(new Clause(pAtom, l)))
            .getRules()
            .iterator()
            .next();
    ClauseSubstitution subst = new ClauseSubstitution(annotator.annotate(cl).iterator().next());
    System.out.println(subst);
    subst.add(x, a);
    System.out.println(subst);
    try {
      subst.add(z, a);
      System.err.println("BAD");
    } catch (AssertionError e) {
      System.out.println("Correctly threw error when adding in wrong order");
    }
    subst.add(y, b);
    System.out.println(subst);
    try {
      subst.add(x, a);
      System.err.println("BAD");
    } catch (AssertionError e) {
      System.out.println("Correctly threw error when adding in wrong order");
    }
    subst.add(z, a);
    System.out.println(subst);
    try {
      subst.add(Variable.create("w"), b);
      System.err.println("BAD");
    } catch (AssertionError e) {
      System.out.println("Correctly threw error when adding a variable not in substitution");
    }

    System.out.println("\nTesting getting and resetting...");
    subst.resetState(0);
    assert subst.get(x) == null;
    subst.add(x, a);
    assert subst.get(x).equals(a);
    subst.add(y, b);
    assert subst.get(x).equals(a);
    assert subst.get(y).equals(b);
    subst.add(z, a);
    assert subst.get(x).equals(a);
    assert subst.get(y).equals(b);
    assert subst.get(z).equals(a);
    subst.resetState(1);
    assert subst.get(x).equals(a);
    assert subst.get(y).equals(b);
    assert subst.get(z) == null;
  }

  @Override
  public Term[] apply(Term[] original) {
    Term[] terms = new Term[original.length];
    for (int i = 0; i < original.length; ++i) {
      Term t = original[i];
      Term s;
      if (t instanceof Variable && (s = this.get((Variable) t)) != null) {
        t = s;
      }
      terms[i] = t;
    }
    return terms;
  }
}
