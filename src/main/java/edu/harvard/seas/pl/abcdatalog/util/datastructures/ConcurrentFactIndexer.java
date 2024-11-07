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
import edu.harvard.seas.pl.abcdatalog.ast.visitors.TermVisitor;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.TermVisitorBuilder;
import edu.harvard.seas.pl.abcdatalog.util.Utilities;
import edu.harvard.seas.pl.abcdatalog.util.substitution.ConstOnlySubstitution;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An index that holds facts. The facts are indexed by predicate symbol and then by the constants in
 * each argument position. The indexer is parameterized by the type of the container that ultimately
 * holds the facts (i.e., all facts that belong to the same index are added to the same container).
 *
 * <p>In the presence of multiple threads, the indexer can momentarily be in an inconsistent state.
 * However, after the add method returns having been invoked with a fact f, the indexer will be
 * consistent with respect to f, meaning that f is properly indexed and that it is visible as such
 * to all threads. (This only holds, of course, if the provided container type is thread safe.)
 *
 * @param <T> the container type
 */
public class ConcurrentFactIndexer<T extends Iterable<PositiveAtom>> implements FactIndexer {
  private final Supplier<T> generator;
  private final BiConsumer<T, PositiveAtom> addFunc;
  private final Supplier<T> empty;
  private final Function<T, Integer> size;

  private final ConcurrentMap<PredicateSym, AtomicReferenceArray<ConcurrentMap<Constant, T>>>
      fineIdx = Utilities.createConcurrentMap();
  private final ConcurrentMap<PredicateSym, T> coarseIdx = Utilities.createConcurrentMap();

  /**
   * Creates a new fact indexer.
   *
   * @param generator an anonymous function that returns a container
   * @param addFunc an anonymous function that adds a fact to a container
   * @param size an anonymous function that gets the number of items in the container
   */
  public ConcurrentFactIndexer(Supplier<T> generator, BiConsumer<T, PositiveAtom> addFunc, Function<T, Integer> size) {
    this(generator, addFunc, generator, size);
  }

  /**
   * Creates a new fact indexer.
   *
   * @param generator an anonymous function that returns a container
   * @param addFunc an anonymous function that adds a fact to a container
   * @param empty an anonymous function that returns an empty container (such as a static instance)
   * @param size an anonymous function that gets the number of items in the container
   */
  public ConcurrentFactIndexer(
      Supplier<T> generator, BiConsumer<T, PositiveAtom> addFunc, Supplier<T> empty, Function<T, Integer> size) {
    this.generator = generator;
    this.addFunc = addFunc;
    this.empty = empty;
    this.size = size;
  }

  /**
   * Adds a fact to this indexer.
   *
   * @param fact the fact
   */
  public void add(PositiveAtom fact) {
    assert fact.isGround();
    T rough = this.coarseIdx.get(fact.getPred());
    if (rough == null) {
      rough = this.generator.get();
      T existing = this.coarseIdx.putIfAbsent(fact.getPred(), rough);
      if (existing != null) {
        rough = existing;
      }
    }
    this.addFunc.accept(rough, fact);

    AtomicReferenceArray<ConcurrentMap<Constant, T>> byPos = this.fineIdx.get(fact.getPred());
    if (byPos == null) {
      byPos = new AtomicReferenceArray<>(fact.getPred().getArity());
      AtomicReferenceArray<ConcurrentMap<Constant, T>> existing =
          this.fineIdx.putIfAbsent(fact.getPred(), byPos);
      if (existing != null) {
        byPos = existing;
      }
    }
    assert byPos != null;

    Term[] args = fact.getArgs();
    for (int i = 0; i < args.length; ++i) {
      ConcurrentMap<Constant, T> byConstant = byPos.get(i);
      if (byConstant == null) {
        byConstant = Utilities.createConcurrentMap();
        if (!byPos.compareAndSet(i, null, byConstant)) {
          byConstant = byPos.get(i);
        }
      }
      Constant key = (Constant) args[i];
      T n = byConstant.get(key);
      if (n == null) {
        n = this.generator.get();
        T existing = byConstant.putIfAbsent(key, n);
        if (existing != null) {
          n = existing;
        }
      }
      this.addFunc.accept(n, fact);
    }
  }

  /**
   * Adds the facts to the index.
   *
   * @param facts the facts
   */
  public void addAll(Iterable<PositiveAtom> facts) {
    for (PositiveAtom a : facts) {
      this.add(a);
    }
  }

  @Override
  public T indexInto(PositiveAtom a) {
    return this.indexInto(a, null);
  }

  private static final TermVisitor<ConstOnlySubstitution, Constant> tv =
      (new TermVisitorBuilder<ConstOnlySubstitution, Constant>())
          .onConstant((c, s) -> c)
          .onVariable(
              (x, s) -> {
                if (s != null) {
                  return s.get(x);
                }
                return null;
              })
          .orCrash();

  @Override
  public T indexInto(PositiveAtom a, ConstOnlySubstitution s) {
    AtomicReferenceArray<ConcurrentMap<Constant, T>> byPos = this.fineIdx.get(a.getPred());
    if (byPos == null) {
      return this.empty.get();
    }

    int bestIdx = -1;
    Term bestConst = null;
    int minFactSetSize = Integer.MAX_VALUE;
    Term[] args = a.getArgs();
    for (int i = 0; i < args.length; ++i) {
      Term t = args[i];
      if ((t = t.accept(tv, s)) != null) {
        ConcurrentMap<Constant, T> byConstant = byPos.get(i);
        if (byConstant != null) {
          T collection = byConstant.get(t);
          if (collection == null) {
            return this.empty.get();
          }
          int factSetSize = size.apply(collection);
          if (factSetSize < minFactSetSize) {
            minFactSetSize = factSetSize;
            bestIdx = i;
            bestConst = t;
          }
        }
      }
    }

    if (bestIdx == -1) {
      return this.coarseIdx.get(a.getPred());
    }

    return byPos.get(bestIdx).get(bestConst);
  }

  @Override
  public T indexInto(PredicateSym pred) {
    T t = this.coarseIdx.get(pred);
    if (t == null) {
      t = this.empty.get();
    }
    return t;
  }

  /** Clears this index. */
  public void clear() {
    this.fineIdx.clear();
    this.coarseIdx.clear();
  }

  @Override
  public boolean isEmpty() {
    return this.coarseIdx.isEmpty();
  }

  public ConcurrentFactIndexer<T> getCopy() {
    // Lazy man's copy function... probably be faster if we actually
    // recursed through data structure copying whole indices. On the other
    // hand, that might end up creating a new fact indexer with an
    // inconsistent state.
    ConcurrentFactIndexer<T> r =
        new ConcurrentFactIndexer<>(this.generator, this.addFunc, this.empty, this.size);
    for (PredicateSym pred : this.coarseIdx.keySet()) {
      r.addAll(this.indexInto(pred));
    }
    return r;
  }

  @Override
  public Set<PredicateSym> getPreds() {
    return this.coarseIdx.keySet();
  }

  /**
   * Add all the facts from an indexable fact collection to this index.
   *
   * @param that the indexable fact collection
   */
  public void addAll(IndexableFactCollection that) {
    for (PredicateSym pred : that.getPreds()) {
      this.addAll(that.indexInto(pred));
    }
  }
}
