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
package abcdatalog.ast.visitors;

import java.util.function.BiFunction;

import abcdatalog.ast.BinaryDisunifier;
import abcdatalog.ast.BinaryUnifier;
import abcdatalog.ast.NegatedAtom;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.Premise;
import abcdatalog.engine.bottomup.AnnotatedAtom;

public class PremiseVisitorBuilder<I, O> {
	private BiFunction<PositiveAtom, I, O> onPositiveAtom;
	private BiFunction<NegatedAtom, I, O> onNegatedAtom;
	private BiFunction<BinaryUnifier, I, O> onBinaryUnifier;
	private BiFunction<BinaryDisunifier, I, O> onBinaryDisunifier;
	private BiFunction<AnnotatedAtom, I, O> onAnnotatedAtom;
	
	public PremiseVisitorBuilder<I, O> onPositiveAtom(BiFunction<PositiveAtom, I, O> onPositiveAtom) {
		this.onPositiveAtom = onPositiveAtom;
		return this;
	}
	
	public PremiseVisitorBuilder<I, O> onNegatedAtom(BiFunction<NegatedAtom, I, O> onNegatedAtom) {
		this.onNegatedAtom = onNegatedAtom;
		return this;
	}
	
	public PremiseVisitorBuilder<I, O> onBinaryUnifier(BiFunction<BinaryUnifier, I, O> onBinaryUnifier) {
		this.onBinaryUnifier = onBinaryUnifier;
		return this;
	}
	
	public PremiseVisitorBuilder<I, O> onBinaryDisunifier(BiFunction<BinaryDisunifier, I, O> onBinaryDisunifier) {
		this.onBinaryDisunifier = onBinaryDisunifier;
		return this;
	}
	
	public PremiseVisitorBuilder<I, O> onAnnotatedAtom(BiFunction<AnnotatedAtom, I, O> onAnnotatedAtom) {
		this.onAnnotatedAtom = onAnnotatedAtom;
		return this;
	}
	
	public PremiseVisitor<I, O> or(BiFunction<Premise, I, O> f) {
		return new Visitor(f);
	}
	
	public PremiseVisitor<I, O> orCrash() {
		return this.or((conj, state) -> { throw new UnsupportedOperationException(); });
	}
	
	public PremiseVisitor<I, O> orNull() {
		return this.or((conj, state) -> null);
	}
	
	private class Visitor implements PremiseVisitor<I,O> {
		private final BiFunction<PositiveAtom, I, O> onPositiveAtom;
		private final BiFunction<NegatedAtom, I, O> onNegatedAtom;
		private final BiFunction<BinaryUnifier, I, O> onBinaryUnifier;
		private final BiFunction<BinaryDisunifier, I, O> onBinaryDisunifier;
		private BiFunction<AnnotatedAtom, I, O> onAnnotatedAtom;
		private final BiFunction<Premise, I, O> otherwise;
		
		public Visitor(BiFunction<Premise, I, O> otherwise) {
			this.onPositiveAtom = PremiseVisitorBuilder.this.onPositiveAtom;
			this.onNegatedAtom = PremiseVisitorBuilder.this.onNegatedAtom;
			this.onBinaryUnifier = PremiseVisitorBuilder.this.onBinaryUnifier;
			this.onBinaryDisunifier = PremiseVisitorBuilder.this.onBinaryDisunifier;
			this.onAnnotatedAtom = PremiseVisitorBuilder.this.onAnnotatedAtom;
			this.otherwise = otherwise;
		}

		@Override
		public O visit(PositiveAtom atom, I state) {
			if (this.onPositiveAtom != null) {
				return this.onPositiveAtom.apply(atom, state);
			}
			return this.otherwise.apply(atom, state);
		}

		@Override
		public O visit(BinaryUnifier u, I state) {
			if (this.onBinaryUnifier != null) {
				return this.onBinaryUnifier.apply(u, state);
			}
			return this.otherwise.apply(u, state);
		}

		@Override
		public O visit(BinaryDisunifier u, I state) {
			if (this.onBinaryDisunifier != null) {
				return this.onBinaryDisunifier.apply(u, state);
			}
			return this.otherwise.apply(u, state);
		}

		@Override
		public O visit(NegatedAtom atom, I state) {
			if (this.onNegatedAtom != null) {
				return this.onNegatedAtom.apply(atom, state);
			}
			return this.otherwise.apply(atom, state);
		}

		@Override
		public O visit(AnnotatedAtom atom, I state) {
			if (this.onAnnotatedAtom != null) {
				return this.onAnnotatedAtom.apply(atom, state);
			}
			return this.otherwise.apply(atom, state);
		}
	}
}
