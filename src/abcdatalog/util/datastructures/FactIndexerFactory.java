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

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import abcdatalog.ast.PositiveAtom;
import abcdatalog.util.Utilities;

/**
 * A factory for creating some useful fact indexers.
 *
 */
public final class FactIndexerFactory {

	private FactIndexerFactory() {

	}

	/**
	 * Creates a fact indexer that uses concurrent sets for the base container.
	 * 
	 * @return the fact indexer
	 */
	public static ConcurrentFactIndexer<Set<PositiveAtom>> createConcurrentSetFactIndexer() {
		return new ConcurrentFactIndexer<>(() -> Utilities.createConcurrentSet(), (set,
				fact) -> set.add(fact));
	}

	/**
	 * Creates a fact indexer that uses concurrent queues for the base
	 * container.
	 * 
	 * @return the fact indexer
	 */
	public static ConcurrentFactIndexer<Queue<PositiveAtom>> createConcurrentQueueFactIndexer() {
		return new ConcurrentFactIndexer<>(() -> new ConcurrentLinkedQueue<>(), (queue,
				fact) -> queue.add(fact));
	}

}
