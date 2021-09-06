package edu.harvard.seas.pl.abcdatalog.engine.bottomup;

import java.util.HashSet;
import java.util.Set;

import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidationException;
import edu.harvard.seas.pl.abcdatalog.engine.DatalogEngine;
import edu.harvard.seas.pl.abcdatalog.util.datastructures.IndexableFactCollection;

/**
 * A framework for a bottom-up Datalog engine.
 *
 */
public class BottomUpEngineFrame<E extends EvalManager> implements DatalogEngine {
	/**
	 * The evaluation manager for this engine.
	 */
	protected final E manager;
	/**
	 * The set of facts that can be derived from the current program.
	 */
	private volatile IndexableFactCollection facts;
	/**
	 * Has the engine been initialized?
	 */
	private volatile boolean isInitialized = false;

	/**
	 * Constructs a bottom-up engine with the provided evaluation manager.
	 * 
	 * @param manager
	 *            the manager
	 */
	public BottomUpEngineFrame(E manager) {
		this.manager = manager;
	}

	@Override
	public synchronized void init(Set<Clause> program) throws DatalogValidationException {
		if (this.isInitialized) {
			throw new IllegalStateException("Cannot initialize an engine more than once.");
		}
		
		this.manager.initialize(program);
		this.facts = this.manager.eval();
		this.isInitialized = true;
	}

	@Override
	public Set<PositiveAtom> query(PositiveAtom q) {
		if (!this.isInitialized) {
			throw new IllegalStateException("Engine must be initialized before it can be queried.");
		}
		
		Set<PositiveAtom> r = new HashSet<>();
		for (PositiveAtom a : this.facts.indexInto(q)) {
			if (q.unify(a) != null) {
				r.add(a);
			}
		}
		return r;
	}

}
