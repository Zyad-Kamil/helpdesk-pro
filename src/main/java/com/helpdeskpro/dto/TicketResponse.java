package com.helpdeskpro.dto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import com.helpdeskpro.entity.Ticket;
import com.helpdeskpro.entity.TicketPriority;
import com.helpdeskpro.entity.TicketStatus;

/**
 * Read model of a ticket, fully loaded: safe to use once the transaction is closed.
 *
 * @param dueAt creation date plus the resolution target of the priority
 * @param overdue {@code true} for an open or in-progress ticket past its due date
 * @param resolvedWithinTarget for a resolved or closed ticket, whether it was resolved by its due date;
 *        {@code null} while the ticket is still active
 */
public record TicketResponse(Long id, String title, String description, String requesterName, String requesterEmail,
		TicketStatus status, TicketPriority priority, LocalDateTime createdAt, LocalDateTime updatedAt,
		LocalDateTime resolvedAt, String resolution, LocalDateTime dueAt, boolean overdue,
		Boolean resolvedWithinTarget, Long technicianId, String technicianName, Long categoryId,
		String categoryName, List<TagResponse> tags) {

	/** The ticket can still be edited, categorized and tagged (only a closed ticket is frozen). */
	public boolean editable() {
		return this.status != TicketStatus.CLOSED;
	}

	/** A technician can be assigned or replaced while the ticket is open or in progress. */
	public boolean technicianChangeable() {
		return this.status == TicketStatus.OPEN || this.status == TicketStatus.IN_PROGRESS;
	}

	/** A technician can only be removed while the ticket is still open. */
	public boolean technicianRemovable() {
		return this.status == TicketStatus.OPEN && this.technicianId != null;
	}

	/** The only allowed next status, or {@code null}. */
	public TicketStatus nextStatus() {
		return this.status.next();
	}

	public static TicketResponse from(Ticket ticket, LocalDateTime dueAt, boolean overdue,
			Boolean resolvedWithinTarget) {
		return new TicketResponse(ticket.getId(), ticket.getTitle(), ticket.getDescription(),
				ticket.getRequesterName(), ticket.getRequesterEmail(), ticket.getStatus(), ticket.getPriority(),
				ticket.getCreatedAt(), ticket.getUpdatedAt(), ticket.getResolvedAt(), ticket.getResolution(),
				dueAt, overdue, resolvedWithinTarget,
				(ticket.getTechnician() != null) ? ticket.getTechnician().getId() : null,
				(ticket.getTechnician() != null)
						? ticket.getTechnician().getFirstName() + " " + ticket.getTechnician().getLastName() : null,
				(ticket.getCategory() != null) ? ticket.getCategory().getId() : null,
				(ticket.getCategory() != null) ? ticket.getCategory().getName() : null,
				ticket.getTags().stream()
					.map(TagResponse::from)
					.sorted(Comparator.comparing(TagResponse::name, String.CASE_INSENSITIVE_ORDER))
					.toList());
	}

}
