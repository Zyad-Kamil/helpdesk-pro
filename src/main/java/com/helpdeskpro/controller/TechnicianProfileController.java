package com.helpdeskpro.controller;

import com.helpdeskpro.controller.form.ProfileForm;
import com.helpdeskpro.service.TechnicianProfileService;
import com.helpdeskpro.service.TechnicianService;
import jakarta.validation.ConstraintViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Profiles are listed under {@code /profiles} and managed through their technician. */
@Controller
public class TechnicianProfileController {

	private static final String FORM_VIEW = "profiles/form";

	private final TechnicianProfileService profileService;

	private final TechnicianService technicianService;

	public TechnicianProfileController(TechnicianProfileService profileService,
			TechnicianService technicianService) {
		this.profileService = profileService;
		this.technicianService = technicianService;
	}

	@GetMapping("/profiles")
	public String list(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("profiles", this.profileService.search(keyword));
		model.addAttribute("keyword", keyword);
		return "profiles/list";
	}

	@GetMapping("/technicians/{technicianId}/profile/new")
	public String newForm(@PathVariable Long technicianId, Model model) {
		model.addAttribute("form", new ProfileForm());
		return formView(model, technicianId, false);
	}

	@PostMapping("/technicians/{technicianId}/profile")
	public String create(@PathVariable Long technicianId, @ModelAttribute("form") ProfileForm form,
			BindingResult result, Model model, RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				this.profileService.create(technicianId, form.toRequest());
				redirect.addFlashAttribute("success", "Le profil a été créé.");
				return "redirect:/technicians/" + technicianId;
			}
			catch (ConstraintViolationException ex) {
				FormSupport.reject(result, ex);
			}
		}
		return formView(model, technicianId, false);
	}

	@GetMapping("/technicians/{technicianId}/profile/edit")
	public String editForm(@PathVariable Long technicianId, Model model) {
		model.addAttribute("form", ProfileForm.from(this.profileService.findByTechnicianId(technicianId)));
		return formView(model, technicianId, true);
	}

	@PostMapping("/technicians/{technicianId}/profile/edit")
	public String update(@PathVariable Long technicianId, @ModelAttribute("form") ProfileForm form,
			BindingResult result, Model model, RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				this.profileService.update(technicianId, form.toRequest());
				redirect.addFlashAttribute("success", "Le profil a été modifié.");
				return "redirect:/technicians/" + technicianId;
			}
			catch (ConstraintViolationException ex) {
				FormSupport.reject(result, ex);
			}
		}
		return formView(model, technicianId, true);
	}

	@PostMapping("/technicians/{technicianId}/profile/delete")
	public String delete(@PathVariable Long technicianId, RedirectAttributes redirect) {
		this.profileService.delete(technicianId);
		redirect.addFlashAttribute("success", "Le profil a été supprimé.");
		return "redirect:/technicians/" + technicianId;
	}

	private String formView(Model model, Long technicianId, boolean editing) {
		model.addAttribute("technician", this.technicianService.findById(technicianId));
		model.addAttribute("editing", editing);
		return FORM_VIEW;
	}

}
