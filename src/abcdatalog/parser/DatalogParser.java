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
package abcdatalog.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import abcdatalog.ast.BinaryDisunifier;
import abcdatalog.ast.BinaryUnifier;
import abcdatalog.ast.Clause;
import abcdatalog.ast.Constant;
import abcdatalog.ast.NegatedAtom;
import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.PredicateSym;
import abcdatalog.ast.Premise;
import abcdatalog.ast.Term;
import abcdatalog.ast.Variable;

/**
 * A recursive descent parser for Datalog. <br>
 * <br>
 * A Datalog program is a set of clauses, where each clause is in the form "a0
 * :- a1, ..., an." or "a0." and each ai is an atom of the form "pi" or "pi(t1,
 * ..., tki)" for ki > 0 such that pi is a predicate symbol and each tj for 0 <
 * j <= ki is a term (i.e. a constant or variable). Any variable in a0 must
 * appear in at least one of ai, ..., an. Identifiers can contain letters,
 * digits and underscores. Identifiers that begin with an upper case letter or
 * an underscore are parsed as variables.
 *
 */
public final class DatalogParser {

	/**
	 * Class cannot be instantiated.
	 */
	private DatalogParser() {
	}

	/**
	 * Generates an abstract syntax tree representation of the program described
	 * by the provided token stream.
	 * 
	 * @param t
	 *            the token stream representation of program
	 * @return the AST of program
	 * @throws DatalogParseException
	 */
	public static Set<Clause> parseProgram(DatalogTokenizer t) throws DatalogParseException {
		Set<Clause> clauses = new HashSet<>();
		while (t.hasNext()) {
			clauses.add(parseClause(t));
		}
		return clauses;
	}

	/**
	 * Attempts to extract a clause from the provided token stream.
	 * 
	 * @param t
	 *            the token stream
	 * @return the clause
	 * @throws DatalogParseException
	 */
	private static Clause parseClause(DatalogTokenizer t) throws DatalogParseException {
		// Parse the head of the clause.
		PositiveAtom head = parsePositiveAtom(t);

		// Parse the body (if any).
		List<Premise> body = new ArrayList<>();
		// Set<Variable> bodyVars = new HashSet<>();
		if (!t.peek().equals(".")) {
			t.consume(":-");
			while (true) {
				body.add(parseConjunct(t));
				if (t.peek().equals(".")) {
					break;
				}
				t.consume(",");
			}
		}
		t.consume(".");

		return new Clause(head, body);
	}

	private static Premise parseConjunct(DatalogTokenizer t) throws DatalogParseException {
		String id = parseIdentifier(t);
		String next = t.peek();
		if (id.equals("not")) {
			return new NegatedAtom(parsePositiveAtom(t));
		}
		if (next.equals("=")) {
			return parseBinaryUnifier(id, t);
		}
		if (next.equals("!")) {
			return parseBinaryDisunifier(id, t);
		}
		return parsePositiveAtom(id, t);
	}

	private static BinaryUnifier parseBinaryUnifier(String first, DatalogTokenizer t) throws DatalogParseException {
		Term t1 = parseTerm(first);
		t.consume("=");
		Term t2 = parseTerm(t.next());
		return new BinaryUnifier(t1, t2);
	}

	private static BinaryDisunifier parseBinaryDisunifier(String first, DatalogTokenizer t)
			throws DatalogParseException {
		Term t1 = parseTerm(first);
		t.consume("!=");
		Term t2 = parseTerm(t.next());
		return new BinaryDisunifier(t1, t2);
	}

	private static PositiveAtom parsePositiveAtom(String predSym, DatalogTokenizer t) throws DatalogParseException {
		char first = predSym.charAt(0);
		if (Character.isUpperCase(first)) {
			throw new DatalogParseException(
					"Invalid predicate symbol \"" + predSym + "\" begins with an upper case letter.");
		}
		if (first == '_') {
			throw new DatalogParseException("Invalid predicate symbol \"" + predSym + "\" begins with an underscore.");
		}

		ArrayList<Term> args = new ArrayList<>();
		// If followed by a parenthesis, must have arity greater than zero.
		if (t.peek().equals("(")) {
			t.consume("(");
			while (true) {
				args.add(parseTerm(t.next()));
				if (t.peek().equals(")")) {
					// End of the arguments!
					break;
				}
				t.consume(",");
			}
			t.consume(")");
		}
		args.trimToSize();
		Term[] array = new Term[args.size()];
		for (int i = 0; i < args.size(); ++i) {
			array[i] = args.get(i);
		}
		return PositiveAtom.create(PredicateSym.create(predSym, args.size()), array);
	}

	/**
	 * Attempts to extract an atom from the provided token stream.
	 * 
	 * @param t
	 *            the token stream
	 * @param vars
	 *            records the variables in this atom
	 * @return the atom
	 * @throws DatalogParseException
	 */
	private static PositiveAtom parsePositiveAtom(DatalogTokenizer t) throws DatalogParseException {
		return parsePositiveAtom(parseIdentifier(t), t);
	}

	private static Term parseTerm(String s) throws DatalogParseException {
		if (s.equals("_")) {
			return Variable.createFreshVariable();
		}
		char c = s.charAt(0);
		if (Character.isUpperCase(c) || c == '_') {
			return Variable.create(s);
		}
		return Constant.create(s);
	}

	/**
	 * Attempts to extract a valid identifier from the supplied token stream.
	 * Valid identifiers are formed from alphanumeric characters and
	 * underscores.
	 * 
	 * @param t
	 *            the token stream
	 * @return the identifier
	 * @throws DatalogParseException
	 */
	private static String parseIdentifier(DatalogTokenizer t) throws DatalogParseException {
		String s = t.next();
		// Check to make sure it contains only appropriate characters for an
		// identifier.
		boolean okay = true;
		for (int i = 0; i < s.length(); ++i) {
			okay &= Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_';
		}
		if (!okay) {
			throw new DatalogParseException("Invalid identifier \"" + s + "\" not alphanumeric.");
		}
		return s;
	}

	/**
	 * Attempts to extract an atom representation of the query described in the
	 * token stream.
	 * 
	 * @param t
	 *            the token stream
	 * @return the atom
	 * @throws DatalogParseException
	 */
	public static PositiveAtom parseQuery(DatalogTokenizer t) throws DatalogParseException {
		PositiveAtom r = parsePositiveAtom(t);
		t.consume("?");
		return r;
	}

	/**
	 * Extracts an atom from the token stream. The atom must be followed by a
	 * period.
	 * 
	 * @param t
	 *            the token stream
	 * @return the atom
	 * @throws DatalogParseException
	 */
	public static PositiveAtom parseClauseAsPositiveAtom(DatalogTokenizer t) throws DatalogParseException {
		PositiveAtom r = parsePositiveAtom(t);
		t.consume(".");
		return r;
	}

	// Basic demonstration of parser.
	public static void main(String[] args) throws DatalogParseException {
		String source = "lk_1(a,b). lk_1(b,c). reachable(X,Y) :- lk_1(X,Y)."
				+ "reachable(X,Y) :- lk_1(X,Z), %ignore\n not reachable(Z,Y).";
		DatalogTokenizer t = new DatalogTokenizer(new StringReader(source));
		for (Clause c : parseProgram(t)) {
			System.out.println(c);
		}

		String query = "lk_1(a,X)?";
		t = new DatalogTokenizer(new StringReader(query));
		System.out.println(parseQuery(t));

		t = new DatalogTokenizer(new StringReader("X != Y"));
		System.out.println(parseConjunct(t));
	}
}
