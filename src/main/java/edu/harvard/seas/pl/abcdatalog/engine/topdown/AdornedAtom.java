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

import edu.harvard.seas.pl.abcdatalog.ast.Term;

/** An adorned atom (i.e., an atom where every argument is marked as either bound or free). */
public class AdornedAtom {
  private final AdornedPredicateSym pred;
  private final Term[] args;

  /**
   * Constructs an adorned atom with the given predicate symbol and arguments.
   *
   * @param pred adorned predicate symbol
   * @param args arguments
   */
  public AdornedAtom(AdornedPredicateSym pred, Term[] args) {
    this.pred = pred;
    this.args = args;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(pred);
    if (this.args.length > 0) {
      sb.append("(");
      for (int i = 0; i < args.length; ++i) {
        sb.append(args[i]);
        if (i < args.length - 1) {
          sb.append(", ");
        }
      }
      sb.append(")");
    }
    return sb.toString();
  }

  public AdornedPredicateSym getPred() {
    return pred;
  }

  public Term[] getArgs() {
    return args;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((args == null) ? 0 : args.hashCode());
    result = prime * result + ((pred == null) ? 0 : pred.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    AdornedAtom other = (AdornedAtom) obj;
    if (args == null) {
      if (other.args != null) return false;
    } else if (!args.equals(other.args)) return false;
    if (pred == null) {
      if (other.pred != null) return false;
    } else if (!pred.equals(other.pred)) return false;
    return true;
  }
}
