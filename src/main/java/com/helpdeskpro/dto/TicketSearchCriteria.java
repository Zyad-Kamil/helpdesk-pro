package com.helpdeskpro.dto;

import com.helpdeskpro.entity.TicketPriority;
import com.helpdeskpro.entity.TicketStatus;

/**
 * Optional filters combined with AND; {@code null} or blank means "no filter".
 *
 * @param overdue when {@code true}, keeps only open or in-progress tickets past their due date
 */
public record TicketSearchCriteria(String keyword, TicketStatus status, TicketPriority priority, Long technicianId,
		Long categoryId, Long tagId, Boolean overdue) {

	public TicketSearchCriteria(String keyword, TicketStatus status, TicketPriority priority, Long technicianId,
			Long categoryId, Long tagId) {
		this(keyword, status, priority, technicianId, categoryId, tagId, null);
	}

	public static TicketSearchCriteria none() {
		return new TicketSearchCriteria(null, null, null, null, null, null, null);
	}

}
