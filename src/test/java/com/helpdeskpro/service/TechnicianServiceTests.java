package com.helpdeskpro.service;

import com.helpdeskpro.dto.TechnicianProfileRequest;
import com.helpdeskpro.dto.TechnicianProfileResponse;
import com.helpdeskpro.dto.TechnicianRequest;
import com.helpdeskpro.dto.TechnicianResponse;
import com.helpdeskpro.dto.TicketResponse;
import com.helpdeskpro.exception.BusinessRuleException;
import com.helpdeskpro.exception.DuplicateResourceException;
import com.helpdeskpro.exception.ResourceNotFoundException;
import com.helpdeskpro.repository.TechnicianProfileRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Technicians and their profiles. */
@ServiceTest
class TechnicianServiceTests extends AbstractServiceTests {

	@Autowired
	private TechnicianProfileService profileService;

	@Autowired
	private TechnicianProfileRepository profileRepository;

	@Test
	void technicianCrudNormalizesInputAndIsActiveByDefault() {
		TechnicianResponse created = this.technicianService
			.create(new TechnicianRequest("  Jean  Pierre ", " Dupont ", "  Jean.Dupont@Example.COM "));
		assertThat(created.firstName()).isEqualTo("Jean Pierre");
		assertThat(created.lastName()).isEqualTo("Dupont");
		assertThat(created.email()).isEqualTo("jean.dupont@example.com");
		assertThat(created.active()).isTrue();
		assertThat(created.profile()).isNull();

		TechnicianResponse updated = this.technicianService.update(created.id(),
				new TechnicianRequest("Jean", "Martin", "jean.martin@example.com"));
		assertThat(updated.lastName()).isEqualTo("Martin");
		assertThat(this.technicianService.findById(created.id())).isEqualTo(updated);

		this.technicianService.delete(created.id());
		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> this.technicianService.findById(created.id()));
	}

	@Test
	void invalidTechnicianIsRejected() {
		assertThatExceptionOfType(ConstraintViolationException.class)
			.isThrownBy(() -> this.technicianService.create(new TechnicianRequest("", "Dupont", "not-an-email")))
			.satisfies((ex) -> assertThat(ex.getConstraintViolations()).hasSize(2));
	}

	@Test
	void emailsAreUniqueIgnoringCase() {
		newTechnician("tech@example.com");
		TechnicianResponse other = newTechnician("other@example.com");

		assertThatExceptionOfType(DuplicateResourceException.class)
			.isThrownBy(() -> this.technicianService.create(new TechnicianRequest("A", "B", " TECH@example.com ")))
			.withMessageContaining("tech@example.com");
		assertThatExceptionOfType(DuplicateResourceException.class).isThrownBy(
				() -> this.technicianService.update(other.id(), new TechnicianRequest("A", "B", "Tech@Example.com")));
		assertThat(this.technicianService.update(other.id(), new TechnicianRequest("A", "B", "OTHER@example.com"))
			.email()).isEqualTo("other@example.com");
	}

	@Test
	void searchIsCaseInsensitiveAndCanFilterOnActivity() {
		newTechnician("alice.durand@example.com");
		this.technicianService.create(new TechnicianRequest("Bob", "Martin", "bob@example.com"));
		TechnicianResponse inactive = this.technicianService.create(new TechnicianRequest("Carl", "Durand", "carl@example.com"));
		this.technicianService.setActive(inactive.id(), false);

		assertThat(this.technicianService.findAll()).extracting(TechnicianResponse::email)
			.containsExactly("carl@example.com", "bob@example.com", "alice.durand@example.com");
		assertThat(this.technicianService.search("DURAND", null)).hasSize(2);
		assertThat(this.technicianService.search("durand", true)).extracting(TechnicianResponse::email)
			.containsExactly("alice.durand@example.com");
		assertThat(this.technicianService.search("", false)).extracting(TechnicianResponse::email)
			.containsExactly("carl@example.com");
		assertThat(this.technicianService.search("BOB@", null)).hasSize(1);
		assertThat(this.technicianService.search("zzz", null)).isEmpty();
	}

	@Test
	void technicianWithATicketCannotBeDeleted() {
		TicketResponse ticket = newTicket();
		TechnicianResponse technician = newTechnician("tech@example.com");
		this.ticketService.assignTechnician(ticket.id(), technician.id());

		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> this.technicianService.delete(technician.id()))
			.withMessageContaining("ne peut pas être supprimé")
			.withMessageContaining("1 ticket");

		this.ticketService.unassignTechnician(ticket.id());
		this.technicianService.delete(technician.id());
		assertThat(this.technicianService.findAll()).isEmpty();
	}

	@Test
	void technicianCanBeReactivated() {
		TechnicianResponse technician = newTechnician("tech@example.com");
		assertThat(this.technicianService.setActive(technician.id(), false).active()).isFalse();
		assertThat(this.technicianService.setActive(technician.id(), true).active()).isTrue();
	}

	// --- Profiles ---

	@Test
	void profileLifeCycle() {
		TechnicianResponse technician = newTechnician("tech@example.com");

		TechnicianProfileResponse created = this.profileService.create(technician.id(),
				new TechnicianProfileRequest(" 0100000000 ", "  Network ", "Demo\nbiography "));
		assertThat(created.technicianId()).isEqualTo(technician.id());
		assertThat(created.technicianName()).isEqualTo("Demo Technician");
		assertThat(created.phone()).isEqualTo("0100000000");
		assertThat(created.specialization()).isEqualTo("Network");
		assertThat(created.biography()).isEqualTo("Demo\nbiography");
		assertThat(this.technicianService.findById(technician.id()).profile()).isEqualTo(created);

		TechnicianProfileResponse updated = this.profileService.update(technician.id(),
				new TechnicianProfileRequest(null, "Systems", "  "));
		assertThat(updated.id()).isEqualTo(created.id());
		assertThat(updated.phone()).isNull();
		assertThat(updated.specialization()).isEqualTo("Systems");
		assertThat(updated.biography()).isNull();
		assertThat(this.profileService.findByTechnicianId(technician.id())).isEqualTo(updated);
		assertThat(this.profileService.findById(created.id())).isEqualTo(updated);
		assertThat(this.profileService.findAll()).containsExactly(updated);

		this.profileService.delete(technician.id());
		assertThat(this.profileRepository.count()).isZero();
		assertThat(this.technicianService.findById(technician.id()).profile()).isNull();
		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> this.profileService.findByTechnicianId(technician.id()));
		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> this.profileService.update(technician.id(), new TechnicianProfileRequest(null, null, null)));
		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> this.profileService.delete(technician.id()));
		// The technician is untouched and a new profile can be created
		this.profileService.create(technician.id(), new TechnicianProfileRequest(null, null, null));
		assertThat(this.profileRepository.count()).isOne();
	}

	@Test
	void technicianCannotHaveTwoProfiles() {
		TechnicianResponse technician = newTechnician("tech@example.com");
		this.profileService.create(technician.id(), new TechnicianProfileRequest(null, "Network", null));

		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> this.profileService.create(technician.id(), new TechnicianProfileRequest(null, null, null)))
			.withMessageContaining("déjà un profil");
		assertThat(this.profileRepository.count()).isOne();
	}

	@Test
	void profileOfUnknownTechnicianIsRejectedAndValidated() {
		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> this.profileService.create(999L, new TechnicianProfileRequest(null, null, null)));
		TechnicianResponse technician = newTechnician("tech@example.com");
		assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(
				() -> this.profileService.create(technician.id(), new TechnicianProfileRequest("1".repeat(31), null, null)));
	}

	@Test
	void profilesAreSearchedBySpecializationOrTechnician() {
		TechnicianResponse alice = this.technicianService.create(new TechnicianRequest("Alice", "Durand", "alice@example.com"));
		TechnicianResponse bob = this.technicianService.create(new TechnicianRequest("Bob", "Martin", "bob@example.com"));
		newTechnician("without.profile@example.com");
		this.profileService.create(alice.id(), new TechnicianProfileRequest(null, "Network security", null));
		this.profileService.create(bob.id(), new TechnicianProfileRequest(null, null, null));

		assertThat(this.profileService.search("SECURITY")).extracting(TechnicianProfileResponse::technicianId)
			.containsExactly(alice.id());
		assertThat(this.profileService.search("martin")).extracting(TechnicianProfileResponse::technicianId)
			.containsExactly(bob.id());
		assertThat(this.profileService.search("BOB@EXAMPLE")).hasSize(1);
		assertThat(this.profileService.search(" ")).hasSize(2);
		assertThat(this.profileService.search("without")).isEmpty();
	}

	@Test
	void deletingATechnicianDeletesItsProfile() {
		TechnicianResponse technician = newTechnician("tech@example.com");
		this.profileService.create(technician.id(), new TechnicianProfileRequest(null, "Network", null));

		this.technicianService.delete(technician.id());

		assertThat(this.profileRepository.count()).isZero();
	}

}
