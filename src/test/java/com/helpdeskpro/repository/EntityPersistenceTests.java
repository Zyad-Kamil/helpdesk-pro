package com.helpdeskpro.repository;

import java.time.LocalDateTime;

import com.helpdeskpro.entity.Category;
import com.helpdeskpro.entity.Tag;
import com.helpdeskpro.entity.Technician;
import com.helpdeskpro.entity.TechnicianProfile;
import com.helpdeskpro.entity.Ticket;
import com.helpdeskpro.entity.TicketPriority;
import com.helpdeskpro.entity.TicketStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJpaTest
class EntityPersistenceTests {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private TechnicianRepository technicianRepository;

	@Autowired
	private TechnicianProfileRepository technicianProfileRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private TagRepository tagRepository;

	@Test
	void modelContainsExactlyTheFiveExpectedEntities() {
		assertThat(this.entityManager.getMetamodel().getEntities()).extracting(EntityType::getName)
			.containsExactlyInAnyOrder("Ticket", "Technician", "TechnicianProfile", "Category", "Tag");
	}

	@Test
	void persistsTicketWithAllItsAttributes() {
		Ticket ticket = newTicket();
		ticket.setStatus(TicketStatus.RESOLVED);
		ticket.setPriority(TicketPriority.CRITICAL);
		LocalDateTime resolvedAt = LocalDateTime.of(2026, 11, 25, 10, 30);
		ticket.setResolvedAt(resolvedAt);
		ticket.setResolution("Cable replaced");
		this.ticketRepository.saveAndFlush(ticket);
		this.entityManager.clear();

		Ticket found = this.ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(found).isNotSameAs(ticket);
		assertThat(found.getTitle()).isEqualTo("Network down");
		assertThat(found.getDescription()).isEqualTo("No network in room 12");
		assertThat(found.getRequesterName()).isEqualTo("Demo Requester");
		assertThat(found.getRequesterEmail()).isEqualTo("requester@example.com");
		assertThat(found.getStatus()).isEqualTo(TicketStatus.RESOLVED);
		assertThat(found.getPriority()).isEqualTo(TicketPriority.CRITICAL);
		assertThat(found.getCreatedAt()).isEqualTo(ticket.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isEqualTo(found.getCreatedAt());
		assertThat(found.getResolvedAt()).isEqualTo(resolvedAt);
		assertThat(found.getResolution()).isEqualTo("Cable replaced");
	}

	@Test
	void newTicketIsOpenWithMediumPriorityAndNoRelationship() {
		Ticket ticket = this.ticketRepository.saveAndFlush(newTicket());
		this.entityManager.clear();

		Ticket found = this.ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(found.getStatus()).isEqualTo(TicketStatus.OPEN);
		assertThat(found.getPriority()).isEqualTo(TicketPriority.MEDIUM);
		assertThat(found.getTechnician()).isNull();
		assertThat(found.getCategory()).isNull();
		assertThat(found.getTags()).isEmpty();
		assertThat(found.getResolvedAt()).isNull();
	}

	@Test
	void updatingTicketChangesUpdatedAtButNotCreatedAt() throws InterruptedException {
		Ticket ticket = this.ticketRepository.saveAndFlush(newTicket());
		LocalDateTime createdAt = ticket.getCreatedAt();
		Thread.sleep(20);
		ticket.setTitle("Network still down");
		this.ticketRepository.saveAndFlush(ticket);
		this.entityManager.clear();

		Ticket found = this.ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(found.getCreatedAt()).isEqualTo(createdAt);
		assertThat(found.getUpdatedAt()).isAfter(createdAt);
	}

	@Test
	void enumsAreStoredAsStringsInVarcharColumns() {
		Ticket ticket = newTicket();
		ticket.setStatus(TicketStatus.IN_PROGRESS);
		ticket.setPriority(TicketPriority.HIGH);
		this.ticketRepository.saveAndFlush(ticket);

		assertThat(this.jdbcTemplate.queryForMap("select status, priority from ticket where id = ?", ticket.getId()))
			.containsEntry("STATUS", "IN_PROGRESS")
			.containsEntry("PRIORITY", "HIGH");
		assertThat(this.jdbcTemplate.queryForList("""
				select data_type from information_schema.columns
				where table_name = 'TICKET' and column_name in ('STATUS', 'PRIORITY')""", String.class))
			.containsExactly("CHARACTER VARYING", "CHARACTER VARYING");
	}

	@Test
	void databaseRejectsInvalidEnumValues() {
		assertThat(this.jdbcTemplate.queryForList("select check_clause from information_schema.check_constraints",
				String.class)).anyMatch((clause) -> clause.contains("OPEN"))
			.anyMatch((clause) -> clause.contains("CRITICAL"));
		Ticket ticket = this.ticketRepository.saveAndFlush(newTicket());

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> this.jdbcTemplate.update("update ticket set status = 'INVALID' where id = ?",
					ticket.getId()));
		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> this.jdbcTemplate.update("update ticket set priority = 'INVALID' where id = ?",
					ticket.getId()));
	}

	@Test
	void persistsTechnician() {
		Technician technician = newTechnician("tech@example.com");
		technician.setActive(false);
		this.technicianRepository.saveAndFlush(technician);
		this.entityManager.clear();

		Technician found = this.technicianRepository.findById(technician.getId()).orElseThrow();
		assertThat(found.getFirstName()).isEqualTo("Demo");
		assertThat(found.getLastName()).isEqualTo("Technician");
		assertThat(found.getEmail()).isEqualTo("tech@example.com");
		assertThat(found.isActive()).isFalse();
		assertThat(found.getProfile()).isNull();
	}

	@Test
	void persistsTechnicianProfile() {
		Technician technician = newTechnician("tech@example.com");
		TechnicianProfile profile = new TechnicianProfile("0100000000", "Network", "Demo biography");
		technician.assignProfile(profile);
		this.technicianRepository.saveAndFlush(technician);
		this.entityManager.clear();

		TechnicianProfile found = this.technicianProfileRepository.findById(profile.getId()).orElseThrow();
		assertThat(found.getPhone()).isEqualTo("0100000000");
		assertThat(found.getSpecialization()).isEqualTo("Network");
		assertThat(found.getBiography()).isEqualTo("Demo biography");
	}

	@Test
	void persistsCategory() {
		Category category = this.categoryRepository.saveAndFlush(new Category("Hardware", "Physical equipment"));
		this.entityManager.clear();

		Category found = this.categoryRepository.findById(category.getId()).orElseThrow();
		assertThat(found.getName()).isEqualTo("Hardware");
		assertThat(found.getDescription()).isEqualTo("Physical equipment");
	}

	@Test
	void persistsTag() {
		Tag tag = this.tagRepository.saveAndFlush(new Tag("urgent"));
		this.entityManager.clear();

		assertThat(this.tagRepository.findById(tag.getId()).orElseThrow().getName()).isEqualTo("urgent");
	}

	@Test
	void technicianEmailIsUnique() {
		this.technicianRepository.saveAndFlush(newTechnician("tech@example.com"));

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> this.technicianRepository.saveAndFlush(newTechnician("tech@example.com")))
			.withStackTraceContaining("UK_TECHNICIAN_EMAIL");
	}

	@Test
	void categoryNameIsUnique() {
		this.categoryRepository.saveAndFlush(new Category("Hardware", null));

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> this.categoryRepository.saveAndFlush(new Category("Hardware", null)))
			.withStackTraceContaining("UK_CATEGORY_NAME");
	}

	@Test
	void tagNameIsUnique() {
		this.tagRepository.saveAndFlush(new Tag("urgent"));

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> this.tagRepository.saveAndFlush(new Tag("urgent")))
			.withStackTraceContaining("UK_TAG_NAME");
	}

	@Test
	void invalidTicketIsRejectedByValidation() {
		Ticket ticket = new Ticket(" ", "Description", "Demo Requester", "not-an-email");

		assertThatExceptionOfType(ConstraintViolationException.class)
			.isThrownBy(() -> this.ticketRepository.saveAndFlush(ticket))
			.satisfies((ex) -> assertThat(ex.getConstraintViolations())
				.extracting((violation) -> violation.getPropertyPath().toString())
				.containsExactlyInAnyOrder("title", "requesterEmail"));
	}

	private static Ticket newTicket() {
		return new Ticket("Network down", "No network in room 12", "Demo Requester", "requester@example.com");
	}

	private static Technician newTechnician(String email) {
		return new Technician("Demo", "Technician", email);
	}

}
