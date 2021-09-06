package edu.harvard.seas.pl.abcdatalog.engine;

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
