package edu.harvard.seas.pl.abcdatalog.engine.topdown;

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
