package edu.harvard.seas.pl.abcdatalog.ast;

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
