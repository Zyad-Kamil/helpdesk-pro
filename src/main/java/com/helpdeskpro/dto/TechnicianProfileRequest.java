package com.helpdeskpro.dto;

import jakarta.validation.constraints.Size;

/** All fields are optional; blank values are stored as {@code null}. */
public record TechnicianProfileRequest(@Size(max = 30) String phone, @Size(max = 100) String specialization,
		@Size(max = 2000) String biography) {

}
