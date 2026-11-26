package com.helpdeskpro.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.helpdeskpro.entity.TicketPriority;
import com.helpdeskpro.entity.TicketStatus;

/**
 * Statistics computed from the stored tickets and technicians.
 *
 * @param ticketsByStatus every status is present, in life-cycle order, with 0 when empty
 * @param ticketsByPriority every priority is present, in ascending order, with 0 when empty
 * @param resolvedWithinTarget resolved or closed tickets that met their due date
 * @param resolvedOutsideTarget resolved or closed tickets that missed their due date
 * @param urgentTickets open or in-progress tickets that are overdue or critical, the earliest due date first
 */
public record DashboardResponse(long totalTickets, Map<TicketStatus, Long> ticketsByStatus,
		Map<TicketPriority, Long> ticketsByPriority, long overdueTickets, long activeTechnicians,
		long resolvedWithinTarget, long resolvedOutsideTarget, List<TicketResponse> recentTickets,
		List<TicketResponse> urgentTickets) {

	/** One slice of the status donut chart; {@code percent} and {@code offset} are on a 0-100 scale. */
	public record Segment(TicketStatus status, long count, double percent, double offset) {
	}

	public long count(TicketStatus status) {
		return this.ticketsByStatus.getOrDefault(status, 0L);
	}

	public long count(TicketPriority priority) {
		return this.ticketsByPriority.getOrDefault(priority, 0L);
	}

	/** Statuses that have at least one ticket, with the cumulative offset needed to draw the donut. */
	public List<Segment> statusSegments() {
		List<Segment> segments = new ArrayList<>();
		double offset = 0;
		for (TicketStatus status : TicketStatus.values()) {
			long count = count(status);
			if (count > 0) {
				double percent = count * 100.0 / this.totalTickets;
				segments.add(new Segment(status, count, percent, offset));
				offset += percent;
			}
		}
		return segments;
	}

	/** Number of resolved or closed tickets, for which the due date can be judged. */
	public long judgedTickets() {
		return this.resolvedWithinTarget + this.resolvedOutsideTarget;
	}

	/** Share (0-100) of judged tickets resolved on time. */
	public long onTimePercent() {
		return (judgedTickets() == 0) ? 0 : Math.round(this.resolvedWithinTarget * 100.0 / judgedTickets());
	}

}
