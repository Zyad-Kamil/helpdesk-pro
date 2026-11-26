package com.helpdeskpro.controller;

import com.helpdeskpro.dto.TechnicianResponse;
import com.helpdeskpro.dto.TicketResponse;
import com.helpdeskpro.entity.TicketPriority;
import com.helpdeskpro.entity.TicketStatus;
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
class TicketControllerTests extends AbstractWebTests {

	// --- List and search ---

	@Test
	void emptyListShowsAnEmptyState() throws Exception {
		this.mockMvc.perform(get("/tickets"))
			.andExpect(status().isOk())
			.andExpect(view().name("tickets/list"))
			.andExpect(content().string(containsString("Aucun ticket ne correspond")));
	}

	@Test
	void listShowsTicketsWithBadgesAndFiltersThem() throws Exception {
		newTicket("Network down", TicketPriority.HIGH);
		newTicket("Printer jam");
		inProgressTicket("Mail broken");

		this.mockMvc.perform(get("/tickets"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Network down")))
			.andExpect(content().string(containsString("Printer jam")))
			.andExpect(content().string(containsString("Mail broken")))
			.andExpect(content().string(containsString("hp-status-IN_PROGRESS")))
			.andExpect(content().string(containsString("hp-priority-HIGH")))
			.andExpect(content().string(containsString(">En cours<")))
			.andExpect(content().string(containsString(">Haute<")));

		this.mockMvc.perform(get("/tickets").param("keyword", "NETWORK"))
			.andExpect(content().string(containsString("Network down")))
			.andExpect(content().string(not(containsString("Printer jam"))));
		this.mockMvc.perform(get("/tickets").param("status", "IN_PROGRESS"))
			.andExpect(content().string(containsString("Mail broken")))
			.andExpect(content().string(not(containsString("Printer jam"))));
		this.mockMvc.perform(get("/tickets").param("priority", "HIGH"))
			.andExpect(content().string(containsString("Network down")))
			.andExpect(content().string(not(containsString("Mail broken"))));
	}

	@Test
	void listFiltersByTechnicianAndCategory() throws Exception {
		TicketResponse assigned = newTicket("Assigned one");
		newTicket("Loose one");
		TechnicianResponse technician = newTechnician("tech@example.com");
		this.ticketService.assignTechnician(assigned.id(), technician.id());
		this.ticketService.assignCategory(assigned.id(), newCategory("Hardware").id());
		long category = this.categoryService.findAll().getFirst().id();

		this.mockMvc.perform(get("/tickets").param("technicianId", String.valueOf(technician.id())))
			.andExpect(content().string(containsString("Assigned one")))
			.andExpect(content().string(not(containsString("Loose one"))));
		this.mockMvc.perform(get("/tickets").param("categoryId", String.valueOf(category)))
			.andExpect(content().string(containsString("Assigned one")))
			.andExpect(content().string(not(containsString("Loose one"))));
	}

	// --- Create ---

	@Test
	void validCreationRedirectsToTheTicketWithASuccessMessage() throws Exception {
		this.mockMvc.perform(post("/tickets").param("title", "  New   incident ")
				.param("description", "Something broke")
				.param("requesterName", "Alice Martin")
				.param("requesterEmail", "ALICE@Example.com")
				.param("priority", "CRITICAL"))
			.andExpect(status().is3xxRedirection())
			.andExpect(flash().attribute("success", containsString("a été créé")));

		TicketResponse created = this.ticketService.findAll().getFirst();
		assertThat(created.title()).isEqualTo("New incident");
		assertThat(created.requesterEmail()).isEqualTo("alice@example.com");
		assertThat(created.status()).isEqualTo(TicketStatus.OPEN);
		assertThat(created.priority()).isEqualTo(TicketPriority.CRITICAL);
		this.mockMvc.perform(get("/tickets/" + created.id()))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("New incident")));
	}

	@Test
	void newFormIsRendered() throws Exception {
		this.mockMvc.perform(get("/tickets/new"))
			.andExpect(status().isOk())
			.andExpect(view().name("tickets/form"))
			.andExpect(content().string(containsString("Nouveau ticket")))
			.andExpect(content().string(containsString("action=\"/tickets\"")));
	}

	@Test
	void invalidCreationRedisplaysTheFormWithErrorsAndTheTypedValues() throws Exception {
		this.mockMvc.perform(post("/tickets").param("title", " ")
				.param("description", "Kept description")
				.param("requesterName", "Kept Name")
				.param("requesterEmail", "not-an-email")
				.param("priority", "HIGH"))
			.andExpect(status().isOk())
			.andExpect(view().name("tickets/form"))
			.andExpect(model().attributeHasFieldErrors("form", "title", "requesterEmail"))
			.andExpect(content().string(containsString("is-invalid")))
			.andExpect(content().string(containsString("Kept description")))
			.andExpect(content().string(containsString("value=\"Kept Name\"")))
			.andExpect(content().string(containsString("value=\"not-an-email\"")));
		assertThat(this.ticketService.findAll()).isEmpty();
	}

	@Test
	void unsupportedPriorityIsReportedAsAFieldError() throws Exception {
		this.mockMvc.perform(post("/tickets").param("title", "T")
				.param("description", "D")
				.param("requesterName", "N")
				.param("requesterEmail", "n@example.com")
				.param("priority", "URGENT"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("form", "priority"));
	}

	@Test
	void userContentIsEscaped() throws Exception {
		TicketResponse ticket = newTicket("<script>alert('x')</script>");

		this.mockMvc.perform(get("/tickets/" + ticket.id()))
			.andExpect(content().string(not(containsString("<script>alert"))))
			.andExpect(content().string(containsString("&lt;script&gt;alert")));
		this.mockMvc.perform(get("/tickets"))
			.andExpect(content().string(not(containsString("<script>alert"))))
			.andExpect(content().string(containsString("&lt;script&gt;")));
	}

	// --- Detail: only the allowed actions are offered ---

	@Test
	void openTicketWithoutTechnicianOffersAssignmentButNoStart() throws Exception {
		TicketResponse ticket = newTicket("Open one");
		newTechnician("tech@example.com");

		this.mockMvc.perform(get("/tickets/" + ticket.id()))
			.andExpect(status().isOk())
			.andExpect(view().name("tickets/detail"))
			.andExpect(content().string(containsString("Affecter un technicien")))
			.andExpect(content().string(containsString("Affectez d'abord un technicien actif")))
			.andExpect(content().string(containsString("disabled")))
			.andExpect(content().string(not(containsString("Désaffecter"))))
			.andExpect(content().string(containsString("data-confirm=\"Supprimer définitivement le ticket")));
	}

	@Test
	void inProgressTicketOffersResolutionWithASolutionField() throws Exception {
		TicketResponse ticket = inProgressTicket("Working");
		newTechnician("other@example.com");

		this.mockMvc.perform(get("/tickets/" + ticket.id()))
			.andExpect(content().string(containsString("name=\"resolution\"")))
			.andExpect(content().string(containsString("Solution apportée")))
			.andExpect(content().string(containsString("Réaffecter")))
			.andExpect(content().string(not(containsString("Désaffecter"))))
			.andExpect(content().string(not(containsString("Démarrer le traitement"))));
	}

	@Test
	void closedTicketOffersNoModification() throws Exception {
		TicketResponse ticket = closedTicket("Done");
		newCategory("Hardware");
		newTag("urgent");

		this.mockMvc.perform(get("/tickets/" + ticket.id()))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Ticket clôturé")))
			.andExpect(content().string(containsString("Fixed")))
			.andExpect(content().string(not(containsString("/edit"))))
			.andExpect(content().string(not(containsString("Associer une catégorie"))))
			.andExpect(content().string(not(containsString("Ajouter un tag"))))
			.andExpect(content().string(not(containsString("name=\"status\""))))
			.andExpect(content().string(containsString("/delete")));
	}

	@Test
	void overdueTicketShowsItsDueDateAndTheLateBadge() throws Exception {
		TicketResponse ticket = newTicket("Late one", TicketPriority.CRITICAL);
		this.clock.set(ticket.createdAt().plusHours(10));

		this.mockMvc.perform(get("/tickets/" + ticket.id()))
			.andExpect(content().string(containsString("En retard")))
			.andExpect(content().string(containsString(ticket.dueAt()
				.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))));
	}

	// --- Edit and delete ---

	@Test
	void editUpdatesTheTicket() throws Exception {
		TicketResponse ticket = newTicket("Before");

		this.mockMvc.perform(get("/tickets/" + ticket.id() + "/edit"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("value=\"Before\"")));
		this.mockMvc.perform(post("/tickets/" + ticket.id() + "/edit").param("title", "After")
				.param("description", "New description")
				.param("requesterName", "Bob")
				.param("requesterEmail", "bob@example.com")
				.param("priority", "LOW"))
			.andExpect(redirectedUrl("/tickets/" + ticket.id()))
			.andExpect(flash().attribute("success", containsString("modifié")));

		TicketResponse updated = this.ticketService.findById(ticket.id());
		assertThat(updated.title()).isEqualTo("After");
		assertThat(updated.priority()).isEqualTo(TicketPriority.LOW);
	}

	@Test
	void invalidEditKeepsTheTypedValues() throws Exception {
		TicketResponse ticket = newTicket("Before");

		this.mockMvc.perform(post("/tickets/" + ticket.id() + "/edit").param("title", "")
				.param("description", "Typed description")
				.param("requesterName", "Bob")
				.param("requesterEmail", "bob@example.com")
				.param("priority", "LOW"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("form", "title"))
			.andExpect(content().string(containsString("Typed description")))
			.andExpect(content().string(containsString("Modifier le ticket n°" + ticket.id())));
		assertThat(this.ticketService.findById(ticket.id()).title()).isEqualTo("Before");
	}

	@Test
	void closedTicketCannotBeEdited() throws Exception {
		TicketResponse ticket = closedTicket("Done");

		this.mockMvc.perform(get("/tickets/" + ticket.id() + "/edit").header("Referer", "http://localhost/tickets/" + ticket.id()))
			.andExpect(redirectedUrl("/tickets/" + ticket.id()))
			.andExpect(flash().attribute("error", containsString("clôturé")));
		this.mockMvc.perform(postFrom("/tickets/" + ticket.id(), "/tickets/" + ticket.id() + "/edit")
				.param("title", "Hacked")
				.param("description", "d")
				.param("requesterName", "n")
				.param("requesterEmail", "n@example.com"))
			.andExpect(redirectedUrl("/tickets/" + ticket.id()))
			.andExpect(flash().attribute("error", containsString("clôturé")));
		assertThat(this.ticketService.findById(ticket.id()).title()).isEqualTo("Done");
	}

	@Test
	void deleteRemovesTheTicketOnlyAndNeverUsesGet() throws Exception {
		TicketResponse ticket = newTicket("Doomed");
		TechnicianResponse technician = newTechnician("tech@example.com");
		this.ticketService.assignTechnician(ticket.id(), technician.id());
		long tag = newTag("urgent").id();
		this.ticketService.addTag(ticket.id(), tag);

		this.mockMvc.perform(get("/tickets/" + ticket.id() + "/delete")).andExpect(status().is4xxClientError());
		assertThat(this.ticketService.findAll()).hasSize(1);

		this.mockMvc.perform(post("/tickets/" + ticket.id() + "/delete"))
			.andExpect(redirectedUrl("/tickets"))
			.andExpect(flash().attribute("success", containsString("supprimé")));
		assertThat(this.ticketService.findAll()).isEmpty();
		assertThat(this.technicianService.findById(technician.id())).isNotNull();
		assertThat(this.tagService.findById(tag)).isNotNull();
	}

	// --- Status ---

	@Test
	void statusFollowsTheLifeCycleThroughTheBrowser() throws Exception {
		TicketResponse ticket = newTicket("Lifecycle");
		TechnicianResponse technician = newTechnician("tech@example.com");
		String base = "/tickets/" + ticket.id();
		this.mockMvc.perform(post(base + "/technician").param("technicianId", String.valueOf(technician.id())))
			.andExpect(redirectedUrl(base));

		this.mockMvc.perform(post(base + "/status").param("status", "IN_PROGRESS")).andExpect(redirectedUrl(base));
		assertThat(this.ticketService.findById(ticket.id()).status()).isEqualTo(TicketStatus.IN_PROGRESS);

		this.mockMvc.perform(post(base + "/status").param("status", "RESOLVED").param("resolution", "Cable replaced"))
			.andExpect(redirectedUrl(base));
		TicketResponse resolved = this.ticketService.findById(ticket.id());
		assertThat(resolved.status()).isEqualTo(TicketStatus.RESOLVED);
		assertThat(resolved.resolution()).isEqualTo("Cable replaced");
		assertThat(resolved.resolvedAt()).isNotNull();
		this.mockMvc.perform(get(base))
			.andExpect(content().string(containsString("Cable replaced")))
			.andExpect(content().string(containsString("Clôturer")));

		this.mockMvc.perform(post(base + "/status").param("status", "CLOSED")).andExpect(redirectedUrl(base));
		assertThat(this.ticketService.findById(ticket.id()).status()).isEqualTo(TicketStatus.CLOSED);
	}

	@Test
	void businessErrorsGoBackToThePageWithAnExplicitMessage() throws Exception {
		TicketResponse ticket = newTicket("Strict");
		String base = "/tickets/" + ticket.id();

		// No technician: cannot start
		this.mockMvc.perform(postFrom(base, base + "/status").param("status", "IN_PROGRESS"))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("error", containsString("technicien actif")));
		// Skipping a step is forbidden
		this.mockMvc.perform(postFrom(base, base + "/status").param("status", "CLOSED").param("resolution", "x"))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("error", containsString("Transition interdite")));
		assertThat(this.ticketService.findById(ticket.id()).status()).isEqualTo(TicketStatus.OPEN);

		// Resolution without a solution
		TicketResponse inProgress = inProgressTicket("Without solution");
		String other = "/tickets/" + inProgress.id();
		this.mockMvc.perform(postFrom(other, other + "/status").param("status", "RESOLVED").param("resolution", "  "))
			.andExpect(redirectedUrl(other))
			.andExpect(flash().attribute("error", containsString("solution non vide")));
		assertThat(this.ticketService.findById(inProgress.id()).status()).isEqualTo(TicketStatus.IN_PROGRESS);

		// The refusal message is displayed on the page
		this.mockMvc.perform(get(other).flashAttr("error", "Message de test"))
			.andExpect(content().string(containsString("Message de test")))
			.andExpect(content().string(containsString("data-kind=\"error\"")));
	}

	@Test
	void refusalWithoutRefererFallsBackToTheHomePage() throws Exception {
		TicketResponse ticket = newTicket("Strict");

		this.mockMvc.perform(post("/tickets/" + ticket.id() + "/status").param("status", "RESOLVED"))
			.andExpect(redirectedUrl("/"))
			.andExpect(flash().attribute("error", containsString("Transition interdite")));
		this.mockMvc.perform(post("/tickets/" + ticket.id() + "/status").param("status", "RESOLVED")
				.header("Referer", "http://evil.example.com/steal"))
			.andExpect(redirectedUrl("/"));
	}

	@Test
	void statusChangeOnUnknownTicketIs404() throws Exception {
		this.mockMvc.perform(post("/tickets/9999/status").param("status", "IN_PROGRESS"))
			.andExpect(status().isNotFound());
	}

	// --- Technician ---

	@Test
	void technicianCanBeAssignedReassignedAndUnassigned() throws Exception {
		TicketResponse ticket = newTicket("Assign me");
		TechnicianResponse first = newTechnician("first@example.com");
		TechnicianResponse second = this.technicianService
			.create(new com.helpdeskpro.dto.TechnicianRequest("Marie", "Curie", "second@example.com"));
		String base = "/tickets/" + ticket.id();

		this.mockMvc.perform(post(base + "/technician").param("technicianId", String.valueOf(first.id())))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("success", containsString("affecté")));
		assertThat(this.ticketService.findById(ticket.id()).technicianId()).isEqualTo(first.id());
		this.mockMvc.perform(get(base))
			.andExpect(content().string(containsString("Jean Dupont")))
			.andExpect(content().string(containsString("Réaffecter à")))
			.andExpect(content().string(containsString("Désaffecter")))
			.andExpect(content().string(containsString("Marie Curie")));

		this.mockMvc.perform(post(base + "/technician").param("technicianId", String.valueOf(second.id())))
			.andExpect(redirectedUrl(base));
		assertThat(this.ticketService.findById(ticket.id()).technicianId()).isEqualTo(second.id());

		this.mockMvc.perform(post(base + "/technician/remove"))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("success", containsString("retiré")));
		assertThat(this.ticketService.findById(ticket.id()).technicianId()).isNull();
	}

	@Test
	void inactiveTechnicianIsNotOfferedAndRefusedIfForced() throws Exception {
		TicketResponse ticket = newTicket("Assign me");
		TechnicianResponse inactive = newTechnician("off@example.com");
		this.technicianService.setActive(inactive.id(), false);
		String base = "/tickets/" + ticket.id();

		this.mockMvc.perform(get(base)).andExpect(content().string(not(containsString("Jean Dupont"))));
		this.mockMvc.perform(postFrom(base, base + "/technician").param("technicianId", String.valueOf(inactive.id())))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("error", containsString("inactif")));
		assertThat(this.ticketService.findById(ticket.id()).technicianId()).isNull();
	}

	@Test
	void unassignmentIsRefusedOnceInProgress() throws Exception {
		TicketResponse ticket = inProgressTicket("Busy");
		String base = "/tickets/" + ticket.id();

		this.mockMvc.perform(postFrom(base, base + "/technician/remove"))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("error", containsString("OPEN")));
		assertThat(this.ticketService.findById(ticket.id()).technicianId()).isNotNull();
	}

	// --- Category and tags ---

	@Test
	void categoryCanBeAssignedChangedAndRemoved() throws Exception {
		TicketResponse ticket = newTicket("Categorized");
		long hardware = newCategory("Hardware").id();
		long software = newCategory("Software").id();
		String base = "/tickets/" + ticket.id();

		this.mockMvc.perform(post(base + "/category").param("categoryId", String.valueOf(hardware)))
			.andExpect(redirectedUrl(base));
		this.mockMvc.perform(get(base))
			.andExpect(content().string(containsString("Changer de catégorie")))
			.andExpect(content().string(containsString("Retirer la catégorie")))
			.andExpect(content().string(containsString("Hardware")));
		this.mockMvc.perform(post(base + "/category").param("categoryId", String.valueOf(software)))
			.andExpect(redirectedUrl(base));
		assertThat(this.ticketService.findById(ticket.id()).categoryName()).isEqualTo("Software");

		this.mockMvc.perform(post(base + "/category/remove")).andExpect(redirectedUrl(base));
		assertThat(this.ticketService.findById(ticket.id()).categoryId()).isNull();
		assertThat(this.categoryService.findAll()).hasSize(2);
	}

	@Test
	void tagsCanBeAddedAndRemoved() throws Exception {
		TicketResponse ticket = newTicket("Tagged");
		long urgent = newTag("urgent").id();
		long network = newTag("network").id();
		String base = "/tickets/" + ticket.id();

		this.mockMvc.perform(post(base + "/tags").param("tagId", String.valueOf(urgent))).andExpect(redirectedUrl(base));
		this.mockMvc.perform(post(base + "/tags").param("tagId", String.valueOf(network))).andExpect(redirectedUrl(base));
		this.mockMvc.perform(get(base))
			.andExpect(content().string(containsString("urgent")))
			.andExpect(content().string(containsString("network")))
			.andExpect(content().string(containsString("Retirer le tag urgent")));
		assertThat(this.ticketService.findById(ticket.id()).tags()).hasSize(2);

		this.mockMvc.perform(post(base + "/tags/" + urgent + "/remove"))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("success", containsString("retiré")));
		assertThat(this.ticketService.findById(ticket.id()).tags()).hasSize(1);
		assertThat(this.tagService.findAll()).hasSize(2);
	}

	@Test
	void relationshipsOfAClosedTicketCannotBeChanged() throws Exception {
		TicketResponse ticket = closedTicket("Frozen");
		long category = newCategory("Hardware").id();
		long tag = newTag("urgent").id();
		String base = "/tickets/" + ticket.id();

		this.mockMvc.perform(postFrom(base, base + "/category").param("categoryId", String.valueOf(category)))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("error", containsString("clôturé")));
		this.mockMvc.perform(postFrom(base, base + "/tags").param("tagId", String.valueOf(tag)))
			.andExpect(flash().attribute("error", containsString("clôturé")));
		assertThat(this.ticketService.findById(ticket.id()).categoryId()).isNull();
		assertThat(this.ticketService.findById(ticket.id()).tags()).isEmpty();
	}

}
