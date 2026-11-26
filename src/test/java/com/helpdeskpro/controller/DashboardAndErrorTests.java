package com.helpdeskpro.controller;

import com.helpdeskpro.dto.TicketResponse;
import com.helpdeskpro.entity.TicketPriority;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebTest
class DashboardAndErrorTests extends AbstractWebTests {

	@Test
	void emptyDashboardShowsZerosAndEmptyStates() throws Exception {
		this.mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(view().name("dashboard"))
			.andExpect(content().string(containsString("Tableau de bord")))
			.andExpect(content().string(not(containsString("id=\"stat-total\""))))
			.andExpect(content().string(containsString("Rien d’urgent")))
			.andExpect(content().string(containsString("Aucun incident enregistré")))
			.andExpect(content().string(containsString("Aucun ticket pour le moment")));
	}

	@Test
	void dashboardStatisticsComeFromTheData() throws Exception {
		newTicket("First");
		newTicket("Second", TicketPriority.HIGH);
		inProgressTicket("Third");
		closedTicket("Fourth");

		this.mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(content().string(matchesPattern("(?s).*id=\"stat-total\">4</strong>.*")))
			.andExpect(content().string(matchesPattern("(?s).*id=\"stat-open\">2</span>.*")))
			.andExpect(content().string(matchesPattern("(?s).*id=\"stat-in-progress\">1</span>.*")))
			.andExpect(content().string(matchesPattern("(?s).*id=\"stat-resolved\">0</span>.*")))
			.andExpect(content().string(matchesPattern("(?s).*id=\"stat-closed\">1</span>.*")))
			.andExpect(content().string(matchesPattern("(?s).*id=\"stat-overdue\">0</span>.*")))
			.andExpect(content().string(matchesPattern("(?s).*id=\"stat-technicians\">2</span>.*")))
			.andExpect(content().string(containsString("href=\"/tickets?status=OPEN\"")))
			.andExpect(content().string(containsString("Fourth")))
			.andExpect(content().string(containsString("Rien d’urgent")));
	}

	@Test
	void overdueAndCriticalTicketsAreUrgent() throws Exception {
		TicketResponse critical = newTicket("Server room on fire", TicketPriority.CRITICAL);
		newTicket("Printer jam", TicketPriority.LOW);

		this.mockMvc.perform(get("/"))
			.andExpect(content().string(containsString("Server room on fire")))
			.andExpect(content().string(matchesPattern("(?s).*id=\"stat-overdue\">0</span>.*")));

		this.clock.set(critical.createdAt().plusHours(5));
		this.mockMvc.perform(get("/"))
			.andExpect(content().string(matchesPattern("(?s).*id=\"stat-overdue\">1</span>.*")))
			.andExpect(content().string(containsString("En retard")));
		this.mockMvc.perform(get("/tickets").param("overdue", "true"))
			.andExpect(content().string(containsString("Server room on fire")))
			.andExpect(content().string(not(containsString("Printer jam"))));
	}

	@Test
	void pagesUseTheLocalVendorResourcesAndTheFrenchLayout() throws Exception {
		this.mockMvc.perform(get("/"))
			.andExpect(content().string(containsString("href=\"/vendor/bootstrap/css/bootstrap.min-")))
			.andExpect(content().string(containsString("src=\"/vendor/bootstrap/js/bootstrap.bundle.min-")))
			.andExpect(content().string(containsString("href=\"/css/helpdesk-")))
			.andExpect(content().string(containsString("lang=\"fr\"")))
			.andExpect(content().string(not(containsString("cdn."))))
			.andExpect(content().string(containsString("HelpDesk Pro")));
		for (String resource : new String[] { "/vendor/bootstrap/css/bootstrap.min.css",
				"/vendor/bootstrap/js/bootstrap.bundle.min.js", "/vendor/bootstrap/LICENSE",
				"/vendor/bootstrap-icons/bootstrap-icons.min.css", "/vendor/bootstrap-icons/fonts/bootstrap-icons.woff2",
				"/vendor/bootstrap-icons/LICENSE", "/css/helpdesk.css", "/js/helpdesk.js", "/js/theme.js", "/img/logo.svg" }) {
			this.mockMvc.perform(get(resource)).andExpect(status().isOk());
		}
	}

	@Test
	void unknownRouteGivesTheFrench404Page() throws Exception {
		this.mockMvc.perform(get("/does-not-exist")).andExpect(status().isNotFound());
	}

	@Test
	void unknownIdentifiersGiveTheFrench404Page() throws Exception {
		for (String url : new String[] { "/tickets/9999", "/tickets/9999/edit", "/technicians/9999",
				"/technicians/9999/profile/new", "/categories/9999", "/tags/9999", "/tags/9999/edit" }) {
			this.mockMvc.perform(get(url))
				.andExpect(status().isNotFound())
				.andExpect(view().name("error/404"))
				.andExpect(content().string(containsString("Page introuvable")))
				.andExpect(content().string(containsString("introuvable (identifiant 9999)")));
		}
	}

	@Test
	void invalidParametersGiveAClientError() throws Exception {
		this.mockMvc.perform(get("/tickets").param("status", "NOPE")).andExpect(status().isBadRequest());
	}

}
