package abcdatalog.ast.visitors;

import java.util.function.BiFunction;

import abcdatalog.ast.Head;
import abcdatalog.ast.PositiveAtom;

public class HeadVisitorBuilder<I, O> {
	private BiFunction<PositiveAtom, I, O> onPositiveAtom;
	
	public HeadVisitorBuilder<I, O> onPositiveAtom(BiFunction<PositiveAtom, I, O> onPositiveAtom) {
		this.onPositiveAtom = onPositiveAtom;
		return this;
	}
	
	public HeadVisitor<I, O> or(BiFunction<Head, I, O> f) {
		return new Visitor(f);
	}
	
	public HeadVisitor<I, O> orNull() {
		return this.or((head, state) -> null);
	}
	
	public HeadVisitor<I, O> orCrash() {
		return this.or((head, state) -> { throw new UnsupportedOperationException(); });
	}
	
	private class Visitor implements HeadVisitor<I, O> {
		private final BiFunction<PositiveAtom, I, O> onPositiveAtom;
		private final BiFunction<Head, I, O> otherwise;
		
		public Visitor(BiFunction<Head, I, O> otherwise) {
			this.onPositiveAtom = HeadVisitorBuilder.this.onPositiveAtom;
			this.otherwise = otherwise;
		}

		@Override
		public O visit(PositiveAtom atom, I state) {
			if (this.onPositiveAtom != null) {
				return this.onPositiveAtom.apply(atom, state);
			}
			return this.otherwise.apply(atom, state);
		}
		
	}
}
