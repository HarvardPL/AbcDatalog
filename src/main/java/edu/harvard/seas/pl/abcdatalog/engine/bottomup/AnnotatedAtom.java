package edu.harvard.seas.pl.abcdatalog.engine.bottomup;

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

import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.Premise;
import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.ast.visitors.PremiseVisitor;
import edu.harvard.seas.pl.abcdatalog.util.substitution.Substitution;

/**
 * An annotated atom. In certain evaluation algorithms (such as semi-naive
 * evaluation) it is helpful to have an annotation associated with an atom.
 *
 */
public class AnnotatedAtom implements Premise {
	private final PositiveAtom atom;
	private final Annotation anno;

	public AnnotatedAtom(PositiveAtom atom, Annotation anno) {
		this.atom = atom;
		this.anno = anno;
	}

	public enum Annotation {
		IDB, IDB_PREV, EDB, DELTA;
	}

	public Term[] getArgs() {
		return atom.getArgs();
	}
	
	public PredicateSym getPred() {
		return atom.getPred();
	}
	
	public PositiveAtom asUnannotatedAtom() {
		return atom;
	}

	public Annotation getAnnotation() {
		return anno;
	}
	
	@Override
	public <I, O> O accept(PremiseVisitor<I, O> visitor, I state) {
		return visitor.visit(this, state);
	}

	@Override
	public String toString() {
		String a = "";
		switch (anno) {
		case IDB:
			a = "IDB";
			break;
		case IDB_PREV:
			a = "IDB_PREV";
			break;
		case EDB:
			a = "EDB";
			break;
		case DELTA:
			a = "DELTA";
			break;
		default:
			assert false;
		}
		return atom + "<" + a + ">";
	}

	@Override
	public Premise applySubst(Substitution subst) {
		return new AnnotatedAtom(atom.applySubst(subst), anno);
	}

}
