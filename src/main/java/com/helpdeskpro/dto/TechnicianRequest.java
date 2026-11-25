package com.helpdeskpro.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Identity of a technician; activation is handled separately by the service. */
public record TechnicianRequest(
		@NotBlank @Size(max = 80) String firstName,
		@NotBlank @Size(max = 80) String lastName,
		@NotBlank @Email @Size(max = 255) String email) {

}
