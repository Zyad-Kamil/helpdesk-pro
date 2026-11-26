package com.helpdeskpro.controller;

import com.helpdeskpro.dto.TicketResponse;
import com.helpdeskpro.dto.TicketStatusRequest;
import com.helpdeskpro.entity.TicketPriority;
import com.helpdeskpro.entity.TicketStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Theme, command palette, toasts, life-cycle indicator, Kanban and dashboard charts. */
@WebTest
class SignatureUiTests extends AbstractWebTests {

	// --- Shell: theme, palette, toasts, accessibility ---

	@Test
	void everyPageIsDarkByDefaultAndLoadsTheThemeBeforeTheStyles() throws Exception {
		for (String url : new String[] { "/", "/tickets", "/tickets/board", "/tickets/new", "/technicians", "/profiles",
				"/categories", "/tags" }) {
			String html = this.mockMvc.perform(get(url))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("data-bs-theme=\"dark\"")))
				.andExpect(content().string(containsString("data-hp-theme-toggle")))
				.andExpect(content().string(containsString("data-hp-palette-open")))
				.andExpect(content().string(containsString("id=\"hp-palette\"")))
				.andExpect(content().string(containsString("href=\"#contenu\"")))
				.andExpect(content().string(containsString("aria-label=\"Navigation principale\"")))
				.andReturn()
				.getResponse()
				.getContentAsString();
			assertThat(html.indexOf("/js/theme-")).as(url).isPositive().isLessThan(html.indexOf("/css/helpdesk-"));
		}
	}

	@Test
	void paletteOffersOnlyRealRoutes() throws Exception {
		String html = this.mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString();
		String palette = html.substring(html.indexOf("id=\"hp-palette\""), html.indexOf("</dialog>"));

		for (String route : new String[] { "href=\"/\"", "href=\"/tickets\"", "href=\"/tickets/board\"",
				"href=\"/tickets?overdue=true\"", "href=\"/technicians\"", "href=\"/profiles\"", "href=\"/categories\"",
				"href=\"/tags\"", "href=\"/tickets/new\"", "href=\"/technicians/new\"", "href=\"/categories/new\"",
				"href=\"/tags/new\"", "data-palette-action=\"theme\"", "data-base=\"/tickets?keyword=\"",
				"data-base=\"/tickets/\"" }) {
			assertThat(palette).as(route).contains(route);
		}
		// Every palette link resolves
		for (String url : new String[] { "/tickets/board", "/tickets/new", "/technicians/new", "/categories/new",
				"/tags/new" }) {
			this.mockMvc.perform(get(url)).andExpect(status().isOk());
		}
	}

	@Test
	void flashMessagesBecomeAccessibleToasts() throws Exception {
		this.mockMvc.perform(get("/").flashAttr("success", "Ticket créé").flashAttr("error", "Action refusée"))
			.andExpect(content().string(containsString("id=\"hp-toasts\"")))
			.andExpect(content().string(containsString("aria-live=\"polite\"")))
			.andExpect(content().string(matchesPattern("(?s).*data-kind=\"success\"[^>]*>Ticket créé<.*")))
			.andExpect(content().string(matchesPattern("(?s).*data-kind=\"error\"[^>]*>Action refusée<.*")))
			.andExpect(content().string(containsString("<noscript>")));
		this.mockMvc.perform(get("/")).andExpect(content().string(not(containsString("class=\"hp-flash\""))));
	}

	@Test
	void flashContentIsEscaped() throws Exception {
		this.mockMvc.perform(get("/").flashAttr("error", "<img src=x onerror=alert(1)>"))
			.andExpect(content().string(not(containsString("<img src=x"))))
			.andExpect(content().string(containsString("&lt;img src=x")));
	}

	@Test
	void stylesheetDefinesBothThemesTheBrandPaletteAndReducedMotion() throws Exception {
		String css = this.mockMvc.perform(get("/css/helpdesk.css")).andReturn().getResponse().getContentAsString();

		for (String token : new String[] { "#0b1020", "#131c31", "#7c5cfc", "#22d3ee", "#f8fafc",
				":root[data-bs-theme=\"dark\"]", ":root[data-bs-theme=\"light\"]", "prefers-reduced-motion: reduce",
				":focus-visible" }) {
			assertThat(css).as(token).contains(token);
		}
		// The Bootstrap rule that used to wipe the sidebar background must not be relied upon any more
		assertThat(css).doesNotContain("offcanvas-lg");
	}

	@Test
	void layoutNoLongerUsesBootstrapOffcanvasForTheSidebar() throws Exception {
		this.mockMvc.perform(get("/"))
			.andExpect(content().string(not(containsString("offcanvas"))))
			.andExpect(content().string(containsString("class=\"hp-sidebar\"")))
			.andExpect(content().string(containsString("data-hp-drawer-open")));
	}

	@Test
	void activeNavigationItemIsMarkedForAssistiveTechnologies() throws Exception {
		this.mockMvc.perform(get("/tickets/board"))
			.andExpect(content().string(matchesPattern("(?s).*hp-nav-link active\"[^>]*aria-current=\"page\"[^>]*>\\s*<i class=\"bi bi-kanban\".*")));
	}

	// --- Life-cycle indicator ---

	@Test
	void lifecycleIndicatorFollowsTheStatus() throws Exception {
		TicketResponse open = newTicket("Fresh");
		this.mockMvc.perform(get("/tickets/" + open.id()))
			.andExpect(content().string(containsString("aria-label=\"Cycle de vie du ticket\"")))
			.andExpect(content().string(matchesPattern("(?s).*hp-step is-current.*")))
			.andExpect(content().string(not(containsString("hp-step is-done"))))
			.andExpect(content().string(containsString("aria-current=\"step\"")));

		TicketResponse inProgress = inProgressTicket("Moving");
		String html = this.mockMvc.perform(get("/tickets/" + inProgress.id())).andReturn().getResponse().getContentAsString();
		assertThat(html.split("hp-step is-done", -1)).hasSize(2);
		assertThat(html.split("hp-step is-current", -1)).hasSize(2);

		TicketResponse closed = closedTicket("Finished");
		html = this.mockMvc.perform(get("/tickets/" + closed.id())).andReturn().getResponse().getContentAsString();
		assertThat(html.split("hp-step is-done", -1)).hasSize(5);
		assertThat(html).doesNotContain("hp-step is-current");
	}

	// --- Kanban ---

	@Test
	void emptyBoardShowsFourEmptyColumns() throws Exception {
		this.mockMvc.perform(get("/tickets/board"))
			.andExpect(status().isOk())
			.andExpect(view().name("tickets/board"))
			.andExpect(content().string(containsString("Kanban des tickets")))
			.andExpect(model().attributeExists("columns"));
		String html = this.mockMvc.perform(get("/tickets/board")).andReturn().getResponse().getContentAsString();
		assertThat(html.split("Aucun ticket dans cette colonne", -1)).hasSize(5);
	}

	@Test
	void boardGroupsTicketsByStatusAndOffersOnlyTheAllowedStep() throws Exception {
		TicketResponse open = newTicket("Open card");
		TicketResponse working = inProgressTicket("Working card");
		TicketResponse closed = closedTicket("Closed card");
		this.ticketService.changeStatus(working.id(), new TicketStatusRequest(TicketStatus.RESOLVED, "Fixed"));
		TicketResponse resolved = this.ticketService.findById(working.id());

		String html = this.mockMvc.perform(get("/tickets/board")).andReturn().getResponse().getContentAsString();

		assertThat(html).contains("Open card", "Working card", "Closed card");
		// Open without technician: start button disabled; resolved: close with confirmation; closed: no step
		assertThat(html).contains("Affectez d’abord un technicien actif");
		assertThat(html).contains("data-confirm=\"Clôturer le ticket n°" + resolved.id());
		assertThat(html).doesNotContain("/tickets/" + closed.id() + "/status");
		assertThat(html).contains("/tickets/" + open.id() + "/status");
	}

	@Test
	void boardStartButtonIsEnabledOnceATechnicianIsAssigned() throws Exception {
		TicketResponse ticket = newTicket("Assigned card");
		this.ticketService.assignTechnician(ticket.id(), newTechnician("tech@example.com").id());

		String html = this.mockMvc.perform(get("/tickets/board")).andReturn().getResponse().getContentAsString();

		assertThat(html).contains("Démarrer le traitement");
		assertThat(html).doesNotContain("Affectez d’abord");
	}

	@Test
	void boardActionsUseTheBusinessRulesAndComeBackToTheBoard() throws Exception {
		TicketResponse ticket = newTicket("Card");
		this.ticketService.assignTechnician(ticket.id(), newTechnician("tech@example.com").id());
		String url = "/tickets/" + ticket.id() + "/status";

		this.mockMvc.perform(post(url).param("status", "IN_PROGRESS").param("from", "board"))
			.andExpect(redirectedUrl("/tickets/board"))
			.andExpect(flash().attribute("success", containsString("mis à jour")));
		assertThat(this.ticketService.findById(ticket.id()).status()).isEqualTo(TicketStatus.IN_PROGRESS);

		// Skipping a step through the board is refused exactly like from the ticket page
		this.mockMvc.perform(postFrom("/tickets/board", url).param("status", "CLOSED").param("from", "board"))
			.andExpect(redirectedUrl("/tickets/board"))
			.andExpect(flash().attribute("error", containsString("Transition interdite")));
		assertThat(this.ticketService.findById(ticket.id()).status()).isEqualTo(TicketStatus.IN_PROGRESS);

		// Resolving needs a solution: the board only links to the ticket page for it
		this.mockMvc.perform(get("/tickets/board"))
			.andExpect(content().string(containsString("href=\"/tickets/" + ticket.id() + "#resolution\"")));
		this.mockMvc.perform(get("/tickets/" + ticket.id()))
			.andExpect(content().string(containsString("id=\"resolution\"")));
	}

	@Test
	void boardFiltersByKeywordPriorityAndTechnician() throws Exception {
		newTicket("Network outage", TicketPriority.HIGH);
		newTicket("Printer jam");

		this.mockMvc.perform(get("/tickets/board").param("keyword", "NETWORK"))
			.andExpect(content().string(containsString("Network outage")))
			.andExpect(content().string(not(containsString("Printer jam"))));
		this.mockMvc.perform(get("/tickets/board").param("priority", "HIGH"))
			.andExpect(content().string(containsString("Network outage")))
			.andExpect(content().string(not(containsString("Printer jam"))));
		this.mockMvc.perform(get("/tickets/board").param("technicianId", "999"))
			.andExpect(content().string(not(containsString("Network outage"))));
	}

	@Test
	void ticketsAreNotMistakenForTheBoardRoute() throws Exception {
		this.mockMvc.perform(get("/tickets/board")).andExpect(view().name("tickets/board"));
		this.mockMvc.perform(get("/tickets/9999")).andExpect(status().isNotFound());
	}

	// --- Dashboard charts ---

	@Test
	void dashboardChartsAreDrawnFromRealCounts() throws Exception {
		newTicket("A");
		newTicket("B");
		inProgressTicket("C");

		String html = this.mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString();

		// Two statuses have tickets: two donut segments; 2 of 3 open = 66.66...%
		assertThat(html.split("hp-donut-seg ", -1)).hasSize(3);
		assertThat(html).contains("3 tickets répartis par statut").contains("66.6");
		assertThat(html).contains("67 %").contains("33 %");
		// No resolved ticket yet: no ring, an explicit empty state instead
		assertThat(html).contains("Aucun ticket résolu pour le moment").doesNotContain("id=\"stat-ontime\"");
	}

	@Test
	void deadlineRingShowsTheRealShareOfTicketsResolvedOnTime() throws Exception {
		TicketResponse onTime = inProgressTicket("On time");
		TicketResponse late = inProgressTicket("Late");
		this.ticketService.changeStatus(onTime.id(), new TicketStatusRequest(TicketStatus.RESOLVED, "Fixed"));
		this.clock.set(late.createdAt().plusDays(10));
		this.ticketService.changeStatus(late.id(), new TicketStatusRequest(TicketStatus.RESOLVED, "Fixed"));

		this.mockMvc.perform(get("/"))
			.andExpect(content().string(containsString("id=\"stat-ontime\">50 %<")))
			.andExpect(content().string(containsString("dans les délais")))
			.andExpect(content().string(containsString("hors délai")));
	}

}
