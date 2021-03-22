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

import java.util.concurrent.ConcurrentMap;

import abcdatalog.ast.Constant;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.ast.Term;
import abcdatalog.ast.Variable;
import abcdatalog.util.Utilities;
import abcdatalog.util.substitution.ConstOnlySubstitution;

/**
 * A trie that holds a set of facts (i.e., ground atoms).
 *
 */
public class ConcurrentFactTrie {
	private ConcurrentMap<PredicateSym, Object> trie = Utilities.createConcurrentMap();

	/**
	 * Adds an atom a to this trie. The atom must be ground once the
	 * substitution s has been applied. This method returns whether the trie has
	 * changed.
	 * 
	 * @param a
	 *            the atom
	 * @param s
	 *            the substitution
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
			ConcurrentMap<Constant, Object> existing = (ConcurrentMap<Constant, Object>) trie.putIfAbsent(a.getPred(), n);
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
				ConcurrentMap<Constant, Object> existing = (ConcurrentMap<Constant, Object>) n.putIfAbsent((Constant) t, m);
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
	 * @param fact
	 *            the fact
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
	
	/**
	 * Clears this trie.
	 */
	public void clear() {
		this.trie.clear();
	}

}
