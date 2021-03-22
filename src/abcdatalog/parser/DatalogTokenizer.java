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

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;

/**
 * A tokenizer for Datalog. <br>
 * <br>
 * The character '%' begins a single line comment. Newlines are treated like
 * normal whitespace, to the effect that a clause can extend over multiple
 * lines. Alphanumeric characters and underscores are grouped together, but
 * other punctuation is tokenized character by character.
 * 
 */
public class DatalogTokenizer {
	/**
	 * A stream of tokens.
	 */
	private final StreamTokenizer st;

	/**
	 * Construct a stream of Datalog tokens from a Reader.
	 * 
	 * @param r
	 *            the Reader
	 */
	public DatalogTokenizer(Reader r) {
		this.st = new StreamTokenizer(r);
		this.st.commentChar('%');
		// Treat numbers as strings.
		this.st.ordinaryChars('0', '9');
		this.st.ordinaryChar('.');
		this.st.wordChars('0', '9');
		this.st.wordChars('_', '_');
	}

	/**
	 * Returns the string signified by the given token code.
	 * 
	 * @param token
	 *            the token code
	 * @return the string representation
	 * @throws DatalogParseException
	 */
	private String stringifyToken(int token) throws DatalogParseException {
		switch (this.st.ttype) {

		case StreamTokenizer.TT_EOF:
			throw new DatalogParseException("Unexpected EOF token.");
		case StreamTokenizer.TT_EOL:
			// This case should never be reached, since EOLs are treated as
			// having no special significance.
			// Fall through to...
		case StreamTokenizer.TT_NUMBER:
			// This case should never be reached, since numbers are tokenized as
			// strings.
			throw new AssertionError();
		case StreamTokenizer.TT_WORD:
			return this.st.sval;
		default:
			return Character.toString((char) token);
		}
	}

	/**
	 * Attempts to consume the supplied string from the beginning of the token
	 * stream. An exception is thrown if the string does not match the token
	 * stream. The string must describe complete (i.e. not partial) tokens.
	 * 
	 * @param s
	 *            the string representation of the tokens to be consumed
	 * @throws DatalogParseException
	 */
	public void consume(String s) throws DatalogParseException {
		try {
			String token = "";
			while (token.length() < s.length()) {
				token += stringifyToken(this.st.nextToken());
			}

			if (!token.equals(s)) {
				throw new DatalogParseException("Tried to consume \"" + s
						+ "\" but found \"" + token + "\".");
			}
		} catch (IOException e) {
			throw new DatalogParseException(e);
		}
	}

	/**
	 * Returns the next token in this stream without consuming it. Throws an
	 * exception if at EOF.
	 * 
	 * @return the string representation of token
	 * @throws DatalogParseException
	 */
	public String peek() throws DatalogParseException {
		try {
			String token = stringifyToken(this.st.nextToken());
			this.st.pushBack();
			return token;
		} catch (IOException e) {
			throw new DatalogParseException(e);
		}
	}

	/**
	 * Returns (and consumes) the next token in this stream. Throws an exception
	 * if at EOF.
	 * 
	 * @return the string representation of token
	 * @throws DatalogParseException
	 */
	public String next() throws DatalogParseException {
		try {
			String token = stringifyToken(this.st.nextToken());
			return token;
		} catch (IOException e) {
			throw new DatalogParseException(e);
		}
	}

	/**
	 * Returns whether there is another token in this stream.
	 * 
	 * @return whether there is another token
	 * @throws DatalogParseException
	 */
	public boolean hasNext() throws DatalogParseException {
		try {
			this.st.nextToken();
			this.st.pushBack();
			return this.st.ttype != StreamTokenizer.TT_EOF;
		} catch (IOException e) {
			throw new DatalogParseException(e);
		}
	}

}
