package com.custom.postprocessing.exception;

/**
 * @author kumar.charanswain
 *
 */

public class ResourceNotFoundException extends RuntimeException{
	
	private static final long serialVersionUID = 1L;
	
	private String message;
	private int code;
	public ResourceNotFoundException(String message, int code) {
		super();
		this.message = message;
		this.code = code;
	}

}
