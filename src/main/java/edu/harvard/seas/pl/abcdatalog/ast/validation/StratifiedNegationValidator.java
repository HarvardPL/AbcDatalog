package edu.harvard.seas.pl.abcdatalog.ast.validation;

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

import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.PredicateSym;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidator.ValidClause;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class for validating that an unstratified program can be successfully stratified for negation.
 */
public final class StratifiedNegationValidator {

  private StratifiedNegationValidator() {
    // Cannot be instantiated.
  }

  /**
   * Validates that the given unstratified program can be stratified for negation and returns a
   * witness stratified program.
   *
   * @param prog the unstratified program
   * @return the stratified program
   * @throws DatalogValidationException if the given program cannot be stratified for negation
   */
  public static StratifiedProgram validate(UnstratifiedProgram prog)
      throws DatalogValidationException {
    StratifiedNegationGraph g = StratifiedNegationGraph.create(prog);
    return new StratifiedProgram() {

      @Override
      public Set<ValidClause> getRules() {
        return prog.getRules();
      }

      @Override
      public Set<PositiveAtom> getInitialFacts() {
        return prog.getInitialFacts();
      }

      @Override
      public Set<PredicateSym> getEdbPredicateSyms() {
        return prog.getEdbPredicateSyms();
      }

      @Override
      public Set<PredicateSym> getIdbPredicateSyms() {
        return prog.getIdbPredicateSyms();
      }

      @Override
      public List<Set<PredicateSym>> getStrata() {
        return g.getStrata();
      }

      @Override
      public Map<PredicateSym, Integer> getPredToStratumMap() {
        return g.getPredToStratumMap();
      }
    };
  }
}
