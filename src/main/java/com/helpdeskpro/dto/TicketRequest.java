package com.helpdeskpro.dto;

import com.helpdeskpro.entity.TicketPriority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Creation or edition of a ticket. The status, resolution, technician, category and tags are
 * never set here: they change through dedicated service operations that enforce the business rules.
 *
 * @param priority optional: {@code MEDIUM} on creation, unchanged on edition when {@code null}
 */
public record TicketRequest(
		@NotBlank @Size(max = 150) String title,
		@NotBlank @Size(max = 4000) String description,
		@NotBlank @Size(max = 160) String requesterName,
		@NotBlank @Email @Size(max = 255) String requesterEmail,
		TicketPriority priority) {

}
