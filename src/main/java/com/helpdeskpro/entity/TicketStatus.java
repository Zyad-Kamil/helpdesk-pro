package com.helpdeskpro.entity;

/** Ticket life cycle: OPEN, then IN_PROGRESS, then RESOLVED, then CLOSED. */
public enum TicketStatus {

	OPEN, IN_PROGRESS, RESOLVED, CLOSED;

	/** The only status a ticket may move to from this one, or {@code null} if it is final. */
	public TicketStatus next() {
		return switch (this) {
			case OPEN -> IN_PROGRESS;
			case IN_PROGRESS -> RESOLVED;
			case RESOLVED -> CLOSED;
			case CLOSED -> null;
		};
	}

}
