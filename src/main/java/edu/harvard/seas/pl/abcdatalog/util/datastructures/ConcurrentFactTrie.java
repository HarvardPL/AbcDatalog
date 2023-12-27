package edu.harvard.seas.pl.abcdatalog.util.datastructures;

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
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.ast.Variable;
import edu.harvard.seas.pl.abcdatalog.util.Utilities;
import edu.harvard.seas.pl.abcdatalog.util.substitution.ConstOnlySubstitution;
import java.util.concurrent.ConcurrentMap;

/** A trie that holds a set of facts (i.e., ground atoms). */
public class ConcurrentFactTrie {
  private ConcurrentMap<PredicateSym, Object> trie = Utilities.createConcurrentMap();

  /**
   * Adds an atom a to this trie. The atom must be ground once the substitution s has been applied.
   * This method returns whether the trie has changed.
   *
   * @param a the atom
   * @param s the substitution
   * @return whether the set has changed
   */
  @SuppressWarnings("unchecked")
  public boolean add(PositiveAtom a, ConstOnlySubstitution s) {
    if (a.getPred().getArity() == 0) {
      return trie.get(a.getPred()) == null && trie.putIfAbsent(a.getPred(), Boolean.TRUE) == null;
    }

    ConcurrentMap<Constant, Object> n = (ConcurrentMap<Constant, Object>) trie.get(a.getPred());
    if (n == null) {
      n = Utilities.createConcurrentMap();
      ConcurrentMap<Constant, Object> existing =
          (ConcurrentMap<Constant, Object>) trie.putIfAbsent(a.getPred(), n);
      if (existing != null) {
        n = existing;
      }
    }
    Term[] args = a.getArgs();
    for (int i = 0; i < args.length - 1; ++i) {
      Term t = args[i];
      if (t instanceof Variable) {
        t = s.get((Variable) t);
      }
      assert t != null;
      ConcurrentMap<Constant, Object> m = (ConcurrentMap<Constant, Object>) n.get(t);
      if (m == null) {
        m = Utilities.createConcurrentMap();
        ConcurrentMap<Constant, Object> existing =
            (ConcurrentMap<Constant, Object>) n.putIfAbsent((Constant) t, m);
        if (existing != null) {
          m = existing;
        }
      }
      n = m;
    }
    Term last = args[args.length - 1];
    if (last instanceof Variable) {
      last = s.get((Variable) last);
    }
    assert last != null;
    return n.get(last) == null && n.putIfAbsent((Constant) last, Boolean.TRUE) == null;
  }

  /**
   * Adds a fact to this trie and returns whether the trie has changed.
   *
   * @param fact the fact
   * @return whether the trie has changed
   */
  public boolean add(PositiveAtom fact) {
    try {
      return add(fact, null);
    } catch (NullPointerException e) {
      if (!fact.isGround()) {
        throw new IllegalArgumentException("Argument atom must be ground.");
      }
      throw new AssertionError();
    }
  }

  /** Clears this trie. */
  public void clear() {
    this.trie.clear();
  }
}
