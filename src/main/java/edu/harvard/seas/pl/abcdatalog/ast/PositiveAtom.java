package edu.harvard.seas.pl.abcdatalog.ast;

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

import edu.harvard.seas.pl.abcdatalog.ast.visitors.HeadVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.util.substitution.SimpleConstSubstitution;
import edu.harvard.seas.pl.abcdatalog.util.substitution.Substitution;
import java.util.Arrays;

/** A non-negated atom; i.e., a predicate symbol, and a sequence of terms. */
public class PositiveAtom implements Premise, Head {
  /** Predicate symbol of this atom. */
  protected final PredicateSym pred;

  /** Arguments of this atom. */
  protected final Term[] args;

  /** Is the atom ground (i.e., all arguments are constants)? */
  protected volatile Boolean isGround;

  /**
   * A static factory method for the creation of atoms. Returns an atom with the provided predicate
   * symbol and arguments. The argument array becomes "owned" by this atom and should not be
   * modified.
   *
   * @param pred the predicate symbol
   * @param args the arguments
   * @return an atom with the provided predicate symbol and arguments
   */
  public static PositiveAtom create(final PredicateSym pred, final Term[] args) {
    return new PositiveAtom(pred, args);
  }

  /**
   * Constructs an atom from a predicate symbol and a list of arguments.
   *
   * @param pred predicate symbol
   * @param args arguments
   */
  protected PositiveAtom(final PredicateSym pred, final Term[] args) {
    this.pred = pred;
    this.args = args;
    if (pred.getArity() != args.length) {
      throw new IllegalArgumentException(
          "Arity of predicate symbol \""
              + pred
              + "\" is "
              + pred.getArity()
              + " but given "
              + args.length
              + " argument(s).");
    }
  }

  public Term[] getArgs() {
    return this.args;
  }

  public PredicateSym getPred() {
    return this.pred;
  }

  public boolean isGround() {
    Boolean isGround;
    if ((isGround = this.isGround) == null) {
      // This might do redundant work since we do not synchronize, but
      // it's still sound, and it's probably cheap enough that
      // synchronizing might be more expensive.
      boolean b = true;
      for (Term t : args) {
        b &= t instanceof Constant;
      }
      this.isGround = isGround = Boolean.valueOf(b);
    }
    return isGround;
  }

  /**
   * Attempts to unify this atom with a fact (i.e., a ground atom).
   *
   * @param fact the fact
   * @return a substitution, or null if the atoms do not unify
   */
  public Substitution unify(PositiveAtom fact) {
    assert fact.isGround();
    if (!this.getPred().equals(fact.getPred())) {
      return null;
    }
    return SimpleConstSubstitution.unify(this.args, fact.args);
  }

  /**
   * Apply a substitution to the terms in this atom.
   *
   * @param subst the substitution
   * @return a new atom with the substitution applied
   */
  @Override
  public PositiveAtom applySubst(Substitution subst) {
    return create(this.pred, subst.apply(this.args));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.pred);
    if (this.args.length != 0) {
      sb.append('(');
      for (int i = 0; i < this.args.length; ++i) {
        sb.append(this.args[i]);
        if (i < this.args.length - 1) {
          sb.append(", ");
        }
      }
      sb.append(')');
    }
    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(args);
    result = prime * result + pred.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PositiveAtom other = (PositiveAtom) obj;
    // This check relies on isGround being set to one of the static
    // attributes Boolean.TRUE or Boolean.FALSE.
    if (isGround != null && other.isGround != null && isGround != other.isGround) return false;
    if (!pred.equals(other.pred)) return false;
    if (!Arrays.equals(args, other.args)) return false;
    return true;
  }

  @Override
  public <I, O> O accept(PremiseVisitor<I, O> visitor, I state) {
    return visitor.visit(this, state);
  }

  @Override
  public <I, O> O accept(HeadVisitor<I, O> visitor, I state) {
    return visitor.visit(this, state);
  }
}
