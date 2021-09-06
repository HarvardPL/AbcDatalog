package edu.harvard.seas.pl.abcdatalog.engine.bottomup;

import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.engine.DatalogEngineWithProvenance;

public class BottomUpEngineFrameWithProvenance extends BottomUpEngineFrame<EvalManagerWithProvenance> implements DatalogEngineWithProvenance {

	public BottomUpEngineFrameWithProvenance(EvalManagerWithProvenance manager) {
		super(manager);
	}

	@Override
	public Clause getJustification(PositiveAtom fact) {
		return manager.getJustification(fact);
	}

}
