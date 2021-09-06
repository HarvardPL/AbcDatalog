package edu.harvard.seas.pl.abcdatalog.engine;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.harvard.seas.pl.abcdatalog.ast.Constant;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.Term;
import edu.harvard.seas.pl.abcdatalog.ast.Variable;
import edu.harvard.seas.pl.abcdatalog.util.substitution.ConstOnlySubstitution;
import edu.harvard.seas.pl.abcdatalog.util.substitution.SimpleConstSubstitution;

/**
 * A helper that adds (naive) conjunctive query support to an arbitrary
 * DatalogEngine by turning a conjunctive query into multiple singleton queries.
 */
public final class ConjunctiveQueryHelper {

	private ConjunctiveQueryHelper() {
		throw new AssertionError("impossible");
	}
	
	public static Set<ConstOnlySubstitution> query(DatalogEngine engine, List<PositiveAtom> query) {
		Set<ConstOnlySubstitution> r = new LinkedHashSet<>();
		query(engine, query, 0, new SimpleConstSubstitution(), r);
		return r;
	}

	private static void query(DatalogEngine engine, List<PositiveAtom> query, int pos, SimpleConstSubstitution subst,
			Set<ConstOnlySubstitution> acc) {
		if (pos >= query.size()) {
			acc.add(subst);
			return;
		}
		PositiveAtom curQ = query.get(pos).applySubst(subst);
		for (PositiveAtom fact : engine.query(curQ)) {
			SimpleConstSubstitution curSubst = new SimpleConstSubstitution(subst);
			Term[] qArgs = curQ.getArgs();
			Term[] fArgs = fact.getArgs();
			for (int i = 0; i < qArgs.length; ++i) {
				Term qArg = qArgs[i];
				if (qArg instanceof Variable) {
					curSubst.put((Variable) qArg, (Constant) fArgs[i]);
				}
			}
			query(engine, query, pos + 1, curSubst, acc);
		}
	}

}
