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
package abcdatalog.engine.bottomup;

import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.ast.Premise;
import abcdatalog.ast.Term;
import abcdatalog.ast.visitors.PremiseVisitor;

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

}
