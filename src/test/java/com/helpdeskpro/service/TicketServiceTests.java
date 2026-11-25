package com.helpdeskpro.service;

import java.time.LocalDateTime;
import java.util.List;

import com.helpdeskpro.dto.TagResponse;
import com.helpdeskpro.dto.TechnicianResponse;
import com.helpdeskpro.dto.TicketRequest;
import com.helpdeskpro.dto.TicketResponse;
import com.helpdeskpro.dto.TicketSearchCriteria;
import com.helpdeskpro.dto.TicketStatusRequest;
import com.helpdeskpro.entity.TicketPriority;
import com.helpdeskpro.entity.TicketStatus;
import com.helpdeskpro.exception.BusinessRuleException;
import com.helpdeskpro.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ServiceTest
class TicketServiceTests extends AbstractServiceTests {

	// --- CRUD, validation, normalization ---

	@Test
	void newTicketIsOpenWithMediumPriorityByDefaultAndInputIsNormalized() {
		TicketResponse ticket = this.ticketService.create(new TicketRequest("  Network   down ",
				"  Room 12\nno link  ", " Demo   Requester ", "  Requester@Example.COM ", null));

		assertThat(ticket.status()).isEqualTo(TicketStatus.OPEN);
		assertThat(ticket.priority()).isEqualTo(TicketPriority.MEDIUM);
		assertThat(ticket.title()).isEqualTo("Network down");
		assertThat(ticket.description()).isEqualTo("Room 12\nno link");
		assertThat(ticket.requesterName()).isEqualTo("Demo Requester");
		assertThat(ticket.requesterEmail()).isEqualTo("requester@example.com");
		assertThat(ticket.createdAt()).isNotNull();
		assertThat(ticket.technicianId()).isNull();
		assertThat(ticket.tags()).isEmpty();
	}

	@Test
	void invalidRequestIsRejected() {
		assertThatExceptionOfType(ConstraintViolationException.class)
			.isThrownBy(() -> this.ticketService.create(new TicketRequest(" ", "d", "n", "not-an-email", null)))
			.satisfies((ex) -> assertThat(ex.getConstraintViolations()).hasSize(2));
	}

	@Test
	void updateChangesDescriptiveFieldsAndKeepsStatusAndPriorityWhenNotGiven() {
		TicketResponse created = newTicket(TicketPriority.HIGH);

		TicketResponse updated = this.ticketService.update(created.id(),
				new TicketRequest("New title", "New description", "Other Requester", "other@example.com", null));

		assertThat(updated.title()).isEqualTo("New title");
		assertThat(updated.requesterEmail()).isEqualTo("other@example.com");
		assertThat(updated.priority()).isEqualTo(TicketPriority.HIGH);
		assertThat(updated.status()).isEqualTo(TicketStatus.OPEN);
		assertThat(updated.createdAt()).isEqualTo(created.createdAt());
	}

	@Test
	void unknownIdentifiersAreReported() {
		long ticket = newTicket().id();
		assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.ticketService.findById(999L))
			.withMessageContaining("999");
		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> this.ticketService.assignTechnician(ticket, 999L));
		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> this.ticketService.assignCategory(ticket, 999L));
		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> this.ticketService.addTag(ticket, 999L));
		assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.ticketService.delete(999L));
	}

	@Test
	void searchIsCaseInsensitiveAndCombinesFilters() {
		TicketResponse network = this.ticketService.create(new TicketRequest("Network down", "Cable issue",
				"Alice Martin", "alice@example.com", TicketPriority.HIGH));
		this.ticketService.create(new TicketRequest("Printer jam", "Paper stuck", "Bob", "bob@example.com", null));
		long category = newCategory("Hardware").id();
		long tag = newTag("urgent").id();
		this.ticketService.assignCategory(network.id(), category);
		this.ticketService.addTag(network.id(), tag);

		assertThat(this.ticketService.findAll()).hasSize(2);
		assertThat(titles(new TicketSearchCriteria("NETWORK", null, null, null, null, null)))
			.containsExactly("Network down");
		assertThat(titles(new TicketSearchCriteria("alice@", null, null, null, null, null)))
			.containsExactly("Network down");
		assertThat(titles(new TicketSearchCriteria("PAPER", null, null, null, null, null)))
			.containsExactly("Printer jam");
		assertThat(titles(new TicketSearchCriteria(null, null, TicketPriority.HIGH, null, null, null)))
			.containsExactly("Network down");
		assertThat(titles(new TicketSearchCriteria(null, TicketStatus.OPEN, null, null, category, null)))
			.containsExactly("Network down");
		assertThat(titles(new TicketSearchCriteria(null, null, null, null, null, tag)))
			.containsExactly("Network down");
		assertThat(titles(new TicketSearchCriteria("printer", null, TicketPriority.HIGH, null, null, null))).isEmpty();
		assertThat(titles(new TicketSearchCriteria("100%", null, null, null, null, null))).isEmpty();
	}

	@Test
	void searchByTechnician() {
		TicketResponse ticket = newTicket();
		newTicket();
		long technician = newTechnician("tech@example.com").id();
		this.ticketService.assignTechnician(ticket.id(), technician);

		assertThat(this.ticketService.search(new TicketSearchCriteria(null, null, null, technician, null, null)))
			.extracting(TicketResponse::id)
			.containsExactly(ticket.id());
	}

	private List<String> titles(TicketSearchCriteria criteria) {
		return this.ticketService.search(criteria).stream().map(TicketResponse::title).toList();
	}

	// --- Status transitions ---

	@Test
	void allowedTransitionsFollowTheLifeCycleAndSetResolvedAt() {
		LocalDateTime now = LocalDateTime.of(2026, 11, 25, 10, 0);
		TicketResponse ticket = newTicket();
		long technician = newTechnician("tech@example.com").id();
		this.ticketService.assignTechnician(ticket.id(), technician);

		TicketResponse started = this.ticketService.changeStatus(ticket.id(),
				new TicketStatusRequest(TicketStatus.IN_PROGRESS, null));
		assertThat(started.status()).isEqualTo(TicketStatus.IN_PROGRESS);
		assertThat(started.resolvedAt()).isNull();

		this.clock.set(now);
		TicketResponse resolved = this.ticketService.changeStatus(ticket.id(),
				new TicketStatusRequest(TicketStatus.RESOLVED, "  Cable replaced "));
		assertThat(resolved.status()).isEqualTo(TicketStatus.RESOLVED);
		assertThat(resolved.resolution()).isEqualTo("Cable replaced");
		assertThat(resolved.resolvedAt()).isEqualTo(now);

		TicketResponse closed = this.ticketService.changeStatus(ticket.id(),
				new TicketStatusRequest(TicketStatus.CLOSED, null));
		assertThat(closed.status()).isEqualTo(TicketStatus.CLOSED);
		assertThat(closed.resolvedAt()).isEqualTo(now);
	}

	@Test
	void forbiddenTransitionsAreRejected() {
		TicketResponse open = newTicket();
		for (TicketStatus target : List.of(TicketStatus.OPEN, TicketStatus.RESOLVED, TicketStatus.CLOSED)) {
			assertThatExceptionOfType(BusinessRuleException.class)
				.isThrownBy(() -> this.ticketService.changeStatus(open.id(), new TicketStatusRequest(target, "Fixed")))
				.withMessageContaining("Transition interdite");
		}

		TicketResponse inProgress = ticketIn(TicketStatus.IN_PROGRESS);
		for (TicketStatus target : List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.CLOSED)) {
			assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(
					() -> this.ticketService.changeStatus(inProgress.id(), new TicketStatusRequest(target, "Fixed")));
		}

		TicketResponse resolved = ticketIn(TicketStatus.RESOLVED);
		for (TicketStatus target : List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED)) {
			assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(
					() -> this.ticketService.changeStatus(resolved.id(), new TicketStatusRequest(target, "Fixed")));
		}
		assertThat(this.ticketService.findById(open.id()).status()).isEqualTo(TicketStatus.OPEN);
	}

	@Test
	void startingNeedsAnActiveTechnician() {
		TicketResponse withoutTechnician = newTicket();
		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> this.ticketService.changeStatus(withoutTechnician.id(),
					new TicketStatusRequest(TicketStatus.IN_PROGRESS, null)))
			.withMessageContaining("technicien actif");

		TicketResponse ticket = newTicket();
		TechnicianResponse technician = newTechnician("tech@example.com");
		this.ticketService.assignTechnician(ticket.id(), technician.id());
		this.technicianService.setActive(technician.id(), false);
		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(() -> this.ticketService
			.changeStatus(ticket.id(), new TicketStatusRequest(TicketStatus.IN_PROGRESS, null)));
	}

	@Test
	void resolvingNeedsANonBlankResolution() {
		TicketResponse ticket = ticketIn(TicketStatus.IN_PROGRESS);
		for (String resolution : new String[] { null, "", "   " }) {
			assertThatExceptionOfType(BusinessRuleException.class)
				.isThrownBy(() -> this.ticketService.changeStatus(ticket.id(),
						new TicketStatusRequest(TicketStatus.RESOLVED, resolution)))
				.withMessageContaining("solution");
		}
		TicketResponse unchanged = this.ticketService.findById(ticket.id());
		assertThat(unchanged.status()).isEqualTo(TicketStatus.IN_PROGRESS);
		assertThat(unchanged.resolvedAt()).isNull();
		assertThat(unchanged.resolution()).isNull();
	}

	@Test
	void closedTicketCannotBeModifiedByAnyOperation() {
		TicketResponse closed = ticketIn(TicketStatus.CLOSED);
		long id = closed.id();
		long category = newCategory("Hardware").id();
		long tag = newTag("urgent").id();
		long other = newTechnician("other@example.com").id();
		TicketRequest request = new TicketRequest("Title", "Description", "Requester", "r@example.com", null);

		List<Runnable> operations = List.of(() -> this.ticketService.update(id, request),
				() -> this.ticketService.changeStatus(id, new TicketStatusRequest(TicketStatus.RESOLVED, "x")),
				() -> this.ticketService.assignCategory(id, category), () -> this.ticketService.removeCategory(id),
				() -> this.ticketService.addTag(id, tag), () -> this.ticketService.removeTag(id, tag),
				() -> this.ticketService.assignTechnician(id, other), () -> this.ticketService.unassignTechnician(id));
		operations.forEach((operation) -> assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(operation::run)
			.withMessageContaining("clôturé"));
		assertThat(this.ticketService.findById(id)).isEqualTo(closed);
	}

	// --- Technician relationship ---

	@Test
	void technicianCanBeAssignedReassignedAndUnassignedWhileOpen() {
		TicketResponse ticket = newTicket();
		TechnicianResponse first = newTechnician("first@example.com");
		TechnicianResponse second = newTechnician("second@example.com");

		assertThat(this.ticketService.assignTechnician(ticket.id(), first.id()).technicianId()).isEqualTo(first.id());
		TicketResponse reassigned = this.ticketService.assignTechnician(ticket.id(), second.id());
		assertThat(reassigned.technicianId()).isEqualTo(second.id());
		assertThat(reassigned.technicianName()).isEqualTo("Demo Technician");
		assertThat(this.ticketService.search(new TicketSearchCriteria(null, null, null, first.id(), null, null)))
			.isEmpty();

		assertThat(this.ticketService.unassignTechnician(ticket.id()).technicianId()).isNull();
		assertThat(this.technicianService.findById(second.id())).isNotNull();
	}

	@Test
	void inactiveTechnicianCannotReceiveAnAssignment() {
		TicketResponse ticket = newTicket();
		TechnicianResponse technician = newTechnician("tech@example.com");
		this.technicianService.setActive(technician.id(), false);

		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> this.ticketService.assignTechnician(ticket.id(), technician.id()))
			.withMessageContaining("inactif");
		assertThat(this.ticketService.findById(ticket.id()).technicianId()).isNull();
	}

	@Test
	void unassignmentIsOnlyAllowedForAnOpenTicket() {
		TicketResponse inProgress = ticketIn(TicketStatus.IN_PROGRESS);
		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> this.ticketService.unassignTechnician(inProgress.id()))
			.withMessageContaining("OPEN");
		TicketResponse resolved = ticketIn(TicketStatus.RESOLVED);
		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> this.ticketService.unassignTechnician(resolved.id()));
		assertThat(this.ticketService.findById(inProgress.id()).technicianId()).isNotNull();
	}

	@Test
	void reassignmentIsRefusedOnceResolved() {
		TicketResponse resolved = ticketIn(TicketStatus.RESOLVED);
		TechnicianResponse other = newTechnician("other@example.com");
		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> this.ticketService.assignTechnician(resolved.id(), other.id()));
	}

	@Test
	void inProgressTicketCanBeReassignedToAnotherActiveTechnician() {
		TicketResponse ticket = ticketIn(TicketStatus.IN_PROGRESS);
		TechnicianResponse other = newTechnician("other@example.com");
		assertThat(this.ticketService.assignTechnician(ticket.id(), other.id()).technicianId()).isEqualTo(other.id());
	}

	@Test
	void technicianWithTicketsInProgressCannotBeDeactivatedNorDeleted() {
		TicketResponse ticket = ticketIn(TicketStatus.IN_PROGRESS);
		long technician = ticket.technicianId();

		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> this.technicianService.setActive(technician, false))
			.withMessageContaining("tickets en cours");
		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> this.technicianService.delete(technician))
			.withMessageContaining("1 ticket");
		assertThat(this.technicianService.findById(technician).active()).isTrue();
	}

	@Test
	void technicianWithOnlyOpenTicketsCanBeDeactivated() {
		TicketResponse ticket = newTicket();
		long technician = newTechnician("tech@example.com").id();
		this.ticketService.assignTechnician(ticket.id(), technician);

		assertThat(this.technicianService.setActive(technician, false).active()).isFalse();
	}

	// --- Category and tags ---

	@Test
	void categoryCanBeAssignedChangedAndRemoved() {
		TicketResponse ticket = newTicket();
		long hardware = newCategory("Hardware").id();
		long software = newCategory("Software").id();

		TicketResponse assigned = this.ticketService.assignCategory(ticket.id(), hardware);
		assertThat(assigned.categoryId()).isEqualTo(hardware);
		assertThat(assigned.categoryName()).isEqualTo("Hardware");
		assertThat(this.ticketService.assignCategory(ticket.id(), software).categoryName()).isEqualTo("Software");
		assertThat(this.ticketService.search(new TicketSearchCriteria(null, null, null, null, hardware, null)))
			.isEmpty();
		assertThat(this.ticketService.removeCategory(ticket.id()).categoryId()).isNull();
		assertThat(this.categoryService.findAll()).hasSize(2);
	}

	@Test
	void tagsCanBeAddedAndRemovedWithoutDeletingThem() {
		TicketResponse ticket = newTicket();
		long urgent = newTag("urgent").id();
		long network = newTag("Network").id();

		this.ticketService.addTag(ticket.id(), urgent);
		TicketResponse tagged = this.ticketService.addTag(ticket.id(), network);
		this.ticketService.addTag(ticket.id(), network);
		assertThat(tagged.tags()).extracting(TagResponse::name).containsExactly("Network", "urgent");
		assertThat(this.ticketService.findById(ticket.id()).tags()).hasSize(2);

		TicketResponse untagged = this.ticketService.removeTag(ticket.id(), urgent);
		assertThat(untagged.tags()).extracting(TagResponse::name).containsExactly("Network");
		assertThat(this.tagService.findAll()).hasSize(2);
	}

	@Test
	void categoryAndTagCannotBeDeletedWhileUsedButCanOnceReleased() {
		TicketResponse ticket = newTicket();
		long category = newCategory("Hardware").id();
		long tag = newTag("urgent").id();
		this.ticketService.assignCategory(ticket.id(), category);
		this.ticketService.addTag(ticket.id(), tag);

		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(() -> this.categoryService.delete(category))
			.withMessageContaining("Hardware")
			.withMessageContaining("1 ticket");
		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(() -> this.tagService.delete(tag))
			.withMessageContaining("urgent")
			.withMessageContaining("1 ticket");

		this.ticketService.removeCategory(ticket.id());
		this.ticketService.removeTag(ticket.id(), tag);
		this.categoryService.delete(category);
		this.tagService.delete(tag);
		assertThat(this.categoryService.findAll()).isEmpty();
		assertThat(this.tagService.findAll()).isEmpty();
	}

	// --- Ticket deletion ---

	@Test
	void deleteRemovesTheTicketOnlyAndReleasesItsRelationships() {
		TicketResponse ticket = newTicket();
		long technician = newTechnician("tech@example.com").id();
		long category = newCategory("Hardware").id();
		long tag = newTag("urgent").id();
		this.ticketService.assignTechnician(ticket.id(), technician);
		this.ticketService.assignCategory(ticket.id(), category);
		this.ticketService.addTag(ticket.id(), tag);

		this.ticketService.delete(ticket.id());

		assertThat(this.ticketService.findAll()).isEmpty();
		assertThat(this.technicianService.findById(technician)).isNotNull();
		assertThat(this.categoryService.findById(category)).isNotNull();
		assertThat(this.tagService.findById(tag)).isNotNull();
		// Nothing references them any more, so they can now be deleted
		this.categoryService.delete(category);
		this.tagService.delete(tag);
		this.technicianService.delete(technician);
	}

	@Test
	void closedTicketCanBeDeleted() {
		TicketResponse closed = ticketIn(TicketStatus.CLOSED);
		this.ticketService.delete(closed.id());
		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> this.ticketService.findById(closed.id()));
	}

}
