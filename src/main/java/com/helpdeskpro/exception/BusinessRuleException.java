package com.helpdeskpro.exception;

/** Raised when an operation violates a business rule (invalid transition, entity still in use...). */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}

}
