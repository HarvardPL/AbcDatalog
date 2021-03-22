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
package abcdatalog.engine.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import abcdatalog.ast.Clause;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.engine.DatalogEngine;
import abcdatalog.parser.DatalogParseException;
import abcdatalog.parser.DatalogParser;
import abcdatalog.parser.DatalogTokenizer;

public abstract class AbstractTests {
	private final Supplier<DatalogEngine> engineFactory;
	
	public AbstractTests(Supplier<DatalogEngine> engineFactory) {
		this.engineFactory = engineFactory;
	}
	
	/**
	 * Instantiates an engine with the given program. This wrapper makes it easy
	 * to switch engine implementations.
	 * 
	 * @param program
	 *            the program
	 * @return the engine
	 */
	protected DatalogEngine initEngine(String program) {
		DatalogEngine e = engineFactory.get();
		try {
			e.init(parseCode(program));
		} catch (DatalogValidationException e1) {
			fail("Validation error: " + e1.getMessage());
		}
		return e;
	}
	
	protected DatalogEngine initEngineUnsafe(String program) throws DatalogValidationException {
		DatalogEngine e = engineFactory.get();
		e.init(parseCode(program));
		return e;
	}
	
	protected DatalogEngine initEngineUnsafe(Reader program) throws DatalogValidationException {
		DatalogEngine e = engineFactory.get();
		e.init(parseCode(program));
		return e;
	}
	
	/**
	 * Builds a Datalog AST out of the string representation of the source code.
	 * 
	 * @param src
	 *            the source code
	 * @return the AST representation of the code
	 */
	protected Set<Clause> parseCode(String src) {
		return parseCode(new StringReader(src));
	}
	
	protected Set<Clause> parseCode(Reader reader) {
		DatalogTokenizer t = new DatalogTokenizer(reader);
		try {
			return DatalogParser.parseProgram(t);
		} catch (DatalogParseException e) {
			fail("Parsing error: " + e.getMessage());
			// Never reached.
			return null;
		}
	}

	/**
	 * Builds a query AST out of the string representation of the query.
	 * 
	 * @param q
	 *            the string representation of the query
	 * @return the AST representation of the query
	 */
	protected PositiveAtom parseQuery(String q) {
		DatalogTokenizer t = new DatalogTokenizer(new StringReader(q));
		try {
			return DatalogParser.parseQuery(t);
		} catch (DatalogParseException e) {
			fail("Parsing error: " + e.getMessage());
			// Never reached.
			return null;
		}
	}

	/**
	 * Builds an AST representation of a set of facts (i.e. atoms) out of a
	 * string representation of Datalog facts.
	 * 
	 * @param facts
	 *            the string representation of the facts
	 * @return the AST representation of the facts
	 */
	protected Set<PositiveAtom> parseFacts(String facts) {
		DatalogTokenizer t = new DatalogTokenizer(new StringReader(facts));
		Set<PositiveAtom> r = new LinkedHashSet<>();
		try {
			while (t.hasNext()) {
				r.add(DatalogParser.parseClauseAsPositiveAtom(t));
			}
			return r;
		} catch (DatalogParseException e) {
			fail("Parsing error: " + e.getMessage());
			// Never reached.
			return null;
		}
	}
	
	protected void test(String program, String query, String expected) throws DatalogValidationException {
		DatalogEngine e = initEngineUnsafe(program);
		Set<PositiveAtom> rs = e.query(parseQuery(query));
		Set<PositiveAtom> facts = parseFacts(expected);
		assertEquals(rs.size(), facts.size());
		assertTrue(rs.containsAll(facts));
	}
	
	protected void testFile(String file, String query, String expected) throws DatalogValidationException {
		InputStream is = getClass().getClassLoader().getResourceAsStream(file);
		DatalogEngine e = initEngineUnsafe(new InputStreamReader(is));
		Set<PositiveAtom> rs = e.query(parseQuery(query));
		Set<PositiveAtom> facts = parseFacts(expected);
		assertEquals(rs.size(), facts.size());
		assertTrue(rs.containsAll(facts));
	}

}
