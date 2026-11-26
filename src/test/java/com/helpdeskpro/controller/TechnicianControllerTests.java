package com.helpdeskpro.controller;

import com.helpdeskpro.dto.TechnicianProfileRequest;
import com.helpdeskpro.dto.TechnicianRequest;
import com.helpdeskpro.dto.TechnicianResponse;
import com.helpdeskpro.dto.TicketResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebTest
class TechnicianControllerTests extends AbstractWebTests {

	@Test
	void emptyListShowsAnEmptyState() throws Exception {
		this.mockMvc.perform(get("/technicians"))
			.andExpect(status().isOk())
			.andExpect(view().name("technicians/list"))
			.andExpect(content().string(containsString("Aucun technicien ne correspond")));
	}

	@Test
	void listSearchesAndFiltersOnActivity() throws Exception {
		newTechnician("alice@example.com");
		TechnicianResponse bob = this.technicianService.create(new TechnicianRequest("Bob", "Martin", "bob@example.com"));
		this.technicianService.setActive(bob.id(), false);

		this.mockMvc.perform(get("/technicians"))
			.andExpect(content().string(containsString("alice@example.com")))
			.andExpect(content().string(containsString("bob@example.com")));
		this.mockMvc.perform(get("/technicians").param("keyword", "MARTIN"))
			.andExpect(content().string(containsString("bob@example.com")))
			.andExpect(content().string(not(containsString("alice@example.com"))));
		this.mockMvc.perform(get("/technicians").param("active", "true"))
			.andExpect(content().string(containsString("alice@example.com")))
			.andExpect(content().string(not(containsString("bob@example.com"))));
		this.mockMvc.perform(get("/technicians").param("active", "false"))
			.andExpect(content().string(containsString("bob@example.com")))
			.andExpect(content().string(not(containsString("alice@example.com"))));
	}

	@Test
	void validCreationRedirectsToTheDetail() throws Exception {
		this.mockMvc.perform(post("/technicians").param("firstName", " Jean ")
				.param("lastName", "Dupont")
				.param("email", "Jean.Dupont@Example.com"))
			.andExpect(flash().attribute("success", containsString("a été créé")));

		TechnicianResponse created = this.technicianService.findAll().getFirst();
		assertThat(created.email()).isEqualTo("jean.dupont@example.com");
		assertThat(created.active()).isTrue();
		this.mockMvc.perform(get("/technicians/" + created.id()))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Jean Dupont")))
			.andExpect(content().string(containsString("Aucun profil renseigné")))
			.andExpect(content().string(containsString("Aucun ticket affecté")));
	}

	@Test
	void invalidCreationKeepsTheTypedValues() throws Exception {
		this.mockMvc.perform(post("/technicians").param("firstName", "")
				.param("lastName", "Typed Name")
				.param("email", "bad"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("form", "firstName", "email"))
			.andExpect(content().string(containsString("value=\"Typed Name\"")))
			.andExpect(content().string(containsString("value=\"bad\"")));
		assertThat(this.technicianService.findAll()).isEmpty();
	}

	@Test
	void duplicateEmailIsReportedOnTheFieldIgnoringCase() throws Exception {
		newTechnician("jean@example.com");

		this.mockMvc.perform(post("/technicians").param("firstName", "Other")
				.param("lastName", "Person")
				.param("email", "JEAN@example.com"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("form", "email"))
			.andExpect(content().string(containsString("existe déjà")))
			.andExpect(content().string(containsString("value=\"Other\"")));
		assertThat(this.technicianService.findAll()).hasSize(1);
	}

	@Test
	void editUpdatesTheTechnician() throws Exception {
		TechnicianResponse technician = newTechnician("jean@example.com");

		this.mockMvc.perform(get("/technicians/" + technician.id() + "/edit"))
			.andExpect(content().string(containsString("value=\"jean@example.com\"")));
		this.mockMvc.perform(post("/technicians/" + technician.id() + "/edit").param("firstName", "Jeanne")
				.param("lastName", "Durand")
				.param("email", "jeanne@example.com"))
			.andExpect(redirectedUrl("/technicians/" + technician.id()));
		assertThat(this.technicianService.findById(technician.id()).firstName()).isEqualTo("Jeanne");

		this.mockMvc.perform(post("/technicians/" + technician.id() + "/edit").param("firstName", "")
				.param("lastName", "Kept")
				.param("email", "jeanne@example.com"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("value=\"Kept\"")));
	}

	@Test
	void technicianCanBeDeactivatedAndReactivated() throws Exception {
		TechnicianResponse technician = newTechnician("jean@example.com");
		String base = "/technicians/" + technician.id();

		this.mockMvc.perform(post(base + "/active").param("active", "false"))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("success", containsString("désactivé")));
		assertThat(this.technicianService.findById(technician.id()).active()).isFalse();
		this.mockMvc.perform(get(base))
			.andExpect(content().string(containsString("Inactif")))
			.andExpect(content().string(containsString("ne peut pas recevoir de nouvelle affectation")));

		this.mockMvc.perform(post(base + "/active").param("active", "true"))
			.andExpect(flash().attribute("success", containsString("activé")));
		assertThat(this.technicianService.findById(technician.id()).active()).isTrue();
	}

	@Test
	void deactivationIsRefusedWhileTicketsAreInProgress() throws Exception {
		TicketResponse ticket = inProgressTicket("Busy");
		String base = "/technicians/" + ticket.technicianId();

		this.mockMvc.perform(postFrom(base, base + "/active").param("active", "false"))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("error", containsString("tickets en cours")));
		assertThat(this.technicianService.findById(ticket.technicianId()).active()).isTrue();
	}

	@Test
	void deletionIsRefusedWhileTicketsAreAssignedThenAllowed() throws Exception {
		TicketResponse ticket = newTicket("Assigned");
		TechnicianResponse technician = newTechnician("jean@example.com");
		this.ticketService.assignTechnician(ticket.id(), technician.id());
		String base = "/technicians/" + technician.id();

		this.mockMvc.perform(postFrom(base, base + "/delete"))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("error", containsString("ne peut pas être supprimé")))
			.andExpect(flash().attribute("error", containsString("1 ticket")));
		assertThat(this.technicianService.findAll()).hasSize(1);

		this.ticketService.unassignTechnician(ticket.id());
		this.mockMvc.perform(post(base + "/delete"))
			.andExpect(redirectedUrl("/technicians"))
			.andExpect(flash().attribute("success", containsString("supprimé")));
		assertThat(this.technicianService.findAll()).isEmpty();
		assertThat(this.ticketService.findAll()).hasSize(1);
	}

	@Test
	void ticketsCanBeAssignedAndUnassignedFromTheTechnicianPage() throws Exception {
		TicketResponse ticket = newTicket("To assign");
		TicketResponse closed = closedTicket("Archived");
		TechnicianResponse technician = newTechnician("jean@example.com");
		String base = "/technicians/" + technician.id();

		this.mockMvc.perform(get(base))
			.andExpect(content().string(containsString("Affecter un ticket")))
			.andExpect(content().string(containsString("To assign")))
			.andExpect(content().string(not(containsString("Archived"))));
		this.mockMvc.perform(post(base + "/tickets").param("ticketId", String.valueOf(ticket.id())))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("success", containsString("affecté")));
		assertThat(this.ticketService.findById(ticket.id()).technicianId()).isEqualTo(technician.id());
		this.mockMvc.perform(get(base))
			.andExpect(content().string(containsString("To assign")))
			.andExpect(content().string(containsString("Désaffecter")));

		this.mockMvc.perform(post(base + "/tickets/" + ticket.id() + "/remove"))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("success", containsString("retiré")));
		assertThat(this.ticketService.findById(ticket.id()).technicianId()).isNull();

		// A closed ticket cannot be assigned, a foreign ticket cannot be removed from this technician
		this.mockMvc.perform(postFrom(base, base + "/tickets").param("ticketId", String.valueOf(closed.id())))
			.andExpect(flash().attribute("error", containsString("clôturé")));
		this.mockMvc.perform(post(base + "/tickets/" + closed.id() + "/remove"))
			.andExpect(flash().attribute("error", containsString("n'est pas affecté")));
	}

	@Test
	void unassignmentFromTheTechnicianPageFollowsTheBusinessRule() throws Exception {
		TicketResponse ticket = inProgressTicket("Busy");
		String base = "/technicians/" + ticket.technicianId();

		this.mockMvc.perform(get(base)).andExpect(content().string(containsString("non désaffectable")));
		this.mockMvc.perform(postFrom(base, base + "/tickets/" + ticket.id() + "/remove"))
			.andExpect(flash().attribute("error", containsString("OPEN")));
		assertThat(this.ticketService.findById(ticket.id()).technicianId()).isEqualTo(ticket.technicianId());
	}

	// --- Profiles ---

	@Test
	void profileLifeCycleThroughTheBrowser() throws Exception {
		TechnicianResponse technician = newTechnician("jean@example.com");
		String base = "/technicians/" + technician.id();

		this.mockMvc.perform(get(base + "/profile/new"))
			.andExpect(status().isOk())
			.andExpect(view().name("profiles/form"))
			.andExpect(content().string(containsString("Créer le profil")));
		this.mockMvc.perform(post(base + "/profile").param("phone", "0102030405")
				.param("specialization", "Réseau")
				.param("biography", "Dix ans d'expérience"))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("success", containsString("créé")));
		this.mockMvc.perform(get(base))
			.andExpect(content().string(containsString("0102030405")))
			.andExpect(content().string(containsString("Réseau")))
			.andExpect(content().string(containsString("Dix ans d&#39;expérience")));

		this.mockMvc.perform(get(base + "/profile/edit"))
			.andExpect(content().string(containsString("value=\"R&eacute;seau\"")));
		this.mockMvc.perform(post(base + "/profile/edit").param("phone", "")
				.param("specialization", "Sécurité")
				.param("biography", ""))
			.andExpect(redirectedUrl(base));
		assertThat(this.profileService.findByTechnicianId(technician.id()).specialization()).isEqualTo("Sécurité");
		assertThat(this.profileService.findByTechnicianId(technician.id()).phone()).isNull();

		this.mockMvc.perform(post(base + "/profile/delete"))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("success", containsString("supprimé")));
		this.mockMvc.perform(get(base)).andExpect(content().string(containsString("Aucun profil renseigné")));
	}

	@Test
	void invalidProfileKeepsTheTypedValues() throws Exception {
		TechnicianResponse technician = newTechnician("jean@example.com");

		this.mockMvc.perform(post("/technicians/" + technician.id() + "/profile").param("phone", "1".repeat(31))
				.param("specialization", "Typed")
				.param("biography", ""))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("form", "phone"))
			.andExpect(content().string(containsString("value=\"Typed\"")));
		assertThat(this.technicianService.findById(technician.id()).profile()).isNull();
	}

	@Test
	void secondProfileIsRefused() throws Exception {
		TechnicianResponse technician = newTechnician("jean@example.com");
		this.profileService.create(technician.id(), new TechnicianProfileRequest(null, "Réseau", null));
		String base = "/technicians/" + technician.id();

		this.mockMvc.perform(postFrom(base, base + "/profile"))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("error", containsString("déjà un profil")));
	}

	@Test
	void profilesAreListedAndSearched() throws Exception {
		TechnicianResponse alice = this.technicianService.create(new TechnicianRequest("Alice", "Durand", "alice@example.com"));
		TechnicianResponse bob = this.technicianService.create(new TechnicianRequest("Bob", "Martin", "bob@example.com"));
		this.profileService.create(alice.id(), new TechnicianProfileRequest(null, "Sécurité réseau", null));
		this.profileService.create(bob.id(), new TechnicianProfileRequest("0102030405", "Bureautique", null));

		this.mockMvc.perform(get("/profiles"))
			.andExpect(status().isOk())
			.andExpect(view().name("profiles/list"))
			.andExpect(content().string(containsString("Sécurité réseau")))
			.andExpect(content().string(containsString("Bureautique")));
		this.mockMvc.perform(get("/profiles").param("keyword", "SÉCURITÉ"))
			.andExpect(content().string(containsString("Alice Durand")))
			.andExpect(content().string(not(containsString("Bob Martin"))));
		this.mockMvc.perform(get("/profiles").param("keyword", "martin"))
			.andExpect(content().string(containsString("Bob Martin")))
			.andExpect(content().string(not(containsString("Alice Durand"))));
		this.mockMvc.perform(get("/profiles").param("keyword", "zzz"))
			.andExpect(content().string(containsString("Aucun profil ne correspond")));
	}

}
