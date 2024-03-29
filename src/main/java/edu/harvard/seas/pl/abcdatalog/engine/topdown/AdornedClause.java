package edu.harvard.seas.pl.abcdatalog.engine.topdown;

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

import edu.harvard.seas.pl.abcdatalog.ast.Constant;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.Premise;
import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.ast.Variable;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator.ValidClause;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.HeadVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitorBuilder;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.TermVisitor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** An adorned clause (i.e., a Horn clause where every atom is itself adorned). */
public final class AdornedClause {
  private final AdornedAtom head;
  private final List<AdornedAtom> body;

  /**
   * Constructs an adorned clause given an adorned atom for the head and a list of adorned atoms for
   * the body.
   *
   * @param head head atom of clause
   * @param body atoms for body of clause
   */
  public AdornedClause(AdornedAtom head, List<AdornedAtom> body) {
    this.head = head;
    this.body = body;
  }

  /**
   * Constructs an adorned clause given an adornment to apply to the head and clause to adorn. The
   * head adornment ripples left to right across the atoms in the body.
   *
   * @param headAdornment adornment for head atom. A true value implies that that term is bound,
   *     false implies free.
   * @param clause original clause
   */
  public static AdornedClause fromClause(List<Boolean> headAdornment, ValidClause clause) {
    HeadVisitor<Void, PositiveAtom> getHead =
        new HeadVisitor<Void, PositiveAtom>() {

          @Override
          public PositiveAtom visit(PositiveAtom atom, Void state) {
            return atom;
          }
        };
    PositiveAtom head = clause.getHead().accept(getHead, null);
    if (headAdornment.size() != head.getPred().getArity()) {
      throw new IllegalArgumentException(
          "Adornment of size "
              + headAdornment.size()
              + " given for a clause with a head of arity "
              + head.getPred().getArity()
              + ".");
    }

    // Determine which variables in head are bound.
    Set<Term> bound = new HashSet<>();
    Term[] args = head.getArgs();
    for (int i = 0; i < args.length; ++i) {
      if (headAdornment.get(i)) {
        bound.add(args[i]);
      }
    }

    // Determine adornment of each atom in body of rule, updating
    // binding information at each step.
    PremiseVisitor<Void, PositiveAtom> getBodyAtom =
        (new PremiseVisitorBuilder<Void, PositiveAtom>())
            .onPositiveAtom((atom, nothing) -> atom)
            .orCrash();
    List<PositiveAtom> body = new ArrayList<>();
    for (Premise c : clause.getBody()) {
      body.add(c.accept(getBodyAtom, null));
    }
    List<AdornedAtom> newAtoms = new ArrayList<>();
    for (PositiveAtom a : body) {
      Set<Term> newBound = new HashSet<>();
      List<Boolean> adornment = new ArrayList<>();
      TermVisitor<Void, Void> tv =
          new TermVisitor<Void, Void>() {

            @Override
            public Void visit(Variable t, Void state) {
              if (bound.contains(t)) {
                adornment.add(true);
              } else {
                adornment.add(false);
                newBound.add(t);
              }
              return null;
            }

            @Override
            public Void visit(Constant t, Void state) {
              adornment.add(true);
              return null;
            }
          };
      for (Term t : a.getArgs()) {
        t.accept(tv, null);
      }
      newAtoms.add(new AdornedAtom(new AdornedPredicateSym(a.getPred(), adornment), a.getArgs()));
      bound.addAll(newBound);
    }

    AdornedAtom newHead =
        new AdornedAtom(new AdornedPredicateSym(head.getPred(), headAdornment), args);
    return new AdornedClause(newHead, newAtoms);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(head);
    if (!body.isEmpty()) {
      sb.append(" :- ");
      for (int i = 0; i < body.size(); ++i) {
        sb.append(body.get(i));
        if (i < this.body.size() - 1) {
          sb.append(", ");
        }
      }
    }
    sb.append('.');
    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((body == null) ? 0 : body.hashCode());
    result = prime * result + ((head == null) ? 0 : head.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    AdornedClause other = (AdornedClause) obj;
    if (body == null) {
      if (other.body != null) return false;
    } else if (!body.equals(other.body)) return false;
    if (head == null) {
      if (other.head != null) return false;
    } else if (!head.equals(other.head)) return false;
    return true;
  }

  public List<AdornedAtom> getBody() {
    return body;
  }

  public AdornedAtom getHead() {
    return head;
  }
}
