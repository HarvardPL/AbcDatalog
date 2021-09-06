package edu.harvard.seas.pl.abcdatalog.parser;

/**
 * An exception signifying a parsing error.
 *
 */
@SuppressWarnings("serial")
public class DatalogParseException extends Exception {
	/**
	 * Constructs an exception signifying a parsing error.
	 */
	public DatalogParseException() {
	}

	/**
	 * Constructs an exception signifying a parsing error.
	 * 
	 * @param message
	 *            the error message
	 */
	public DatalogParseException(String message) {
		super(message);
	}

	/**
	 * Constructs an exception signifying a parsing error.
	 * 
	 * @param cause
	 *            the exception that caused this exception
	 */
	public DatalogParseException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs an exception signifying a parsing error.
	 * 
	 * @param message
	 *            the error message
	 * @param cause
	 *            the exception that caused this exception
	 */
	public DatalogParseException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs an exception signifying a parsing error.
	 * 
	 * @param message
	 *            the error message
	 * @param cause
	 *            the exception that caused this exception
	 * @param enableSuppression
	 *            whether or not suppression is enabled or disabled
	 * @param writableStackTrace
	 *            whether or not the stack trace should be writable
	 */
	public DatalogParseException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
