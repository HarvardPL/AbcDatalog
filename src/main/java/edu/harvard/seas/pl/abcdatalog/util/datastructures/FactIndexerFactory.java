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
