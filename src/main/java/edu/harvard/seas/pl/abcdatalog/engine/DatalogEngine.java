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

import edu.harvard.seas.pl.abcdatalog.ast.Clause;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidationException;
import edu.harvard.seas.pl.abcdatalog.util.substitution.ConstOnlySubstitution;
import java.util.List;
import java.util.Set;

/**
 * A Datalog evaluation engine. Datalog engines are initialized with a set of clauses that represent
 * initial facts and rules that can be used to derive new facts. After initialization, clients can
 * query about whether certain facts are derivable.
 */
public interface DatalogEngine {
  /**
   * Initializes engine with a Datalog program, including EDB facts. The set that is passed into
   * this method should include rules for deriving new facts as well as the initial facts, which can
   * be encoded as clauses with empty bodies.
   *
   * @param program program to evaluate
   * @throws DatalogValidationException
   * @throws IllegalStateException if this engine has already been initialized
   * @throws DatalogValidationException if the given program is invalid
   */
  void init(Set<Clause> program) throws DatalogValidationException;

  /**
   * Returns all facts that 1) can be derived from the rules and initial facts that were used to
   * initialize this engine and 2) unify with the query.
   *
   * @param q the query
   * @return facts
   * @throws IllegalStateException if this engine has not been initialized with a program
   */
  Set<PositiveAtom> query(PositiveAtom q);

  /**
   * Returns the set of all (minimal) substitutions that 1) ground the given conjunctive query, and
   * 2) make it true with respect to the Datalog program backing this engine. To get a concrete
   * solution to the conjunctive query, apply one of the returned substitutions to the list
   * representing the query (using, e.g., {@link
   * edu.harvard.seas.pl.abcdatalog.util.substitution.SubstitutionUtils#applyToPositiveAtoms(Substitution,
   * Iterable) SubstitutionUtils.applyToPositiveAtoms}).
   *
   * @param query the conjunctive query
   * @return the set of minimal satisfying substitutions
   * @throws IllegalStateException if this engine has not been initialized with a program
   */
  default Set<ConstOnlySubstitution> query(List<PositiveAtom> query) {
    return ConjunctiveQueryHelper.query(this, query);
  }
}
