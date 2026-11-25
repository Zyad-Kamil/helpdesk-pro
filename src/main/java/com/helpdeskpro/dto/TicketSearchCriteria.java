package com.helpdeskpro.dto;

import com.helpdeskpro.entity.TicketPriority;
import com.helpdeskpro.entity.TicketStatus;

/** Optional filters combined with AND; {@code null} or blank means "no filter". */
public record TicketSearchCriteria(String keyword, TicketStatus status, TicketPriority priority, Long technicianId,
		Long categoryId, Long tagId) {

	public static TicketSearchCriteria none() {
		return new TicketSearchCriteria(null, null, null, null, null, null);
	}

}
