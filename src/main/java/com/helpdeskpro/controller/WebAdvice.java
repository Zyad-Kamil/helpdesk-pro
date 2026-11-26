package com.helpdeskpro.controller;

import java.net.URI;
import java.util.stream.Collectors;

import com.helpdeskpro.entity.TicketPriority;
import com.helpdeskpro.entity.TicketStatus;
import com.helpdeskpro.exception.BusinessRuleException;
import com.helpdeskpro.exception.DuplicateResourceException;
import com.helpdeskpro.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Attributes shared by every view, and translation of service exceptions into pages and messages. */
@ControllerAdvice
class WebAdvice {

	@ModelAttribute("statuses")
	TicketStatus[] statuses() {
		return TicketStatus.values();
	}

	@ModelAttribute("priorities")
	TicketPriority[] priorities() {
		return TicketPriority.values();
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	String notFound(ResourceNotFoundException exception, Model model) {
		model.addAttribute("message", exception.getMessage());
		return "error/404";
	}

	/** A refused action goes back to the page it came from, with an explicit message. */
	@ExceptionHandler({ BusinessRuleException.class, DuplicateResourceException.class })
	String refused(RuntimeException exception, HttpServletRequest request, RedirectAttributes redirect) {
		redirect.addFlashAttribute("error", exception.getMessage());
		return backTo(request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	String invalid(ConstraintViolationException exception, HttpServletRequest request, RedirectAttributes redirect) {
		redirect.addFlashAttribute("error", exception.getConstraintViolations()
			.stream()
			.map(ConstraintViolation::getMessage)
			.sorted()
			.collect(Collectors.joining(" ; ", "Données invalides : ", ".")));
		return backTo(request);
	}

	/** Redirects to the referring page of this same host (path and query only), or to the home page. */
	private static String backTo(HttpServletRequest request) {
		String referer = request.getHeader("Referer");
		if (referer != null) {
			try {
				URI uri = URI.create(referer);
				String path = uri.getRawPath();
				if (request.getServerName().equals(uri.getHost()) && path != null && path.startsWith("/")
						&& !path.startsWith("//")) {
					return "redirect:" + path + ((uri.getRawQuery() != null) ? "?" + uri.getRawQuery() : "");
				}
			}
			catch (IllegalArgumentException ex) {
				// Malformed header: fall back to the home page
			}
		}
		return "redirect:/";
	}

}
