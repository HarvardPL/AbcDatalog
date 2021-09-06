package edu.harvard.seas.pl.abcdatalog.util.datastructures;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.util.Utilities;

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
