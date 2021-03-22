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
package abcdatalog.executor;

import java.io.StringReader;
import java.util.Collections;
import java.util.Set;

import abcdatalog.ast.Clause;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.parser.DatalogParseException;
import abcdatalog.parser.DatalogParser;
import abcdatalog.parser.DatalogTokenizer;

/**
 * A basic demonstration of how to use a Datalog executor.
 *
 */
public class ExecutorExample {

	/**
	 * Waits for a second, prints the provided fact, and adds it to the
	 * executor.
	 * 
	 * @param ex
	 *            the executor
	 * @param fact
	 *            the fact
	 */
	private static void waitAndAdd(DatalogExecutor ex, PositiveAtom fact) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Adding... " + fact);
		ex.addFactAsynchronously(fact);
	}

	/**
	 * Runs an executor on a basic graph transitive closure example.
	 * 
	 * @param args
	 *            the command-line arguments (not used)
	 * @throws DatalogParseException
	 *             if there is a parsing error
	 * @throws DatalogValidationException 
	 */
	public static void main(String[] args) throws DatalogParseException, DatalogValidationException {
		String program = "edge(0,1). edge(1,2). tc(X,Y) :- edge(X,Y)."
				+ "tc(X,Y) :- edge(X,Z), tc(Z,Y). cycle :- tc(X,X).";
		DatalogTokenizer t = new DatalogTokenizer(new StringReader(program));
		Set<Clause> ast = DatalogParser.parseProgram(t);
		PredicateSym edge = PredicateSym.create("edge", 2);
		PredicateSym tc = PredicateSym.create("tc", 2);
		PredicateSym cycle = PredicateSym.create("cycle", 0);

		// 1. Initialize the executor.
		
		DatalogParallelExecutor ex = new DatalogParallelExecutor();
		ex.initialize(ast, Collections.singleton(edge));

		// 2. Register listeners.
		
		// Every time a new tuple is added to the transitive closure relation,
		// print it out.
		ex.registerListener(tc, new DatalogListener() {
			@Override
			public void newFactDerived(PositiveAtom fact) {
				synchronized (System.out) {
					System.out.println("Fact derived: " + fact);
				}
			}
		});

		// Notify us if a cycle is detected.
		ex.registerListener(cycle, new DatalogListener() {
			@Override
			public void newFactDerived(PositiveAtom fact) {
				synchronized (System.out) {
					System.out.println("*** Cycle detected. ***");
				}
			}
		});

		// 3. Start the executor.
		
		ex.start();
		
		// 4. Add new facts to the executor.

		String newFacts = "edge(2,3). edge(3,4). edge(4,0).";
		t = new DatalogTokenizer(new StringReader(newFacts));
		while (t.hasNext()) {
			waitAndAdd(ex, DatalogParser.parseClauseAsPositiveAtom(t));
		}
		
		ex.shutdown();
	}

}
