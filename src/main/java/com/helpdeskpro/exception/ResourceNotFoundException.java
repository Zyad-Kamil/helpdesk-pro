package com.helpdeskpro.exception;

/** Raised when an identifier does not match any stored entity. */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resource, Long id) {
		super(resource + " introuvable (identifiant " + id + ").");
	}

}
