package edu.harvard.seas.pl.abcdatalog.ast.visitors;

import java.util.function.BiFunction;

import edu.harvard.seas.pl.abcdatalog.ast.Constant;
import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.ast.Variable;

public class TermVisitorBuilder<I, O> {
	private BiFunction<Variable, I, O> onVariable;
	private BiFunction<Constant, I, O> onConstant;
	
	public TermVisitorBuilder<I, O> onVariable(BiFunction<Variable, I, O> f) {
		this.onVariable = f;
		return this;
	}
	
	public TermVisitorBuilder<I, O> onConstant(BiFunction<Constant, I, O> f) {
		this.onConstant = f;
		return this;
	}
	
	public TermVisitor<I, O> or(BiFunction<Term, I, O> f) {
		return new Visitor(f);
	}
	
	public TermVisitor<I, O> orNull() {
		return this.or((t, state) -> null);
	}
	
	public TermVisitor<I, O> orCrash() {
		return this.or((t, state) -> { throw new UnsupportedOperationException(); });
	}
	
	private class Visitor implements TermVisitor<I, O> {
		private final BiFunction<Variable, I, O> onVariable;
		private final BiFunction<Constant, I, O> onConstant;
		private final BiFunction<Term, I, O> otherwise;

		public Visitor(BiFunction<Term, I, O> otherwise) {
			this.onVariable = TermVisitorBuilder.this.onVariable;
			this.onConstant = TermVisitorBuilder.this.onConstant;
			this.otherwise = otherwise;
		}

		@Override
		public O visit(Variable t, I state) {
			if (onVariable != null) {
				return onVariable.apply(t, state);
			}
			return otherwise.apply(t, state);
		}

		@Override
		public O visit(Constant t, I state) {
			if (onConstant != null) {
				return onConstant.apply(t, state);
			}
			return otherwise.apply(t, state);
		}
	}
}
