package com.helpdeskpro.controller;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.helpdeskpro.controller.form.TicketForm;
import com.helpdeskpro.dto.TagResponse;
import com.helpdeskpro.dto.TicketResponse;
import com.helpdeskpro.dto.TicketSearchCriteria;
import com.helpdeskpro.dto.TicketStatusRequest;
import com.helpdeskpro.entity.TicketPriority;
import com.helpdeskpro.entity.TicketStatus;
import com.helpdeskpro.exception.BusinessRuleException;
import com.helpdeskpro.service.CategoryService;
import com.helpdeskpro.service.TagService;
import com.helpdeskpro.service.TechnicianService;
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
@RequestMapping("/tickets")
public class TicketController {

	private static final String FORM_VIEW = "tickets/form";

	private final TicketService ticketService;

	private final TechnicianService technicianService;

	private final CategoryService categoryService;

	private final TagService tagService;

	public TicketController(TicketService ticketService, TechnicianService technicianService,
			CategoryService categoryService, TagService tagService) {
		this.ticketService = ticketService;
		this.technicianService = technicianService;
		this.categoryService = categoryService;
		this.tagService = tagService;
	}

	@GetMapping
	public String list(@RequestParam(required = false) String keyword, @RequestParam(required = false) TicketStatus status,
			@RequestParam(required = false) TicketPriority priority, @RequestParam(required = false) Long technicianId,
			@RequestParam(required = false) Long categoryId, @RequestParam(required = false) Long tagId,
			@RequestParam(required = false) Boolean overdue, Model model) {
		TicketSearchCriteria criteria = new TicketSearchCriteria(keyword, status, priority, technicianId, categoryId,
				tagId, overdue);
		model.addAttribute("tickets", this.ticketService.search(criteria));
		model.addAttribute("criteria", criteria);
		model.addAttribute("technicians", this.technicianService.findAll());
		model.addAttribute("categories", this.categoryService.findAll());
		model.addAttribute("tags", this.tagService.findAll());
		return "tickets/list";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		TicketResponse ticket = this.ticketService.findById(id);
		model.addAttribute("ticket", ticket);
		if (ticket.technicianChangeable()) {
			model.addAttribute("availableTechnicians", this.technicianService.search(null, true)
				.stream()
				.filter((technician) -> !technician.id().equals(ticket.technicianId()))
				.toList());
		}
		if (ticket.editable()) {
			model.addAttribute("availableCategories", this.categoryService.findAll()
				.stream()
				.filter((category) -> !category.id().equals(ticket.categoryId()))
				.toList());
			Set<Long> attached = ticket.tags().stream().map(TagResponse::id).collect(Collectors.toSet());
			model.addAttribute("availableTags",
					this.tagService.findAll().stream().filter((tag) -> !attached.contains(tag.id())).toList());
		}
		return "tickets/detail";
	}

	/** Kanban view: the tickets of the (optionally filtered) search, one column per status. */
	@GetMapping("/board")
	public String board(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) TicketPriority priority, @RequestParam(required = false) Long technicianId,
			@RequestParam(required = false) Long categoryId, Model model) {
		TicketSearchCriteria criteria = new TicketSearchCriteria(keyword, null, priority, technicianId, categoryId, null,
				null);
		Map<TicketStatus, List<TicketResponse>> columns = new EnumMap<>(TicketStatus.class);
		for (TicketStatus status : TicketStatus.values()) {
			columns.put(status, new ArrayList<>());
		}
		this.ticketService.search(criteria).forEach((ticket) -> columns.get(ticket.status()).add(ticket));
		model.addAttribute("columns", columns);
		model.addAttribute("criteria", criteria);
		model.addAttribute("technicians", this.technicianService.findAll());
		model.addAttribute("categories", this.categoryService.findAll());
		return "tickets/board";
	}

	// --- Create, edit, delete ---

	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("form", new TicketForm());
		model.addAttribute("editing", false);
		return FORM_VIEW;
	}

	@PostMapping
	public String create(@ModelAttribute("form") TicketForm form, BindingResult result, Model model,
			RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				TicketResponse ticket = this.ticketService.create(form.toRequest());
				redirect.addFlashAttribute("success", "Le ticket n°" + ticket.id() + " a été créé.");
				return "redirect:/tickets/" + ticket.id();
			}
			catch (ConstraintViolationException ex) {
				FormSupport.reject(result, ex);
			}
		}
		model.addAttribute("editing", false);
		return FORM_VIEW;
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		TicketResponse ticket = this.ticketService.findById(id);
		if (!ticket.editable()) {
			throw new BusinessRuleException("Le ticket n°" + id + " est clôturé : il ne peut plus être modifié.");
		}
		model.addAttribute("form", TicketForm.from(ticket));
		return editView(model, id);
	}

	@PostMapping("/{id}/edit")
	public String update(@PathVariable Long id, @ModelAttribute("form") TicketForm form, BindingResult result,
			Model model, RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				this.ticketService.update(id, form.toRequest());
				redirect.addFlashAttribute("success", "Le ticket n°" + id + " a été modifié.");
				return "redirect:/tickets/" + id;
			}
			catch (ConstraintViolationException ex) {
				FormSupport.reject(result, ex);
			}
		}
		return editView(model, id);
	}

	private static String editView(Model model, Long id) {
		model.addAttribute("editing", true);
		model.addAttribute("ticketId", id);
		return FORM_VIEW;
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirect) {
		this.ticketService.delete(id);
		redirect.addFlashAttribute("success", "Le ticket n°" + id + " a été supprimé.");
		return "redirect:/tickets";
	}

	// --- Status ---

	@PostMapping("/{id}/status")
	public String changeStatus(@PathVariable Long id, @RequestParam TicketStatus status,
			@RequestParam(required = false) String resolution, 
				@RequestParam(required = false) String from, RedirectAttributes redirect) {
		this.ticketService.changeStatus(id, new TicketStatusRequest(status, resolution));
		redirect.addFlashAttribute("success", "Le statut du ticket n°" + id + " a été mis à jour.");
		// The Kanban board sends its actions back to the board
		return "board".equals(from) ? "redirect:/tickets/board" : detailRedirect(id);
	}

	// --- Technician ---

	@PostMapping("/{id}/technician")
	public String assignTechnician(@PathVariable Long id, @RequestParam Long technicianId,
			RedirectAttributes redirect) {
		this.ticketService.assignTechnician(id, technicianId);
		redirect.addFlashAttribute("success", "Le technicien a été affecté au ticket n°" + id + ".");
		return detailRedirect(id);
	}

	@PostMapping("/{id}/technician/remove")
	public String unassignTechnician(@PathVariable Long id, RedirectAttributes redirect) {
		this.ticketService.unassignTechnician(id);
		redirect.addFlashAttribute("success", "Le technicien a été retiré du ticket n°" + id + ".");
		return detailRedirect(id);
	}

	// --- Category ---

	@PostMapping("/{id}/category")
	public String assignCategory(@PathVariable Long id, @RequestParam Long categoryId, RedirectAttributes redirect) {
		this.ticketService.assignCategory(id, categoryId);
		redirect.addFlashAttribute("success", "La catégorie du ticket n°" + id + " a été enregistrée.");
		return detailRedirect(id);
	}

	@PostMapping("/{id}/category/remove")
	public String removeCategory(@PathVariable Long id, RedirectAttributes redirect) {
		this.ticketService.removeCategory(id);
		redirect.addFlashAttribute("success", "La catégorie a été retirée du ticket n°" + id + ".");
		return detailRedirect(id);
	}

	// --- Tags ---

	@PostMapping("/{id}/tags")
	public String addTag(@PathVariable Long id, @RequestParam Long tagId, RedirectAttributes redirect) {
		this.ticketService.addTag(id, tagId);
		redirect.addFlashAttribute("success", "Le tag a été ajouté au ticket n°" + id + ".");
		return detailRedirect(id);
	}

	@PostMapping("/{id}/tags/{tagId}/remove")
	public String removeTag(@PathVariable Long id, @PathVariable Long tagId, RedirectAttributes redirect) {
		this.ticketService.removeTag(id, tagId);
		redirect.addFlashAttribute("success", "Le tag a été retiré du ticket n°" + id + ".");
		return detailRedirect(id);
	}

	private static String detailRedirect(Long id) {
		return "redirect:/tickets/" + id;
	}

}
