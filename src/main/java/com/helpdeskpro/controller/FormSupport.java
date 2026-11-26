package com.helpdeskpro.controller;

import com.helpdeskpro.exception.DuplicateResourceException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.BindingResult;

/** Turns service-level validation and duplicate errors into field errors, so that forms keep what was typed. */
final class FormSupport {

	private FormSupport() {
	}

	/** Each violation is attached to the form field of the same name, or to the form itself if there is none. */
	static void reject(BindingResult result, ConstraintViolationException exception) {
		for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
			String field = lastNode(violation.getPropertyPath());
			if (field != null && result.getTarget() != null
					&& BeanUtils.getPropertyDescriptor(result.getTarget().getClass(), field) != null) {
				result.rejectValue(field, "invalid", violation.getMessage());
			}
			else {
				result.reject("invalid", violation.getMessage());
			}
		}
	}

	static void reject(BindingResult result, String field, DuplicateResourceException exception) {
		result.rejectValue(field, "duplicate", exception.getMessage());
	}

	private static String lastNode(Path path) {
		String name = null;
		for (Path.Node node : path) {
			name = node.getName();
		}
		return name;
	}

}
