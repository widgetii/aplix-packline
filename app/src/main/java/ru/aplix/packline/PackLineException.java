package ru.aplix.packline;

public class PackLineException extends Exception {

	static final long serialVersionUID = 968597822654963893L;

	public PackLineException() {
		super();
	}

	public PackLineException(String message) {
		super(message);
	}

	public PackLineException(String message, Throwable cause) {
		super(message, cause);
	}

	public PackLineException(Throwable cause) {
		super(cause);
	}
}
