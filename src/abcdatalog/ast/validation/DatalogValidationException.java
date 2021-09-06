package abcdatalog.ast.validation;

@SuppressWarnings("serial")
public class DatalogValidationException extends Exception {

	public DatalogValidationException() {}

	public DatalogValidationException(String message) {
		super(message);
	}

	public DatalogValidationException(Throwable cause) {
		super(cause);
	}

	public DatalogValidationException(String message, Throwable cause) {
		super(message, cause);
	}

	public DatalogValidationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
