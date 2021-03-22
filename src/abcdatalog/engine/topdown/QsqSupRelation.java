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

/**
 * A supplementary relation used in QSQ evaluation. <br>
 * <br>
 * It is modeled as a node in a linked list. The intention is that each node
 * contains the tuples for the supplementary relation itself, a pointer to the
 * atom that appears after the supplementary relation in the relevant rule, and
 * a pointer to the next supplementary relation in the same rule.
 *
 */
public class QsqSupRelation extends Relation {
	/**
	 * Points to the next supplementary relation in the rule being evaluated.
	 */
	public QsqSupRelation next;
	/**
	 * Points to the atom that follows this supplementary relation in the rule
	 * being evaluated.
	 */
	public AdornedAtom nextAtom;

	/**
	 * Constructs an empty supplementary relation with the supplied schema.
	 * 
	 * @param schema
	 *            schema of supplementary relation
	 */
	public QsqSupRelation(TermSchema schema) {
		super(schema);
		nextAtom = null;
		next = null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("; Next atom: ");
		if (nextAtom != null) {
			sb.append(nextAtom);
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((next == null) ? 0 : next.hashCode());
		result = prime * result + ((nextAtom == null) ? 0 : nextAtom.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		QsqSupRelation other = (QsqSupRelation) obj;
		if (next == null) {
			if (other.next != null)
				return false;
		} else if (!next.equals(other.next))
			return false;
		if (nextAtom == null) {
			if (other.nextAtom != null)
				return false;
		} else if (!nextAtom.equals(other.nextAtom))
			return false;
		return true;
	}
	
	

}
