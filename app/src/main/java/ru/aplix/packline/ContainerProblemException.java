package ru.aplix.packline;

public class ContainerProblemException extends PackLineException {

	static final long serialVersionUID = 968597822654963893L;

	private String code;

	public ContainerProblemException() {
		super();
	}

	public ContainerProblemException(String message) {
		super(message);
	}

	public ContainerProblemException(String message, String code) {
		super(message);
		this.code = code;
	}

	public ContainerProblemException(String message, Throwable cause) {
		super(message, cause);
	}

	public ContainerProblemException(String message, Throwable cause, String code) {
		super(message, cause);
		this.code = code;
	}

	public ContainerProblemException(Throwable cause) {
		super(cause);
	}

	public ContainerProblemException(Throwable cause, String code) {
		super(cause);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
