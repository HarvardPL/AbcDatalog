package edu.harvard.seas.pl.abcdatalog.ast.visitors;

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
import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.ast.Variable;
import java.util.function.BiFunction;

public class TermVisitorBuilder<I, O> {
  private BiFunction<Variable, I, O> onVariable;
  private BiFunction<Constant, I, O> onConstant;

  public TermVisitorBuilder<I, O> onVariable(BiFunction<Variable, I, O> f) {
    this.onVariable = f;
    return this;
  }

  public TermVisitorBuilder<I, O> onConstant(BiFunction<Constant, I, O> f) {
    this.onConstant = f;
    return this;
  }

  public TermVisitor<I, O> or(BiFunction<Term, I, O> f) {
    return new Visitor(f);
  }

  public TermVisitor<I, O> orNull() {
    return this.or((t, state) -> null);
  }

  public TermVisitor<I, O> orCrash() {
    return this.or(
        (t, state) -> {
          throw new UnsupportedOperationException();
        });
  }

  private class Visitor implements TermVisitor<I, O> {
    private final BiFunction<Variable, I, O> onVariable;
    private final BiFunction<Constant, I, O> onConstant;
    private final BiFunction<Term, I, O> otherwise;

    public Visitor(BiFunction<Term, I, O> otherwise) {
      this.onVariable = TermVisitorBuilder.this.onVariable;
      this.onConstant = TermVisitorBuilder.this.onConstant;
      this.otherwise = otherwise;
    }

    @Override
    public O visit(Variable t, I state) {
      if (onVariable != null) {
        return onVariable.apply(t, state);
      }
      return otherwise.apply(t, state);
    }

    @Override
    public O visit(Constant t, I state) {
      if (onConstant != null) {
        return onConstant.apply(t, state);
      }
      return otherwise.apply(t, state);
    }
  }
}
