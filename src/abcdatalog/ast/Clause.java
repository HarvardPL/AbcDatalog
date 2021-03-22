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
package abcdatalog.ast;

import java.util.List;

/**
 * A clause consisting of a head and a body, the latter of which is a list of
 * premises. The standard interpretation is that the head of a clause is
 * considered to hold if each premise in the body holds.
 *
 */
public class Clause {

	private final Head head;
	protected final List<Premise> body;

	public Clause(Head head, List<Premise> body) {
		this.head = head;
		this.body = body;
	}

	/**
	 * Returns the head of this clause.
	 * 
	 * @return the head
	 */
	public Head getHead() {
		return this.head;
	}

	/**
	 * Returns the body of this clause.
	 * 
	 * @return the body
	 */
	public List<Premise> getBody() {
		return this.body;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result + ((head == null) ? 0 : head.hashCode());
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
		Clause other = (Clause) obj;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		if (head == null) {
			if (other.head != null)
				return false;
		} else if (!head.equals(other.head))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getHead());
		if (!this.getBody().isEmpty()) {
			sb.append(" :- ");
			for (int i = 0; i < this.getBody().size(); ++i) {
				sb.append(this.getBody().get(i));
				if (i < this.getBody().size() - 1) {
					sb.append(", ");
				}
			}
		}
		sb.append('.');
		return sb.toString();
	}

}
