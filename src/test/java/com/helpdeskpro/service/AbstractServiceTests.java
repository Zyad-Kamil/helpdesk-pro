package com.helpdeskpro.service;

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
import com.helpdeskpro.repository.CategoryRepository;
import com.helpdeskpro.repository.TagRepository;
import com.helpdeskpro.repository.TechnicianRepository;
import com.helpdeskpro.repository.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

abstract class AbstractServiceTests {

	@Autowired
	TicketService ticketService;

	@Autowired
	TechnicianService technicianService;

	@Autowired
	CategoryService categoryService;

	@Autowired
	TagService tagService;

	@Autowired
	MutableClock clock;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private TechnicianRepository technicianRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private TagRepository tagRepository;

	/** Services commit for real in these tests, so the data is removed after each one. */
	@AfterEach
	void cleanDatabase() {
		this.ticketRepository.deleteAll();
		this.technicianRepository.deleteAll();
		this.categoryRepository.deleteAll();
		this.tagRepository.deleteAll();
	}

	TicketResponse newTicket() {
		return newTicket(TicketPriority.MEDIUM);
	}

	TicketResponse newTicket(TicketPriority priority) {
		return this.ticketService
			.create(new TicketRequest("Network down", "No network in room 12", "Demo Requester",
					"requester@example.com", priority));
	}

	TechnicianResponse newTechnician(String email) {
		return this.technicianService.create(new TechnicianRequest("Demo", "Technician", email));
	}

	CategoryResponse newCategory(String name) {
		return this.categoryService.create(new CategoryRequest(name, null));
	}

	TagResponse newTag(String name) {
		return this.tagService.create(new TagRequest(name));
	}

	/** Moves a new ticket to the given status through the real transitions. */
	TicketResponse ticketIn(TicketStatus status) {
		TicketResponse ticket = newTicket();
		if (status == TicketStatus.OPEN) {
			return ticket;
		}
		TechnicianResponse technician = newTechnician("tech" + System.nanoTime() + "@example.com");
		this.ticketService.assignTechnician(ticket.id(), technician.id());
		ticket = this.ticketService.changeStatus(ticket.id(), new TicketStatusRequest(TicketStatus.IN_PROGRESS, null));
		if (status != TicketStatus.IN_PROGRESS) {
			ticket = this.ticketService.changeStatus(ticket.id(), new TicketStatusRequest(TicketStatus.RESOLVED, "Fixed"));
		}
		if (status == TicketStatus.CLOSED) {
			ticket = this.ticketService.changeStatus(ticket.id(), new TicketStatusRequest(TicketStatus.CLOSED, null));
		}
		return ticket;
	}

	/** Clock that tests can move, to check deadlines without waiting. */
	static class MutableClock extends Clock {

		private Instant instant = Instant.now();

		void set(LocalDateTime dateTime) {
			this.instant = dateTime.atZone(getZone()).toInstant();
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.systemDefault();
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return Clock.fixed(this.instant, zone);
		}

		@Override
		public Instant instant() {
			return this.instant;
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestClockConfig {

		@Bean
		MutableClock clock() {
			return new MutableClock();
		}

	}

}
