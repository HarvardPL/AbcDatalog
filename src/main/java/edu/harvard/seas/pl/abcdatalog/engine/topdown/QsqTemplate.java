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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A template containing the attribute schemata of the supplementary relations for a given rule in
 * QSQ evaluation.
 */
public class QsqTemplate {
  /** The attribute schemata for the supplementary relations. */
  private final List<TermSchema> schemata;

  /**
   * Constructs a template from an adorned rule.
   *
   * @param rule the adorned rule
   */
  public QsqTemplate(AdornedClause rule) {
    // Find the last occurrence of each variable.
    Map<Term, Integer> lastPos = new HashMap<>();
    for (int i = 0; i < rule.getBody().size(); ++i) {
      for (Term t : rule.getBody().get(i).getArgs()) {
        if (t instanceof Variable) {
          lastPos.put(t, i);
        }
      }
    }

    // Determine the schemata for the first and last supplementary
    // relations.
    List<TermSchema> schemata = new ArrayList<>();
    Set<Term> bound = new HashSet<>();
    List<Term> firstSchema = new ArrayList<>();
    List<Term> lastSchema = new ArrayList<>();
    for (int i = 0; i < rule.getHead().getArgs().length; ++i) {
      Term t = rule.getHead().getArgs()[i];
      if (t instanceof Variable) {
        // All variables in the head of the rule are in the schema for
        // the final supplementary relation.
        if (lastSchema.indexOf(t) == -1) {
          lastSchema.add(t);
          lastPos.put(t, rule.getBody().size() - 1);
        }

        // Only bound variables in the head of the rule are in the
        // schema for the first supplementary relation.
        if (rule.getHead().getPred().getAdornment().get(i)) {
          firstSchema.add(t);
          bound.add(t);
        }
      }
    }

    // Determine the intermediary schema.
    schemata.add(new TermSchema(firstSchema));
    for (int i = 0; i < rule.getBody().size() - 1; ++i) {
      List<Term> schema = new ArrayList<>();
      for (Term t : rule.getBody().get(i).getArgs()) {
        if (t instanceof Variable) {
          bound.add(t);
        }
      }

      for (Iterator<Term> iter = bound.iterator(); iter.hasNext(); ) {
        Term v = iter.next();
        if (lastPos.get(v) == i) {
          iter.remove();
        } else {
          schema.add(v);
        }
      }
      schemata.add(new TermSchema(schema));
    }
    schemata.add(new TermSchema(lastSchema));

    this.schemata = new ArrayList<>(schemata);
  }

  /**
   * Returns the schema for the ith (0-indexed) supplementary relation.
   *
   * @param i the index
   * @return the schema
   */
  public TermSchema get(int i) {
    return this.schemata.get(i);
  }

  /**
   * Returns the number of schemata in this template.
   *
   * @return the number of schemata
   */
  public int size() {
    return this.schemata.size();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Iterator<TermSchema> it = this.schemata.iterator(); it.hasNext(); ) {
      sb.append(it.next());
      if (it.hasNext()) {
        sb.append(", ");
      }
    }
    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((schemata == null) ? 0 : schemata.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    QsqTemplate other = (QsqTemplate) obj;
    if (schemata == null) {
      if (other.schemata != null) return false;
    } else if (!schemata.equals(other.schemata)) return false;
    return true;
  }
}
