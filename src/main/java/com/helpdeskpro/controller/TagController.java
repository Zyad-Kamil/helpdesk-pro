package com.helpdeskpro.controller;

import com.helpdeskpro.controller.form.TagForm;
import com.helpdeskpro.dto.TagResponse;
import com.helpdeskpro.dto.TicketResponse;
import com.helpdeskpro.dto.TicketSearchCriteria;
import com.helpdeskpro.exception.DuplicateResourceException;
import com.helpdeskpro.service.TagService;
import com.helpdeskpro.service.TicketService;
import jakarta.validation.ConstraintViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tags")
public class TagController {

	private static final String FORM_VIEW = "tags/form";

	private final TagService tagService;

	private final TicketService ticketService;

	public TagController(TagService tagService, TicketService ticketService) {
		this.tagService = tagService;
		this.ticketService = ticketService;
	}

	@GetMapping
	public String list(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("tags", this.tagService.search(keyword));
		model.addAttribute("keyword", keyword);
		return "tags/list";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		TagResponse tag = this.tagService.findById(id);
		model.addAttribute("tag", tag);
		model.addAttribute("tickets",
				this.ticketService.search(new TicketSearchCriteria(null, null, null, null, null, id)));
		model.addAttribute("assignableTickets", this.ticketService.findAll()
			.stream()
			.filter(TicketResponse::editable)
			.filter((ticket) -> ticket.tags().stream().noneMatch((attached) -> attached.id().equals(id)))
			.toList());
		return "tags/detail";
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("form", new TagForm());
		model.addAttribute("editing", false);
		return FORM_VIEW;
	}

	@PostMapping
	public String create(@ModelAttribute("form") TagForm form, BindingResult result, Model model,
			RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				TagResponse tag = this.tagService.create(form.toRequest());
				redirect.addFlashAttribute("success", "Le tag « " + tag.name() + " » a été créé.");
				return "redirect:/tags/" + tag.id();
			}
			catch (ConstraintViolationException ex) {
				FormSupport.reject(result, ex);
			}
			catch (DuplicateResourceException ex) {
				FormSupport.reject(result, "name", ex);
			}
		}
		model.addAttribute("editing", false);
		return FORM_VIEW;
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		model.addAttribute("form", TagForm.from(this.tagService.findById(id)));
		return editView(model, id);
	}

	@PostMapping("/{id}/edit")
	public String update(@PathVariable Long id, @ModelAttribute("form") TagForm form, BindingResult result,
			Model model, RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				this.tagService.update(id, form.toRequest());
				redirect.addFlashAttribute("success", "Le tag a été modifié.");
				return "redirect:/tags/" + id;
			}
			catch (ConstraintViolationException ex) {
				FormSupport.reject(result, ex);
			}
			catch (DuplicateResourceException ex) {
				FormSupport.reject(result, "name", ex);
			}
		}
		return editView(model, id);
	}

	private static String editView(Model model, Long id) {
		model.addAttribute("editing", true);
		model.addAttribute("tagId", id);
		return FORM_VIEW;
	}

	/** A refusal (tag still used by tickets) is reported by {@link WebAdvice}. */
	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirect) {
		this.tagService.delete(id);
		redirect.addFlashAttribute("success", "Le tag a été supprimé.");
		return "redirect:/tags";
	}

	// --- Tickets of a tag ---

	@PostMapping("/{id}/tickets")
	public String addTicket(@PathVariable Long id, @RequestParam Long ticketId, RedirectAttributes redirect) {
		this.ticketService.addTag(ticketId, id);
		redirect.addFlashAttribute("success", "Le tag a été ajouté au ticket n°" + ticketId + ".");
		return "redirect:/tags/" + id;
	}

	@PostMapping("/{id}/tickets/{ticketId}/remove")
	public String removeTicket(@PathVariable Long id, @PathVariable Long ticketId, RedirectAttributes redirect) {
		this.ticketService.removeTag(ticketId, id);
		redirect.addFlashAttribute("success", "Le tag a été retiré du ticket n°" + ticketId + ".");
		return "redirect:/tags/" + id;
	}

}
