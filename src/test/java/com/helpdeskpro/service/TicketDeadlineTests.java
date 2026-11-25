package com.helpdeskpro.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.helpdeskpro.dto.TicketResponse;
import com.helpdeskpro.dto.TicketStatusRequest;
import com.helpdeskpro.entity.TicketPriority;
import com.helpdeskpro.entity.TicketStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ServiceTest
class TicketDeadlineTests extends AbstractServiceTests {

	@Test
	void resolutionTargetsAreExpressedInCalendarHours() {
		assertThat(TicketPriority.LOW.getResolutionTarget()).isEqualTo(Duration.ofHours(72));
		assertThat(TicketPriority.MEDIUM.getResolutionTarget()).isEqualTo(Duration.ofHours(48));
		assertThat(TicketPriority.HIGH.getResolutionTarget()).isEqualTo(Duration.ofHours(24));
		assertThat(TicketPriority.CRITICAL.getResolutionTarget()).isEqualTo(Duration.ofHours(4));
	}

	@Test
	void dueDateIsCreationDatePlusTheTargetOfThePriority() {
		for (TicketPriority priority : TicketPriority.values()) {
			TicketResponse ticket = newTicket(priority);
			assertThat(ticket.dueAt()).isEqualTo(ticket.createdAt().plus(priority.getResolutionTarget()));
		}
	}

	@Test
	void activeTicketIsOverdueOnlyAfterItsDueDate() {
		TicketResponse critical = newTicket(TicketPriority.CRITICAL);
		LocalDateTime due = critical.dueAt();

		this.clock.set(due.minusMinutes(1));
		assertThat(this.ticketService.findById(critical.id()).overdue()).isFalse();
		assertThat(this.ticketService.findOverdue()).isEmpty();

		this.clock.set(due);
		assertThat(this.ticketService.findById(critical.id()).overdue()).isFalse();

		this.clock.set(due.plusSeconds(1));
		assertThat(this.ticketService.findById(critical.id()).overdue()).isTrue();
		assertThat(this.ticketService.findOverdue()).extracting(TicketResponse::id).containsExactly(critical.id());
	}

	@Test
	void overdueTicketsAreDetectedPerPriorityAndSortedByDueDate() {
		TicketResponse low = newTicket(TicketPriority.LOW);
		TicketResponse high = newTicket(TicketPriority.HIGH);
		TicketResponse critical = newTicket(TicketPriority.CRITICAL);
		TicketResponse inProgress = ticketIn(TicketStatus.IN_PROGRESS);

		this.clock.set(high.createdAt().plusHours(30));
		assertThat(this.ticketService.findOverdue()).extracting(TicketResponse::id)
			.containsExactlyInAnyOrder(high.id(), critical.id());
		assertThat(this.ticketService.findOverdue()).extracting(TicketResponse::dueAt).isSorted();

		this.clock.set(low.createdAt().plusDays(10));
		assertThat(this.ticketService.findOverdue()).extracting(TicketResponse::id)
			.contains(low.id(), high.id(), critical.id(), inProgress.id());
	}

	@Test
	void resolvedAndClosedTicketsAreNeverOverdueButTheirTargetIsEvaluatedFromResolvedAt() {
		TicketResponse onTime = ticketIn(TicketStatus.IN_PROGRESS);
		TicketResponse late = ticketIn(TicketStatus.IN_PROGRESS);

		this.clock.set(onTime.dueAt().minusHours(1));
		TicketResponse resolvedOnTime = this.ticketService.changeStatus(onTime.id(),
				new TicketStatusRequest(TicketStatus.RESOLVED, "Fixed"));
		this.clock.set(late.dueAt().plusHours(1));
		TicketResponse resolvedLate = this.ticketService.changeStatus(late.id(),
				new TicketStatusRequest(TicketStatus.RESOLVED, "Fixed"));

		assertThat(resolvedOnTime.resolvedWithinTarget()).isTrue();
		assertThat(resolvedLate.resolvedWithinTarget()).isFalse();
		assertThat(resolvedOnTime.overdue()).isFalse();
		assertThat(resolvedLate.overdue()).isFalse();

		// Much later, closed tickets still keep their verdict and are not reported as overdue
		this.clock.set(late.dueAt().plusDays(100));
		TicketResponse closedLate = this.ticketService.changeStatus(late.id(),
				new TicketStatusRequest(TicketStatus.CLOSED, null));
		assertThat(closedLate.resolvedWithinTarget()).isFalse();
		assertThat(closedLate.overdue()).isFalse();
		assertThat(this.ticketService.findOverdue()).isEmpty();
		assertThat(this.ticketService.findById(onTime.id()).resolvedWithinTarget()).isTrue();
	}

	@Test
	void resolvedExactlyAtTheDueDateRespectsTheTarget() {
		TicketResponse ticket = ticketIn(TicketStatus.IN_PROGRESS);
		this.clock.set(ticket.dueAt());

		TicketResponse resolved = this.ticketService.changeStatus(ticket.id(),
				new TicketStatusRequest(TicketStatus.RESOLVED, "Fixed"));

		assertThat(resolved.resolvedWithinTarget()).isTrue();
	}

	@Test
	void activeTicketHasNoTargetVerdictYet() {
		assertThat(List.of(newTicket(), ticketIn(TicketStatus.IN_PROGRESS))).extracting(TicketResponse::resolvedWithinTarget)
			.containsOnlyNulls();
	}

}
