package com.helpdeskpro.repository;

import com.helpdeskpro.entity.Category;
import com.helpdeskpro.entity.Tag;
import com.helpdeskpro.entity.Technician;
import com.helpdeskpro.entity.TechnicianProfile;
import com.helpdeskpro.entity.Ticket;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@DataJpaTest
class EntityRelationshipTests {

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

	// Technician <-> TechnicianProfile (OneToOne)

	@Test
	void oneToOneIsNavigableFromBothSides() {
		Technician technician = persistTechnicianWithProfile();
		Long profileId = technician.getProfile().getId();
		flushAndClear();

		Technician foundTechnician = this.technicianRepository.findById(technician.getId()).orElseThrow();
		assertThat(foundTechnician.getProfile().getId()).isEqualTo(profileId);
		TechnicianProfile foundProfile = this.technicianProfileRepository.findById(profileId).orElseThrow();
		assertThat(foundProfile.getTechnician()).isEqualTo(foundTechnician);
	}

	@Test
	void removingProfileDeletesItAndKeepsTheTechnician() {
		Technician technician = persistTechnicianWithProfile();
		flushAndClear();

		Technician managed = this.technicianRepository.findById(technician.getId()).orElseThrow();
		managed.removeProfile();
		flushAndClear();

		assertThat(this.technicianProfileRepository.count()).isZero();
		assertThat(this.technicianRepository.findById(technician.getId()).orElseThrow().getProfile()).isNull();
	}

	@Test
	void profileCanBeReplacedOnceThePreviousOneIsRemoved() {
		Technician technician = persistTechnicianWithProfile();
		technician.removeProfile();
		this.entityManager.flush();
		technician.assignProfile(new TechnicianProfile("0200000000", "Security", null));
		flushAndClear();

		assertThat(this.technicianRepository.findById(technician.getId()).orElseThrow().getProfile()
			.getSpecialization()).isEqualTo("Security");
		assertThat(this.technicianProfileRepository.count()).isOne();
	}

	@Test
	void deletingTechnicianDeletesItsProfile() {
		Technician technician = persistTechnicianWithProfile();
		flushAndClear();

		this.technicianRepository.deleteById(technician.getId());
		flushAndClear();

		assertThat(this.technicianRepository.count()).isZero();
		assertThat(this.technicianProfileRepository.count()).isZero();
	}

	@Test
	void technicianCannotReceiveASecondProfile() {
		Technician technician = persistTechnicianWithProfile();

		assertThatIllegalStateException()
			.isThrownBy(() -> technician.assignProfile(new TechnicianProfile(null, "Security", null)));
	}

	@Test
	void databaseRejectsTwoProfilesForTheSameTechnician() {
		Technician technician = persistTechnicianWithProfile();
		flushAndClear();

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> this.jdbcTemplate.update("insert into technician_profile (technician_id) values (?)",
					technician.getId()))
			.withStackTraceContaining("ON PUBLIC.TECHNICIAN_PROFILE(TECHNICIAN_ID");
	}

	@Test
	void databaseRejectsProfileWithoutTechnician() {
		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> this.jdbcTemplate.update("insert into technician_profile (phone) values ('0100000000')"))
			.withStackTraceContaining("TECHNICIAN_ID");
	}

	// Technician <-> Ticket (OneToMany / ManyToOne)

	@Test
	void technicianHasManyTickets() {
		Technician technician = this.technicianRepository.save(newTechnician("tech@example.com"));
		Ticket first = this.ticketRepository.save(newTicket("First"));
		Ticket second = this.ticketRepository.save(newTicket("Second"));
		technician.addTicket(first);
		second.assignTechnician(technician);
		flushAndClear();

		Technician found = this.technicianRepository.findById(technician.getId()).orElseThrow();
		assertThat(found.getTickets()).containsExactlyInAnyOrder(first, second);
		assertThat(this.ticketRepository.findById(first.getId()).orElseThrow().getTechnician()).isEqualTo(found);
	}

	@Test
	void ticketCanBeUnassignedFromItsTechnician() {
		Technician technician = this.technicianRepository.save(newTechnician("tech@example.com"));
		Ticket ticket = this.ticketRepository.save(newTicket("First"));
		technician.addTicket(ticket);
		flushAndClear();

		this.technicianRepository.findById(technician.getId())
			.orElseThrow()
			.removeTicket(this.ticketRepository.findById(ticket.getId()).orElseThrow());
		flushAndClear();

		assertThat(this.ticketRepository.findById(ticket.getId()).orElseThrow().getTechnician()).isNull();
		assertThat(this.technicianRepository.findById(technician.getId()).orElseThrow().getTickets()).isEmpty();
	}

	@Test
	void reassigningTicketUpdatesBothTechnicians() {
		Technician previous = this.technicianRepository.save(newTechnician("previous@example.com"));
		Technician next = this.technicianRepository.save(newTechnician("next@example.com"));
		Ticket ticket = this.ticketRepository.save(newTicket("First"));
		ticket.assignTechnician(previous);
		ticket.assignTechnician(next);

		assertThat(previous.getTickets()).isEmpty();
		assertThat(next.getTickets()).containsExactly(ticket);
		previous.removeTicket(ticket);
		assertThat(ticket.getTechnician()).isEqualTo(next);
	}

	@Test
	void synchronizationWorksThroughLazyProxies() {
		Technician technician = this.technicianRepository.save(newTechnician("tech@example.com"));
		Ticket ticket = this.ticketRepository.save(newTicket("First"));
		ticket.assignTechnician(technician);
		flushAndClear();

		Ticket managed = this.ticketRepository.findById(ticket.getId()).orElseThrow();
		Technician proxy = managed.getTechnician();
		assertThat(Hibernate.isInitialized(proxy)).isFalse();
		managed.assignTechnician(null);
		assertThat(proxy.getTickets()).isEmpty();
		proxy.addTicket(managed);
		flushAndClear();

		assertThat(this.technicianRepository.findById(technician.getId()).orElseThrow().getTickets())
			.containsExactly(ticket);
	}

	@Test
	void technicianStillAssignedToTicketsCannotBeDeleted() {
		Technician technician = this.technicianRepository.save(newTechnician("tech@example.com"));
		technician.addTicket(this.ticketRepository.save(newTicket("First")));
		flushAndClear();

		assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() -> {
			this.technicianRepository.deleteById(technician.getId());
			this.technicianRepository.flush();
		}).withStackTraceContaining("FK_TICKET_TECHNICIAN");
	}

	// Category <-> Ticket (OneToMany / ManyToOne)

	@Test
	void categoryHasManyTickets() {
		Category category = this.categoryRepository.save(new Category("Hardware", null));
		Ticket first = this.ticketRepository.save(newTicket("First"));
		Ticket second = this.ticketRepository.save(newTicket("Second"));
		category.addTicket(first);
		second.assignCategory(category);
		flushAndClear();

		Category found = this.categoryRepository.findById(category.getId()).orElseThrow();
		assertThat(found.getTickets()).containsExactlyInAnyOrder(first, second);
		assertThat(this.ticketRepository.findById(second.getId()).orElseThrow().getCategory()).isEqualTo(found);
	}

	@Test
	void categoryCanBeDeletedOnceItsTicketsAreUnlinked() {
		Category category = this.categoryRepository.save(new Category("Hardware", null));
		Ticket ticket = this.ticketRepository.save(newTicket("First"));
		category.addTicket(ticket);
		flushAndClear();

		Category managed = this.categoryRepository.findById(category.getId()).orElseThrow();
		managed.removeTicket(this.ticketRepository.findById(ticket.getId()).orElseThrow());
		this.categoryRepository.delete(managed);
		flushAndClear();

		assertThat(this.categoryRepository.count()).isZero();
		assertThat(this.ticketRepository.findById(ticket.getId()).orElseThrow().getCategory()).isNull();
	}

	// Ticket <-> Tag (ManyToMany)

	@Test
	void ticketsAndTagsAreLinkedInBothDirections() {
		Tag network = this.tagRepository.save(new Tag("network"));
		Tag urgent = this.tagRepository.save(new Tag("urgent"));
		Ticket first = this.ticketRepository.save(newTicket("First"));
		Ticket second = this.ticketRepository.save(newTicket("Second"));
		first.addTag(network);
		first.addTag(urgent);
		second.addTag(urgent);
		flushAndClear();

		assertThat(this.ticketRepository.findById(first.getId()).orElseThrow().getTags())
			.containsExactlyInAnyOrder(network, urgent);
		assertThat(this.tagRepository.findById(urgent.getId()).orElseThrow().getTickets())
			.containsExactlyInAnyOrder(first, second);
	}

	@Test
	void tagCanBeUnlinkedFromTicketWithoutBeingDeleted() {
		Tag tag = this.tagRepository.save(new Tag("urgent"));
		Ticket ticket = this.ticketRepository.save(newTicket("First"));
		ticket.addTag(tag);
		flushAndClear();

		this.ticketRepository.findById(ticket.getId())
			.orElseThrow()
			.removeTag(this.tagRepository.findById(tag.getId()).orElseThrow());
		flushAndClear();

		assertThat(this.ticketRepository.findById(ticket.getId()).orElseThrow().getTags()).isEmpty();
		assertThat(this.tagRepository.findById(tag.getId()).orElseThrow().getTickets()).isEmpty();
		assertThat(countTicketTagRows()).isZero();
	}

	@Test
	void linkingTheSameTagTwiceCreatesASingleRow() {
		Tag tag = this.tagRepository.save(new Tag("urgent"));
		Ticket ticket = this.ticketRepository.save(newTicket("First"));
		ticket.addTag(tag);
		ticket.addTag(tag);
		flushAndClear();

		assertThat(countTicketTagRows()).isOne();
	}

	@Test
	void deletingTicketKeepsItsTagsTechnicianAndCategory() {
		Tag tag = this.tagRepository.save(new Tag("urgent"));
		Category category = this.categoryRepository.save(new Category("Hardware", null));
		Technician technician = this.technicianRepository.save(newTechnician("tech@example.com"));
		Ticket ticket = this.ticketRepository.save(newTicket("First"));
		ticket.addTag(tag);
		ticket.assignCategory(category);
		ticket.assignTechnician(technician);
		flushAndClear();

		this.ticketRepository.deleteById(ticket.getId());
		flushAndClear();

		assertThat(this.ticketRepository.count()).isZero();
		assertThat(countTicketTagRows()).isZero();
		assertThat(this.tagRepository.count()).isOne();
		assertThat(this.categoryRepository.count()).isOne();
		assertThat(this.technicianRepository.count()).isOne();
	}

	// equals, hashCode and toString

	@Test
	void objectMethodsDoNotRecurseThroughRelationships() {
		Technician technician = persistTechnicianWithProfile();
		Category category = this.categoryRepository.save(new Category("Hardware", null));
		Tag tag = this.tagRepository.save(new Tag("urgent"));
		Ticket ticket = this.ticketRepository.save(newTicket("First"));
		ticket.assignTechnician(technician);
		ticket.assignCategory(category);
		ticket.addTag(tag);

		for (Object entity : new Object[] { ticket, technician, technician.getProfile(), category, tag }) {
			assertThat(entity.toString()).startsWith(entity.getClass().getSimpleName() + "{id=");
			assertThat(entity).isEqualTo(entity).hasSameHashCodeAs(entity).isNotEqualTo(new Object());
		}
		assertThat(newTicket("First")).isNotEqualTo(newTicket("First"));
	}

	private Technician persistTechnicianWithProfile() {
		Technician technician = newTechnician("profile@example.com");
		technician.assignProfile(new TechnicianProfile("0100000000", "Network", "Demo biography"));
		return this.technicianRepository.saveAndFlush(technician);
	}

	private int countTicketTagRows() {
		return this.jdbcTemplate.queryForObject("select count(*) from ticket_tag", Integer.class);
	}

	private void flushAndClear() {
		this.entityManager.flush();
		this.entityManager.clear();
	}

	private static Ticket newTicket(String title) {
		return new Ticket(title, "Demo description", "Demo Requester", "requester@example.com");
	}

	private static Technician newTechnician(String email) {
		return new Technician("Demo", "Technician", email);
	}

}
