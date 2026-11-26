package com.helpdeskpro.controller;

import com.helpdeskpro.dto.CategoryRequest;
import com.helpdeskpro.dto.CategoryResponse;
import com.helpdeskpro.dto.TagResponse;
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

/** Categories (1-N with tickets) and tags (N-N with tickets). */
@WebTest
class ReferenceDataControllerTests extends AbstractWebTests {

	// --- Categories ---

	@Test
	void categoryCrudThroughTheBrowser() throws Exception {
		this.mockMvc.perform(get("/categories"))
			.andExpect(status().isOk())
			.andExpect(view().name("categories/list"))
			.andExpect(content().string(containsString("Aucune catégorie ne correspond")));

		this.mockMvc.perform(post("/categories").param("name", "  Hard  ware ").param("description", "Matériel"))
			.andExpect(flash().attribute("success", containsString("a été créée")));
		CategoryResponse created = this.categoryService.findAll().getFirst();
		assertThat(created.name()).isEqualTo("Hard ware");
		this.mockMvc.perform(get("/categories/" + created.id()))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Hard ware")))
			.andExpect(content().string(containsString("Aucun ticket dans cette catégorie")));

		this.mockMvc.perform(get("/categories/" + created.id() + "/edit"))
			.andExpect(content().string(containsString("value=\"Hard ware\"")));
		this.mockMvc.perform(post("/categories/" + created.id() + "/edit").param("name", "Hardware")
				.param("description", ""))
			.andExpect(redirectedUrl("/categories/" + created.id()));
		assertThat(this.categoryService.findById(created.id()).name()).isEqualTo("Hardware");

		this.mockMvc.perform(post("/categories/" + created.id() + "/delete"))
			.andExpect(redirectedUrl("/categories"))
			.andExpect(flash().attribute("success", containsString("supprimée")));
		assertThat(this.categoryService.findAll()).isEmpty();
	}

	@Test
	void invalidAndDuplicateCategoriesKeepTheTypedValues() throws Exception {
		newCategory("Hardware");

		this.mockMvc.perform(post("/categories").param("name", " ").param("description", "Typed description"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("form", "name"))
			.andExpect(content().string(containsString("Typed description")));
		this.mockMvc.perform(post("/categories").param("name", "HARDWARE").param("description", "Other"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("form", "name"))
			.andExpect(content().string(containsString("existe déjà")))
			.andExpect(content().string(containsString("value=\"HARDWARE\"")));
		assertThat(this.categoryService.findAll()).hasSize(1);
	}

	@Test
	void categoriesAreSearched() throws Exception {
		this.categoryService.create(new CategoryRequest("Software", "Applications"));
		this.categoryService.create(new CategoryRequest("Hardware", "Physical equipment"));

		this.mockMvc.perform(get("/categories").param("keyword", "SOFT"))
			.andExpect(content().string(containsString("Software")))
			.andExpect(content().string(not(containsString("Hardware"))));
		this.mockMvc.perform(get("/categories").param("keyword", "equipment"))
			.andExpect(content().string(containsString("Hardware")));
	}

	@Test
	void ticketsCanBeLinkedToAndUnlinkedFromACategoryGraphically() throws Exception {
		CategoryResponse category = newCategory("Hardware");
		TicketResponse ticket = newTicket("Broken screen");
		closedTicket("Archived");
		String base = "/categories/" + category.id();

		this.mockMvc.perform(get(base))
			.andExpect(content().string(containsString("Ajouter un ticket à la catégorie")))
			.andExpect(content().string(containsString("Broken screen")))
			.andExpect(content().string(not(containsString("Archived"))));
		this.mockMvc.perform(post(base + "/tickets").param("ticketId", String.valueOf(ticket.id())))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("success", containsString("ajouté")));
		assertThat(this.ticketService.findById(ticket.id()).categoryId()).isEqualTo(category.id());
		this.mockMvc.perform(get(base))
			.andExpect(content().string(containsString("Broken screen")))
			.andExpect(content().string(containsString("Retirer")))
			.andExpect(content().string(containsString("Tickets de la catégorie (<span>1</span>)")));

		this.mockMvc.perform(post(base + "/tickets/" + ticket.id() + "/remove"))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("success", containsString("retiré")));
		assertThat(this.ticketService.findById(ticket.id()).categoryId()).isNull();
		this.mockMvc.perform(post(base + "/tickets/" + ticket.id() + "/remove"))
			.andExpect(flash().attribute("error", containsString("n'appartient pas")));
	}

	@Test
	void usedCategoryCannotBeDeletedAndTheMessageIsExplicit() throws Exception {
		CategoryResponse category = newCategory("Hardware");
		TicketResponse ticket = newTicket("Broken screen");
		this.ticketService.assignCategory(ticket.id(), category.id());
		String base = "/categories/" + category.id();

		this.mockMvc.perform(postFrom("/categories", base + "/delete"))
			.andExpect(redirectedUrl("/categories"))
			.andExpect(flash().attribute("error", containsString("« Hardware » ne peut pas être supprimée")))
			.andExpect(flash().attribute("error", containsString("1 ticket")));
		assertThat(this.categoryService.findAll()).hasSize(1);
		this.mockMvc.perform(get("/categories").flashAttr("error", "Message de refus"))
			.andExpect(content().string(containsString("Message de refus")));

		this.ticketService.removeCategory(ticket.id());
		this.mockMvc.perform(post(base + "/delete")).andExpect(redirectedUrl("/categories"));
		assertThat(this.categoryService.findAll()).isEmpty();
	}

	// --- Tags ---

	@Test
	void tagCrudThroughTheBrowser() throws Exception {
		this.mockMvc.perform(get("/tags"))
			.andExpect(status().isOk())
			.andExpect(view().name("tags/list"))
			.andExpect(content().string(containsString("Aucun tag ne correspond")));

		this.mockMvc.perform(post("/tags").param("name", "  urgent ")).andExpect(flash().attribute("success", containsString("créé")));
		TagResponse created = this.tagService.findAll().getFirst();
		assertThat(created.name()).isEqualTo("urgent");
		this.mockMvc.perform(get("/tags/" + created.id()))
			.andExpect(content().string(containsString("Aucun ticket ne porte ce tag")));

		this.mockMvc.perform(get("/tags/" + created.id() + "/edit"))
			.andExpect(content().string(containsString("value=\"urgent\"")));
		this.mockMvc.perform(post("/tags/" + created.id() + "/edit").param("name", "critique"))
			.andExpect(redirectedUrl("/tags/" + created.id()));
		assertThat(this.tagService.findById(created.id()).name()).isEqualTo("critique");
		this.mockMvc.perform(get("/tags").param("keyword", "CRIT"))
			.andExpect(content().string(containsString("critique")));

		this.mockMvc.perform(post("/tags/" + created.id() + "/delete"))
			.andExpect(redirectedUrl("/tags"))
			.andExpect(flash().attribute("success", containsString("supprimé")));
		assertThat(this.tagService.findAll()).isEmpty();
	}

	@Test
	void invalidAndDuplicateTagsKeepTheTypedValues() throws Exception {
		newTag("urgent");

		this.mockMvc.perform(post("/tags").param("name", ""))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("form", "name"));
		this.mockMvc.perform(post("/tags").param("name", "URGENT"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("form", "name"))
			.andExpect(content().string(containsString("value=\"URGENT\"")))
			.andExpect(content().string(containsString("existe déjà")));
		assertThat(this.tagService.findAll()).hasSize(1);
	}

	@Test
	void ticketsCanBeTaggedAndUntaggedFromTheTagPage() throws Exception {
		TagResponse tag = newTag("urgent");
		TicketResponse first = newTicket("First");
		TicketResponse second = newTicket("Second");
		this.ticketService.addTag(second.id(), tag.id());
		String base = "/tags/" + tag.id();

		this.mockMvc.perform(get(base))
			.andExpect(content().string(containsString("Tickets portant ce tag (<span>1</span>)")))
			.andExpect(content().string(containsString("Second")))
			.andExpect(content().string(containsString("First")));
		this.mockMvc.perform(post(base + "/tickets").param("ticketId", String.valueOf(first.id())))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("success", containsString("ajouté")));
		assertThat(this.ticketService.findById(first.id()).tags()).hasSize(1);

		this.mockMvc.perform(post(base + "/tickets/" + second.id() + "/remove"))
			.andExpect(redirectedUrl(base))
			.andExpect(flash().attribute("success", containsString("retiré")));
		assertThat(this.ticketService.findById(second.id()).tags()).isEmpty();
		assertThat(this.tagService.findAll()).hasSize(1);
	}

	@Test
	void usedTagCannotBeDeletedAndTheMessageIsExplicit() throws Exception {
		TagResponse tag = newTag("urgent");
		TicketResponse ticket = newTicket("Tagged");
		this.ticketService.addTag(ticket.id(), tag.id());
		String base = "/tags/" + tag.id();

		this.mockMvc.perform(postFrom("/tags", base + "/delete"))
			.andExpect(redirectedUrl("/tags"))
			.andExpect(flash().attribute("error", containsString("« urgent » ne peut pas être supprimé")))
			.andExpect(flash().attribute("error", containsString("1 ticket")));
		assertThat(this.tagService.findAll()).hasSize(1);

		this.ticketService.removeTag(ticket.id(), tag.id());
		this.mockMvc.perform(post(base + "/delete")).andExpect(redirectedUrl("/tags"));
		assertThat(this.tagService.findAll()).isEmpty();
	}

}
