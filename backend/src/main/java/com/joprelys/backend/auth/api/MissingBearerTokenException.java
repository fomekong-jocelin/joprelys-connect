package com.joprelys.backend.auth.api;

public class MissingBearerTokenException extends RuntimeException {

	public MissingBearerTokenException(String message) {
		super(message);
	}
}
