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

import edu.harvard.seas.pl.abcdatalog.ast.visitors.TermVisitor;
import edu.harvard.seas.pl.abcdatalog.util.substitution.Substitution;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** A Datalog variable. */
public class Variable implements Term {
  /** Identifier for this variable. */
  private final String name;

  /** Map for memoization. */
  private static final ConcurrentMap<String, Variable> memo = new ConcurrentHashMap<>();

  /**
   * Returns a variable with the given string identifier.
   *
   * @param name the string identifier
   * @return the variable
   */
  public static Variable create(String name) {
    Variable c = memo.get(name);
    if (c != null) {
      return c;
    }
    // try creating it
    c = new Variable(name);
    Variable existing = memo.putIfAbsent(name, c);
    if (existing != null) {
      return existing;
    }
    return c;
  }

  /**
   * Constructs a variable from an identifier.
   *
   * @param name identifier
   */
  protected Variable(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return this.getName();
  }

  @Override
  public <I, O> O accept(TermVisitor<I, O> visitor, I state) {
    return visitor.visit(this, state);
  }

  public static Variable createFreshVariable() {
    return new Variable("_");
  }

  @Override
  public Term applySubst(Substitution subst) {
    Term s = subst.get(this);
    return (s != null) ? s : this;
  }
}
