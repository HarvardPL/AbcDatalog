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

import edu.harvard.seas.pl.abcdatalog.ast.BinaryDisunifier;
import edu.harvard.seas.pl.abcdatalog.ast.BinaryUnifier;
import edu.harvard.seas.pl.abcdatalog.ast.NegatedAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.Premise;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.AnnotatedAtom;
import java.util.function.BiFunction;

public class PremiseVisitorBuilder<I, O> {
  private BiFunction<PositiveAtom, I, O> onPositiveAtom;
  private BiFunction<NegatedAtom, I, O> onNegatedAtom;
  private BiFunction<BinaryUnifier, I, O> onBinaryUnifier;
  private BiFunction<BinaryDisunifier, I, O> onBinaryDisunifier;
  private BiFunction<AnnotatedAtom, I, O> onAnnotatedAtom;

  public PremiseVisitorBuilder<I, O> onPositiveAtom(BiFunction<PositiveAtom, I, O> onPositiveAtom) {
    this.onPositiveAtom = onPositiveAtom;
    return this;
  }

  public PremiseVisitorBuilder<I, O> onNegatedAtom(BiFunction<NegatedAtom, I, O> onNegatedAtom) {
    this.onNegatedAtom = onNegatedAtom;
    return this;
  }

  public PremiseVisitorBuilder<I, O> onBinaryUnifier(
      BiFunction<BinaryUnifier, I, O> onBinaryUnifier) {
    this.onBinaryUnifier = onBinaryUnifier;
    return this;
  }

  public PremiseVisitorBuilder<I, O> onBinaryDisunifier(
      BiFunction<BinaryDisunifier, I, O> onBinaryDisunifier) {
    this.onBinaryDisunifier = onBinaryDisunifier;
    return this;
  }

  public PremiseVisitorBuilder<I, O> onAnnotatedAtom(
      BiFunction<AnnotatedAtom, I, O> onAnnotatedAtom) {
    this.onAnnotatedAtom = onAnnotatedAtom;
    return this;
  }

  public PremiseVisitor<I, O> or(BiFunction<Premise, I, O> f) {
    return new Visitor(f);
  }

  public PremiseVisitor<I, O> orCrash() {
    return this.or(
        (conj, state) -> {
          throw new UnsupportedOperationException();
        });
  }

  public PremiseVisitor<I, O> orNull() {
    return this.or((conj, state) -> null);
  }

  private class Visitor implements PremiseVisitor<I, O> {
    private final BiFunction<PositiveAtom, I, O> onPositiveAtom;
    private final BiFunction<NegatedAtom, I, O> onNegatedAtom;
    private final BiFunction<BinaryUnifier, I, O> onBinaryUnifier;
    private final BiFunction<BinaryDisunifier, I, O> onBinaryDisunifier;
    private BiFunction<AnnotatedAtom, I, O> onAnnotatedAtom;
    private final BiFunction<Premise, I, O> otherwise;

    public Visitor(BiFunction<Premise, I, O> otherwise) {
      this.onPositiveAtom = PremiseVisitorBuilder.this.onPositiveAtom;
      this.onNegatedAtom = PremiseVisitorBuilder.this.onNegatedAtom;
      this.onBinaryUnifier = PremiseVisitorBuilder.this.onBinaryUnifier;
      this.onBinaryDisunifier = PremiseVisitorBuilder.this.onBinaryDisunifier;
      this.onAnnotatedAtom = PremiseVisitorBuilder.this.onAnnotatedAtom;
      this.otherwise = otherwise;
    }

    @Override
    public O visit(PositiveAtom atom, I state) {
      if (this.onPositiveAtom != null) {
        return this.onPositiveAtom.apply(atom, state);
      }
      return this.otherwise.apply(atom, state);
    }

    @Override
    public O visit(BinaryUnifier u, I state) {
      if (this.onBinaryUnifier != null) {
        return this.onBinaryUnifier.apply(u, state);
      }
      return this.otherwise.apply(u, state);
    }

    @Override
    public O visit(BinaryDisunifier u, I state) {
      if (this.onBinaryDisunifier != null) {
        return this.onBinaryDisunifier.apply(u, state);
      }
      return this.otherwise.apply(u, state);
    }

    @Override
    public O visit(NegatedAtom atom, I state) {
      if (this.onNegatedAtom != null) {
        return this.onNegatedAtom.apply(atom, state);
      }
      return this.otherwise.apply(atom, state);
    }

    @Override
    public O visit(AnnotatedAtom atom, I state) {
      if (this.onAnnotatedAtom != null) {
        return this.onAnnotatedAtom.apply(atom, state);
      }
      return this.otherwise.apply(atom, state);
    }
  }
}
