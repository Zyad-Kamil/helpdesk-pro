package com.helpdeskpro.controller;

import java.util.Objects;

import com.helpdeskpro.controller.form.CategoryForm;
import com.helpdeskpro.dto.CategoryResponse;
import com.helpdeskpro.dto.TicketResponse;
import com.helpdeskpro.dto.TicketSearchCriteria;
import com.helpdeskpro.exception.DuplicateResourceException;
import com.helpdeskpro.service.CategoryService;
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
@RequestMapping("/categories")
public class CategoryController {

	private static final String FORM_VIEW = "categories/form";

	private final CategoryService categoryService;

	private final TicketService ticketService;

	public CategoryController(CategoryService categoryService, TicketService ticketService) {
		this.categoryService = categoryService;
		this.ticketService = ticketService;
	}

	@GetMapping
	public String list(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("categories", this.categoryService.search(keyword));
		model.addAttribute("keyword", keyword);
		return "categories/list";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		CategoryResponse category = this.categoryService.findById(id);
		model.addAttribute("category", category);
		model.addAttribute("tickets",
				this.ticketService.search(new TicketSearchCriteria(null, null, null, null, id, null)));
		model.addAttribute("assignableTickets", this.ticketService.findAll()
			.stream()
			.filter(TicketResponse::editable)
			.filter((ticket) -> !Objects.equals(ticket.categoryId(), id))
			.toList());
		return "categories/detail";
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("form", new CategoryForm());
		model.addAttribute("editing", false);
		return FORM_VIEW;
	}

	@PostMapping
	public String create(@ModelAttribute("form") CategoryForm form, BindingResult result, Model model,
			RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				CategoryResponse category = this.categoryService.create(form.toRequest());
				redirect.addFlashAttribute("success", "La catégorie « " + category.name() + " » a été créée.");
				return "redirect:/categories/" + category.id();
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
		model.addAttribute("form", CategoryForm.from(this.categoryService.findById(id)));
		return editView(model, id);
	}

	@PostMapping("/{id}/edit")
	public String update(@PathVariable Long id, @ModelAttribute("form") CategoryForm form, BindingResult result,
			Model model, RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				this.categoryService.update(id, form.toRequest());
				redirect.addFlashAttribute("success", "La catégorie a été modifiée.");
				return "redirect:/categories/" + id;
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
		model.addAttribute("categoryId", id);
		return FORM_VIEW;
	}

	/** A refusal (category still used by tickets) is reported by {@link WebAdvice}. */
	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirect) {
		this.categoryService.delete(id);
		redirect.addFlashAttribute("success", "La catégorie a été supprimée.");
		return "redirect:/categories";
	}

	// --- Tickets of a category ---

	@PostMapping("/{id}/tickets")
	public String addTicket(@PathVariable Long id, @RequestParam Long ticketId, RedirectAttributes redirect) {
		this.ticketService.assignCategory(ticketId, id);
		redirect.addFlashAttribute("success", "Le ticket n°" + ticketId + " a été ajouté à la catégorie.");
		return "redirect:/categories/" + id;
	}

	@PostMapping("/{id}/tickets/{ticketId}/remove")
	public String removeTicket(@PathVariable Long id, @PathVariable Long ticketId, RedirectAttributes redirect) {
		if (Objects.equals(this.ticketService.findById(ticketId).categoryId(), id)) {
			this.ticketService.removeCategory(ticketId);
			redirect.addFlashAttribute("success", "Le ticket n°" + ticketId + " a été retiré de la catégorie.");
		}
		else {
			redirect.addFlashAttribute("error", "Le ticket n°" + ticketId + " n'appartient pas à cette catégorie.");
		}
		return "redirect:/categories/" + id;
	}

}
