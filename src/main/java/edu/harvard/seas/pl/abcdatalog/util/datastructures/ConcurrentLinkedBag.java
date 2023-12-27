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

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A linked-list-backed bag that supports a minimal number of operations. Operations are
 * thread-safe, although iterators over the bag are <b>not</b> thread-safe.
 *
 * @param <T> the type of the element of the bag
 */
public class ConcurrentLinkedBag<T> implements Iterable<T> {
  private AtomicReference<Node> head = new AtomicReference<>();

  /** A node in the linked list. */
  public class Node {
    private final T val;
    private final Node next;

    private Node(T val, Node next) {
      this.val = val;
      this.next = next;
    }

    /**
     * Get the value of this node.
     *
     * @return the value
     */
    public T getVal() {
      return this.val;
    }

    /**
     * Returns the next node in the linked list, or null if this is the last node.
     *
     * @return the next node, or null
     */
    public Node getNext() {
      return this.next;
    }
  }

  /**
   * Add an element to the bag.
   *
   * @param e the element
   */
  public void add(T e) {
    Node h, n;
    do {
      h = this.head.get();
      n = new Node(e, h);
    } while (!this.head.compareAndSet(h, n));
  }

  /**
   * Returns the head of the linked-list that backs this bag, or null if there is no head.
   *
   * @return the head, or null
   */
  public Node getHead() {
    return head.get();
  }

  /**
   * Returns an iterator over elements of type T. The iterator is <b>not</b> thread-safe.
   *
   * @return an iterator
   */
  @Override
  public Iterator<T> iterator() {
    return new NodeIterator();
  }

  private class NodeIterator implements Iterator<T> {
    private Node cur = head.get();

    @Override
    public boolean hasNext() {
      return this.cur != null;
    }

    @Override
    public T next() {
      T v = this.cur.val;
      this.cur = this.cur.next;
      return v;
    }
  }

  // Static fields and methods for empty instance.

  @SuppressWarnings("rawtypes")
  private static final ConcurrentLinkedBag EMPTY_BAG = new EmptyBag();

  private static class EmptyBag<T> extends ConcurrentLinkedBag<T> {
    @Override
    public void add(T e) {
      throw new UnsupportedOperationException();
    }
  }

  @SuppressWarnings("unchecked")
  public static final <T> ConcurrentLinkedBag<T> emptyBag() {
    return EMPTY_BAG;
  }
}
