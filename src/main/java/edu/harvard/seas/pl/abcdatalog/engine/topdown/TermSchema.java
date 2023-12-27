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
import edu.harvard.seas.pl.abcdatalog.ast.Variable;
import java.util.ArrayList;
import java.util.List;

/**
 * A list of terms of fixed arity representing the attribute schema for a relation of the same arity
 * (i.e., the 2nd term in the list is the attribute for the 2nd "column" in the relation).
 */
public class TermSchema {
  /** The attributes of this schema. */
  public final List<Term> attributes;

  /**
   * Constructs a schema from a list of terms.
   *
   * @param terms the terms
   */
  public TermSchema(List<Term> terms) {
    this.attributes = terms;
  }

  /**
   * Constructs a schema from another schema.
   *
   * @param other the other schema
   */
  public TermSchema(TermSchema other) {
    this.attributes = new ArrayList<>(other.attributes);
  }

  /**
   * Constructs a schema of the supplied arity. The attributes are given unique but arbitrary names.
   *
   * @param arity the arity
   */
  public TermSchema(int arity) {
    attributes = new ArrayList<>();
    for (int i = 0; i < arity; ++i) {
      Term t = Variable.create("ANON" + i);
      this.attributes.add(t);
    }
  }

  /**
   * Returns the term at the given index into the schema.
   *
   * @param i the index
   * @return the term
   */
  public Term get(int i) {
    return this.attributes.get(i);
  }

  /**
   * Returns the index of the given term in the schema. If the term appears multiple times in the
   * schema, returns the first position.
   *
   * @param t the term
   * @return the index
   */
  public int get(Term t) {
    return this.attributes.indexOf(t);
  }

  /**
   * Returns the size of this schema.
   *
   * @return the size
   */
  public int size() {
    return this.attributes.size();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    int sz = size();
    for (int i = 0; i < sz; ++i) {
      sb.append(get(i));
      if (i < sz - 1) {
        sb.append(", ");
      }
    }
    sb.append(")");
    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    TermSchema other = (TermSchema) obj;
    if (attributes == null) {
      if (other.attributes != null) return false;
    } else if (!attributes.equals(other.attributes)) return false;
    return true;
  }
}
