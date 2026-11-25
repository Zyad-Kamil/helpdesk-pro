package com.helpdeskpro.entity;

import java.time.Duration;

/** Ticket priority, with its resolution target expressed in calendar hours. */
public enum TicketPriority {

	LOW(72), MEDIUM(48), HIGH(24), CRITICAL(4);

	private final int targetHours;

	TicketPriority(int targetHours) {
		this.targetHours = targetHours;
	}

	/** Time allowed between creation and resolution. */
	public Duration getResolutionTarget() {
		return Duration.ofHours(this.targetHours);
	}

}
