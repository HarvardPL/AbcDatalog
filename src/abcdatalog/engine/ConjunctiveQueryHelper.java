package abcdatalog.engine;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import abcdatalog.ast.Constant;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.Term;
import abcdatalog.ast.Variable;
import abcdatalog.util.substitution.ConstOnlySubstitution;
import abcdatalog.util.substitution.SimpleConstSubstitution;

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
