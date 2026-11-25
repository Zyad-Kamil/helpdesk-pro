package com.helpdeskpro.service;

import java.util.List;

import com.helpdeskpro.dto.TechnicianRequest;
import com.helpdeskpro.dto.TechnicianResponse;
import com.helpdeskpro.entity.Technician;
import com.helpdeskpro.entity.TicketStatus;
import com.helpdeskpro.exception.BusinessRuleException;
import com.helpdeskpro.exception.DuplicateResourceException;
import com.helpdeskpro.exception.ResourceNotFoundException;
import com.helpdeskpro.repository.TechnicianRepository;
import com.helpdeskpro.repository.TicketRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.helpdeskpro.service.TextNormalizer.email;
import static com.helpdeskpro.service.TextNormalizer.likePattern;
import static com.helpdeskpro.service.TextNormalizer.singleLine;

@Service
@Validated
@Transactional
public class TechnicianService {

	private final TechnicianRepository technicianRepository;

	private final TicketRepository ticketRepository;

	public TechnicianService(TechnicianRepository technicianRepository, TicketRepository ticketRepository) {
		this.technicianRepository = technicianRepository;
		this.ticketRepository = ticketRepository;
	}

	/** Creates an active technician; the e-mail is stored in lower case. */
	public TechnicianResponse create(@Valid TechnicianRequest request) {
		String email = email(request.email());
		if (this.technicianRepository.existsByEmailIgnoreCase(email)) {
			throw duplicate(email);
		}
		Technician technician = new Technician(singleLine(request.firstName()), singleLine(request.lastName()), email);
		return TechnicianResponse.from(this.technicianRepository.saveAndFlush(technician));
	}

	public TechnicianResponse update(Long id, @Valid TechnicianRequest request) {
		Technician technician = find(id);
		String email = email(request.email());
		if (this.technicianRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
			throw duplicate(email);
		}
		technician.setFirstName(singleLine(request.firstName()));
		technician.setLastName(singleLine(request.lastName()));
		technician.setEmail(email);
		return TechnicianResponse.from(this.technicianRepository.saveAndFlush(technician));
	}

	/** Deactivation is refused while the technician has tickets in progress; activation is always allowed. */
	public TechnicianResponse setActive(Long id, boolean active) {
		Technician technician = find(id);
		if (!active && technician.isActive()
				&& this.ticketRepository.existsByTechnicianIdAndStatus(id, TicketStatus.IN_PROGRESS)) {
			throw new BusinessRuleException("Le technicien " + technician.getFirstName() + " " + technician.getLastName()
					+ " ne peut pas être désactivé : il a des tickets en cours.");
		}
		technician.setActive(active);
		return TechnicianResponse.from(this.technicianRepository.saveAndFlush(technician));
	}

	/** Refused while at least one ticket is assigned; the profile, if any, is deleted with the technician. */
	public void delete(Long id) {
		Technician technician = find(id);
		long tickets = this.ticketRepository.countByTechnicianId(id);
		if (tickets > 0) {
			throw new BusinessRuleException("Le technicien " + technician.getFirstName() + " " + technician.getLastName()
					+ " ne peut pas être supprimé : " + tickets + " ticket(s) lui sont affectés.");
		}
		this.technicianRepository.delete(technician);
	}

	@Transactional(readOnly = true)
	public TechnicianResponse findById(Long id) {
		return TechnicianResponse.from(find(id));
	}

	@Transactional(readOnly = true)
	public List<TechnicianResponse> findAll() {
		return search(null, null);
	}

	/**
	 * Case-insensitive search on first name, last name and e-mail.
	 *
	 * @param keyword blank means "no text filter"
	 * @param active {@code null} means "active and inactive"
	 */
	@Transactional(readOnly = true)
	public List<TechnicianResponse> search(String keyword, Boolean active) {
		return this.technicianRepository.search(likePattern(keyword), active)
			.stream()
			.map(TechnicianResponse::from)
			.toList();
	}

	private Technician find(Long id) {
		return this.technicianRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Technicien", id));
	}

	private static DuplicateResourceException duplicate(String email) {
		return new DuplicateResourceException("Un technicien avec l'e-mail « " + email + " » existe déjà.");
	}

}
