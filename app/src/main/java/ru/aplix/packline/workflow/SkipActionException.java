package ru.aplix.packline.workflow;

public class SkipActionException extends RuntimeException {

	private static final long serialVersionUID = 4602270302887334605L;

	public SkipActionException() {
		super();
	}

	public SkipActionException(String message) {
		super(message);
	}

	public SkipActionException(String message, Throwable cause) {
		super(message, cause);
	}

	public SkipActionException(Throwable cause) {
		super(cause);
	}
}
