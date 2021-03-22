/*******************************************************************************
 * This file is part of the AbcDatalog project.
 *
 * Copyright (c) 2016, Harvard University
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the BSD License which accompanies this distribution.
 *
 * The development of the AbcDatalog project has been supported by the 
 * National Science Foundation under Grant Nos. 1237235 and 1054172.
 *
 * See README for contributors.
 ******************************************************************************/
package abcdatalog.util.datastructures;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A linked-list-backed bag that supports a minimal number of operations.
 * Operations are thread-safe, although iterators over the bag are <b>not</b>
 * thread-safe.
 *
 * @param <T>
 *            the type of the element of the bag
 */
public class ConcurrentLinkedBag<T> implements Iterable<T> {
	private AtomicReference<Node> head = new AtomicReference<>();
	
	/**
	 * A node in the linked list.
	 *
	 */
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
		 * Returns the next node in the linked list, or null if this is the
		 * last node.
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
	 * @param e
	 *            the element
	 */
	public void add(T e) {
		Node h, n;
		do {
			h = this.head.get();
			n = new Node(e, h);
		} while (!this.head.compareAndSet(h, n));
	}
	
	/**
	 * Returns the head of the linked-list that backs this bag, or null if
	 * there is no head.
	 * 
	 * @return the head, or null
	 */
	public Node getHead() {
		return head.get();
	}

	/**
	 * Returns an iterator over elements of type T. The iterator is <b>not</b>
	 * thread-safe.
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
