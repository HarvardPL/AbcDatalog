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
package abcdatalog.engine.topdown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import abcdatalog.ast.Term;

/**
 * A relation, i.e., a set of tuples of a fixed arity with an associated
 * attribute schema of the same arity.
 *
 */
public class Relation implements Iterable<Tuple> {
	/**
	 * The tuples of this relation.
	 */
	protected Set<Tuple> tuples;
	/**
	 * The attribute schema of this relation.
	 */
	protected TermSchema attributes;
	/**
	 * The fixed arity of this relation.
	 */
	public final int arity;

	/**
	 * Constructs a relation from another relation.
	 * 
	 * @param other
	 *            the other relation
	 */
	public Relation(Relation other) {
		this.tuples = new HashSet<>(other.tuples);
		this.attributes = new TermSchema(other.attributes);
		this.arity = other.arity;
	}

	/**
	 * Constructs an empty relation with the given attribute schema.
	 * 
	 * @param attributes
	 *            the attribute schema
	 */
	public Relation(TermSchema attributes) {
		this.tuples = new HashSet<>();
		this.attributes = attributes;
		this.arity = attributes.size();
	}

	/**
	 * Constructs an empty relation of the given arity. It has a default
	 * attribute schema.
	 * 
	 * @param arity
	 *            the arity
	 */
	public Relation(int arity) {
		this.tuples = new HashSet<>();
		this.arity = arity;
		this.attributes = new TermSchema(arity);
	}

	/**
	 * Add a tuple of the proper arity to this relation.
	 * 
	 * @param x
	 *            the tuple
	 * @return whether this relation has changed
	 */
	public boolean add(Tuple x) {
		if (x.size() != this.arity) {
			throw new IllegalArgumentException("Relation has arity "
					+ this.arity + " but tuple has size " + x.size() + ".");
		}
		return this.tuples.add(x);
	}

	/**
	 * Add all the tuples of another relation to this relation. The two
	 * relations must have the same arity.
	 * 
	 * @param other
	 *            the other relation
	 * @return whether this relation has changed
	 */
	public boolean addAll(Relation other) {
		if (other.arity != this.arity) {
			throw new IllegalArgumentException(
					"Cannot add a relation of arity " + other.arity
							+ " to a relation of arity " + this.arity + ".");
		}
		return this.tuples.addAll(other.tuples);
	}

	/**
	 * Remove all the tuples in other relation from this relation. The two
	 * relations must have the same arity.
	 * 
	 * @param other
	 *            the other relation
	 * @return whether this relation has changed
	 */
	public boolean removeAll(Relation other) {
		if (other.arity != this.arity) {
			System.out.println(this);
			System.out.println(other);
			throw new IllegalArgumentException(
					"Cannot remove a relation of arity " + other.arity
							+ " from a relation of arity " + this.arity + ".");
		}
		return this.tuples.removeAll(other.tuples);
	}

	/**
	 * Returns a new relation consisting of those tuples that meet the supplied
	 * predicate.
	 * 
	 * @param f
	 *            the predicate
	 * @return the new relation
	 */
	public Relation filter(Function<Tuple, Boolean> f) {
		Relation r = new Relation(this.attributes);
		for (Tuple t : this.tuples) {
			if (f.apply(t)) {
				r.tuples.add(t);
			}
		}
		return r;
	}

	/**
	 * Returns the number of tuples in this relation.
	 * 
	 * @return the number of tuples
	 */
	public int size() {
		return this.tuples.size();
	}

	/**
	 * Returns whether the relation has any tuples.
	 * 
	 * @return whether the relation is empty
	 */
	public boolean isEmpty() {
		return this.tuples.isEmpty();
	}

	/**
	 * Creates a new relation by joining this relation with the other relation
	 * and projecting onto the supplied attribute schema. If the schema has
	 * attributes not in either relation, those terms are null.
	 * 
	 * @param other
	 *            the other relation
	 * @param schema
	 *            the attribute schema
	 * @return the new relation
	 */
	public Relation joinAndProject(Relation other, TermSchema schema) {
		// The join is implemented via hash-join.

		// Find common terms in this relation and the other one.
		Set<Term> thisTerms = new HashSet<>();
		for (int i = 0; i < this.attributes.size(); ++i) {
			thisTerms.add(this.attributes.get(i));
		}
		Set<Term> otherTerms = new HashSet<>();
		for (int i = 0; i < other.attributes.size(); ++i) {
			otherTerms.add(other.attributes.get(i));
		}
		thisTerms.retainAll(otherTerms);

		// Identify the smaller of the two relations.
		Relation smaller;
		Relation larger;
		if (size() < other.size()) {
			smaller = this;
			larger = other;
		} else {
			smaller = other;
			larger = this;
		}

		// Create a map from term to an integer index into the schema used when
		// hashing tuples.
		Map<Term, Integer> indexMap = new HashMap<>();
		int idx = 0;
		for (Term term : thisTerms) {
			indexMap.put(term, idx++);
		}

		// Create a map from term to an integer index into the schema for output
		// tuples.
		Map<Term, Integer> outputMap = new HashMap<>();
		for (int i = 0; i < schema.size(); ++i) {
			outputMap.put(schema.get(i), i);
		}

		// Determine how the schema for the smaller relation relates to the
		// schemas used for hashing the tuples and for the output.
		Integer[] hashTupIdxForSmaller = new Integer[smaller.arity];
		Integer[] outputTupIdxForSmaller = new Integer[smaller.arity];
		for (int i = 0; i < smaller.attributes.size(); ++i) {
			Term attribute = smaller.attributes.get(i);
			hashTupIdxForSmaller[i] = indexMap.get(attribute);
			outputTupIdxForSmaller[i] = outputMap.get(attribute);
		}

		// Add tuples from smaller relation to the hash table.
		HashMap<Tuple, Set<Tuple>> table = new HashMap<>();
		int hashTupSize = thisTerms.size();
		for (Tuple t : smaller) {
			Tuple hashTup = toHashTuple(t, hashTupIdxForSmaller, hashTupSize);
			Set<Tuple> tuples = table.get(hashTup);
			if (tuples == null) {
				tuples = new HashSet<>();
				table.put(hashTup, tuples);
			}
			tuples.add(t);
		}

		// Determine how the schema for the larger relation relates to the
		// schemas used for hashing the tuples and for the output.
		Integer[] hashTupIdxForLarger = new Integer[larger.arity];
		Integer[] outputTupIdxForLarger = new Integer[larger.arity];
		for (int i = 0; i < larger.arity; ++i) {
			Term attribute = larger.attributes.get(i);
			hashTupIdxForLarger[i] = indexMap.get(attribute);
			outputTupIdxForLarger[i] = outputMap.get(attribute);
		}

		// Perform the actual join and project each tuple onto the output schema
		Relation result = new Relation(schema);
		for (Tuple t1 : larger) {
			Tuple hashTup = toHashTuple(t1, hashTupIdxForLarger, hashTupSize);
			Set<Tuple> tuples = table.get(hashTup);
			if (tuples != null) {
				// Create a template for the output tuple based on the terms in
				// t1.
				Term[] outputTempl = new Term[schema.size()];
				for (int i = 0; i < larger.arity; ++i) {
					Integer j = outputTupIdxForLarger[i];
					if (j != null) {
						outputTempl[j] = t1.get(i);
					}
				}

				// Create new tuples by adding terms from the tuples in the
				// smaller relation to the output template.
				for (Tuple t2 : tuples) {
					Term[] output = new Term[outputTempl.length];
					System.arraycopy(outputTempl, 0, output, 0, output.length);

					for (int i = 0; i < smaller.arity; ++i) {
						Integer j = outputTupIdxForSmaller[i];
						if (j != null) {
							output[j] = t2.get(i);
						}
					}
					result.add(new Tuple(Arrays.asList(output)));
				}
			}
		}

		return result;
	}

	/**
	 * Hashes a tuple given an index from the tuple into the desired output
	 * tuple and the size of the output tuple.
	 * 
	 * @param input
	 *            the input tuple
	 * @param hashTupIdx
	 *            the index from the input tuple into the output tuple
	 * @param hashTupSize
	 *            the size of the output tuple
	 * @return the hashed tuple
	 */
	private static Tuple toHashTuple(Tuple input, Integer[] hashTupIdx,
			int hashTupSize) {
		Term[] terms = new Term[hashTupSize];
		for (int i = 0; i < input.size(); ++i) {
			Integer j = hashTupIdx[i];
			if (j != null) {
				terms[j] = input.get(i);
			}
		}
		return new Tuple(Arrays.asList(terms));
	}

	/**
	 * For each tuple t in this relation, creates a substitution by mapping each
	 * attribute in the schema to the corresponding element in t, and then
	 * creates a new relation by applying each substitution to the input tuple
	 * x.
	 * 
	 * @param x
	 *            the input tuple
	 * @return the new relation
	 */
	public Relation applyTuplesAsSubstitutions(Tuple x) {
		Relation r = new Relation(x.size());
		for (Tuple t : this.tuples) {
			Map<Term, Term> subst = new HashMap<>();
			for (int j = 0; j < this.arity; ++j) {
				subst.put(this.attributes.get(j), t.get(j));
			}
			List<Term> newTerms = new ArrayList<>();
			for (Term term : x.elts) {
				Term s = subst.get(term);
				if (s != null) {
					term = s;
				}
				newTerms.add(term);
			}
			r.add(new Tuple(newTerms));
		}
		return r;
	}

	/**
	 * Creates a new relation that results from the projection of this relation.
	 * The projection is described by a list of booleans, where a true value
	 * denotes that the column should be retained.
	 * 
	 * @param colsToKeep
	 *            the description of the projection
	 * @return the new relation
	 */
	public Relation project(List<Boolean> colsToKeep) {
		assert colsToKeep.size() == this.arity;

		int nKeepers = 0;
		for (int i = 0; i < colsToKeep.size(); ++i) {
			if (colsToKeep.get(i)) {
				++nKeepers;
			}
		}

		Relation r = new Relation(nKeepers);
		for (Tuple t : this.tuples) {
			List<Term> newTerms = new ArrayList<>();
			for (int i = 0; i < this.arity; ++i) {
				if (colsToKeep.get(i)) {
					newTerms.add(t.get(i));
				}
			}
			r.add(new Tuple(newTerms));
		}
		return r;
	}

	/**
	 * Returns whether this relation contains the input tuple x.
	 * 
	 * @param x
	 *            the input tuple
	 * @return whether this relation contains x
	 */
	public boolean contains(Tuple x) {
		return this.tuples.contains(x);
	}

	/**
	 * Returns the attribute schema for this relation.
	 * 
	 * @return the attribute schema
	 */
	public TermSchema getAttributes() {
		return this.attributes;
	}

	/**
	 * Change the attribute schema of this relation to the supplied one, which
	 * must be of the same arity as this relation.
	 * 
	 * @param schema the new schema
	 */
	public void renameAttributes(TermSchema schema) {
		if (schema.size() != this.arity) {
			throw new IllegalArgumentException("Schema of size "
					+ schema.size()
					+ " cannot be given to a relation of arity " + this.arity
					+ ".");
		}
		this.attributes = schema;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Attributes: " + attributes);
		sb.append("; Tuples: ");
		for (Iterator<Tuple> it = this.tuples.iterator(); it.hasNext();) {
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
		result = prime * result + arity;
		result = prime * result
				+ ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((tuples == null) ? 0 : tuples.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Relation other = (Relation) obj;
		if (arity != other.arity)
			return false;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (tuples == null) {
			if (other.tuples != null)
				return false;
		} else if (!tuples.equals(other.tuples))
			return false;
		return true;
	}

	@Override
	public Iterator<Tuple> iterator() {
		return this.tuples.iterator();
	}

}
