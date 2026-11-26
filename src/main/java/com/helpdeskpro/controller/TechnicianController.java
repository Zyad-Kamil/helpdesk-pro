package com.helpdeskpro.controller;

import java.util.Objects;

import com.helpdeskpro.controller.form.TechnicianForm;
import com.helpdeskpro.dto.TechnicianResponse;
import com.helpdeskpro.dto.TicketResponse;
import com.helpdeskpro.dto.TicketSearchCriteria;
import com.helpdeskpro.exception.DuplicateResourceException;
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
@RequestMapping("/technicians")
public class TechnicianController {

	private static final String FORM_VIEW = "technicians/form";

	private final TechnicianService technicianService;

	private final TicketService ticketService;

	public TechnicianController(TechnicianService technicianService, TicketService ticketService) {
		this.technicianService = technicianService;
		this.ticketService = ticketService;
	}

	@GetMapping
	public String list(@RequestParam(required = false) String keyword, @RequestParam(required = false) Boolean active,
			Model model) {
		model.addAttribute("technicians", this.technicianService.search(keyword, active));
		model.addAttribute("keyword", keyword);
		model.addAttribute("active", active);
		return "technicians/list";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		TechnicianResponse technician = this.technicianService.findById(id);
		model.addAttribute("technician", technician);
		model.addAttribute("tickets",
				this.ticketService.search(new TicketSearchCriteria(null, null, null, id, null, null)));
		if (technician.active()) {
			model.addAttribute("assignableTickets", this.ticketService.findAll()
				.stream()
				.filter(TicketResponse::technicianChangeable)
				.filter((ticket) -> !Objects.equals(ticket.technicianId(), id))
				.toList());
		}
		return "technicians/detail";
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("form", new TechnicianForm());
		model.addAttribute("editing", false);
		return FORM_VIEW;
	}

	@PostMapping
	public String create(@ModelAttribute("form") TechnicianForm form, BindingResult result, Model model,
			RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				TechnicianResponse technician = this.technicianService.create(form.toRequest());
				redirect.addFlashAttribute("success", "Le technicien " + technician.firstName() + " "
						+ technician.lastName() + " a été créé.");
				return "redirect:/technicians/" + technician.id();
			}
			catch (ConstraintViolationException ex) {
				FormSupport.reject(result, ex);
			}
			catch (DuplicateResourceException ex) {
				FormSupport.reject(result, "email", ex);
			}
		}
		model.addAttribute("editing", false);
		return FORM_VIEW;
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		model.addAttribute("form", TechnicianForm.from(this.technicianService.findById(id)));
		return editView(model, id);
	}

	@PostMapping("/{id}/edit")
	public String update(@PathVariable Long id, @ModelAttribute("form") TechnicianForm form, BindingResult result,
			Model model, RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				this.technicianService.update(id, form.toRequest());
				redirect.addFlashAttribute("success", "Le technicien a été modifié.");
				return "redirect:/technicians/" + id;
			}
			catch (ConstraintViolationException ex) {
				FormSupport.reject(result, ex);
			}
			catch (DuplicateResourceException ex) {
				FormSupport.reject(result, "email", ex);
			}
		}
		return editView(model, id);
	}

	private static String editView(Model model, Long id) {
		model.addAttribute("editing", true);
		model.addAttribute("technicianId", id);
		return FORM_VIEW;
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirect) {
		this.technicianService.delete(id);
		redirect.addFlashAttribute("success", "Le technicien a été supprimé.");
		return "redirect:/technicians";
	}

	@PostMapping("/{id}/active")
	public String setActive(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes redirect) {
		this.technicianService.setActive(id, active);
		redirect.addFlashAttribute("success", active ? "Le technicien a été activé." : "Le technicien a été désactivé.");
		return "redirect:/technicians/" + id;
	}

	// --- Tickets of a technician ---

	@PostMapping("/{id}/tickets")
	public String assignTicket(@PathVariable Long id, @RequestParam Long ticketId, RedirectAttributes redirect) {
		this.ticketService.assignTechnician(ticketId, id);
		redirect.addFlashAttribute("success", "Le ticket n°" + ticketId + " a été affecté au technicien.");
		return "redirect:/technicians/" + id;
	}

	@PostMapping("/{id}/tickets/{ticketId}/remove")
	public String unassignTicket(@PathVariable Long id, @PathVariable Long ticketId, RedirectAttributes redirect) {
		if (Objects.equals(this.ticketService.findById(ticketId).technicianId(), id)) {
			this.ticketService.unassignTechnician(ticketId);
			redirect.addFlashAttribute("success", "Le ticket n°" + ticketId + " a été retiré du technicien.");
		}
		else {
			redirect.addFlashAttribute("error", "Le ticket n°" + ticketId + " n'est pas affecté à ce technicien.");
		}
		return "redirect:/technicians/" + id;
	}

}
