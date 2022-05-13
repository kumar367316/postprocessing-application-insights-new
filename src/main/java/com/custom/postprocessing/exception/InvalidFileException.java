package com.custom.postprocessing.exception;

/**
 * @author kumar.charanswain
 *
 */

public class InvalidFileException extends RuntimeException{
	
	private static final long serialVersionUID = 1L;
	
	private String message;
	private int code;
	public InvalidFileException(String message, int code) {
		super();
		this.message = message;
		this.code = code;
	}

}
