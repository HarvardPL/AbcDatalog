package abcdatalog.engine.bottomup;

import abcdatalog.ast.Clause;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.engine.DatalogEngineWithProvenance;

public class BottomUpEngineFrameWithProvenance extends BottomUpEngineFrame<EvalManagerWithProvenance> implements DatalogEngineWithProvenance {

	public BottomUpEngineFrameWithProvenance(EvalManagerWithProvenance manager) {
		super(manager);
	}

	@Override
	public Clause getJustification(PositiveAtom fact) {
		return manager.getJustification(fact);
	}

}
