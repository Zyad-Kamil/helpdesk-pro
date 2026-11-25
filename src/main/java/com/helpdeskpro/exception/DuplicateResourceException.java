package com.helpdeskpro.exception;

/** Raised when a unique value (name, e-mail) is already used, ignoring case. */
public class DuplicateResourceException extends RuntimeException {

	public DuplicateResourceException(String message) {
		super(message);
	}

}
