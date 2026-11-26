package com.helpdeskpro.service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.helpdeskpro.dto.DashboardResponse;
import com.helpdeskpro.dto.TicketResponse;
import com.helpdeskpro.entity.TicketPriority;
import com.helpdeskpro.entity.TicketStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dashboard statistics, always computed from the real data. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

	static final int LIST_SIZE = 5;

	private final TicketService ticketService;

	private final TechnicianService technicianService;

	public DashboardService(TicketService ticketService, TechnicianService technicianService) {
		this.ticketService = ticketService;
		this.technicianService = technicianService;
	}

	public DashboardResponse compute() {
		// Newest first, as returned by the ticket search
		List<TicketResponse> tickets = this.ticketService.findAll();
		Map<TicketStatus, Long> byStatus = new EnumMap<>(TicketStatus.class);
		for (TicketStatus status : TicketStatus.values()) {
			byStatus.put(status, 0L);
		}
		Map<TicketPriority, Long> byPriority = new EnumMap<>(TicketPriority.class);
		for (TicketPriority priority : TicketPriority.values()) {
			byPriority.put(priority, 0L);
		}
		tickets.forEach((ticket) -> {
			byStatus.merge(ticket.status(), 1L, Long::sum);
			byPriority.merge(ticket.priority(), 1L, Long::sum);
		});
		List<TicketResponse> urgent = tickets.stream()
			.filter(TicketResponse::technicianChangeable)
			.filter((ticket) -> ticket.overdue() || ticket.priority() == TicketPriority.CRITICAL)
			.sorted(Comparator.comparing(TicketResponse::dueAt))
			.limit(LIST_SIZE)
			.toList();
		return new DashboardResponse(tickets.size(), byStatus, byPriority,
				tickets.stream().filter(TicketResponse::overdue).count(),
				this.technicianService.search(null, true).size(),
				tickets.stream().filter((ticket) -> Boolean.TRUE.equals(ticket.resolvedWithinTarget())).count(),
				tickets.stream().filter((ticket) -> Boolean.FALSE.equals(ticket.resolvedWithinTarget())).count(),
				tickets.stream().limit(LIST_SIZE).toList(), urgent);
	}

}
