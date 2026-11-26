package com.helpdeskpro.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.helpdeskpro.dto.CategoryRequest;
import com.helpdeskpro.dto.CategoryResponse;
import com.helpdeskpro.dto.TagRequest;
import com.helpdeskpro.dto.TagResponse;
import com.helpdeskpro.dto.TechnicianRequest;
import com.helpdeskpro.dto.TechnicianResponse;
import com.helpdeskpro.dto.TicketRequest;
import com.helpdeskpro.dto.TicketResponse;
import com.helpdeskpro.dto.TicketStatusRequest;
import com.helpdeskpro.entity.TicketPriority;
import com.helpdeskpro.entity.TicketStatus;
import com.helpdeskpro.service.CategoryService;
import com.helpdeskpro.service.TagService;
import com.helpdeskpro.service.TechnicianProfileService;
import com.helpdeskpro.service.TechnicianService;
import com.helpdeskpro.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Real application context, real templates and an in-memory database. Each test runs in a transaction that is
 * rolled back, and the services prepare its data in that same transaction.
 */
abstract class AbstractWebTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	TicketService ticketService;

	@Autowired
	TechnicianService technicianService;

	@Autowired
	TechnicianProfileService profileService;

	@Autowired
	CategoryService categoryService;

	@Autowired
	TagService tagService;

	@Autowired
	MovableClock clock;

	@BeforeEach
	void resetClock() {
		this.clock.set(null);
	}

	TicketResponse newTicket(String title) {
		return this.ticketService.create(
				new TicketRequest(title, "Description of " + title, "Alice Martin", "alice@example.com", null));
	}

	TicketResponse newTicket(String title, TicketPriority priority) {
		return this.ticketService.create(
				new TicketRequest(title, "Description of " + title, "Alice Martin", "alice@example.com", priority));
	}

	TechnicianResponse newTechnician(String email) {
		return this.technicianService.create(new TechnicianRequest("Jean", "Dupont", email));
	}

	CategoryResponse newCategory(String name) {
		return this.categoryService.create(new CategoryRequest(name, null));
	}

	TagResponse newTag(String name) {
		return this.tagService.create(new TagRequest(name));
	}

	TicketResponse inProgressTicket(String title) {
		TicketResponse ticket = newTicket(title);
		this.ticketService.assignTechnician(ticket.id(), newTechnician(title.hashCode() + "@example.com").id());
		return this.ticketService.changeStatus(ticket.id(), new TicketStatusRequest(TicketStatus.IN_PROGRESS, null));
	}

	TicketResponse closedTicket(String title) {
		TicketResponse ticket = inProgressTicket(title);
		this.ticketService.changeStatus(ticket.id(), new TicketStatusRequest(TicketStatus.RESOLVED, "Fixed"));
		return this.ticketService.changeStatus(ticket.id(), new TicketStatusRequest(TicketStatus.CLOSED, null));
	}

	/** A POST coming from the given page, as a browser does (the refusals go back to that page). */
	static MockHttpServletRequestBuilder postFrom(String page, String url) {
		return post(url).header("Referer", "http://localhost" + page);
	}

	/** Clock the tests can move forward; it follows the system clock until set. */
	static class MovableClock extends Clock {

		private Instant fixed;

		void set(LocalDateTime dateTime) {
			this.fixed = (dateTime != null) ? dateTime.atZone(getZone()).toInstant() : null;
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.systemDefault();
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return Clock.fixed(instant(), zone);
		}

		@Override
		public Instant instant() {
			return (this.fixed != null) ? this.fixed : Instant.now();
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ClockConfig {

		@Bean
		@Primary
		MovableClock movableClock() {
			return new MovableClock();
		}

	}

}
