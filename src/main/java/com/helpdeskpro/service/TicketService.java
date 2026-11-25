package com.helpdeskpro.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.helpdeskpro.dto.TicketRequest;
import com.helpdeskpro.dto.TicketResponse;
import com.helpdeskpro.dto.TicketSearchCriteria;
import com.helpdeskpro.dto.TicketStatusRequest;
import com.helpdeskpro.entity.Category;
import com.helpdeskpro.entity.Tag;
import com.helpdeskpro.entity.Technician;
import com.helpdeskpro.entity.Ticket;
import com.helpdeskpro.entity.TicketStatus;
import com.helpdeskpro.exception.BusinessRuleException;
import com.helpdeskpro.exception.ResourceNotFoundException;
import com.helpdeskpro.repository.CategoryRepository;
import com.helpdeskpro.repository.TagRepository;
import com.helpdeskpro.repository.TechnicianRepository;
import com.helpdeskpro.repository.TicketRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.helpdeskpro.service.TextNormalizer.email;
import static com.helpdeskpro.service.TextNormalizer.likePattern;
import static com.helpdeskpro.service.TextNormalizer.singleLine;
import static com.helpdeskpro.service.TextNormalizer.text;

/**
 * Ticket life cycle and its relationships. This service is the only way to change a ticket's status,
 * resolution and relationships: it enforces the business rules that the entity setters do not.
 */
@Service
@Validated
@Transactional
public class TicketService {

	private static final Map<TicketStatus, TicketStatus> NEXT_STATUS = new EnumMap<>(
			Map.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED,
					TicketStatus.RESOLVED, TicketStatus.CLOSED));

	private static final Set<TicketStatus> ACTIVE = Set.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);

	private final TicketRepository ticketRepository;

	private final TechnicianRepository technicianRepository;

	private final CategoryRepository categoryRepository;

	private final TagRepository tagRepository;

	private final Clock clock;

	public TicketService(TicketRepository ticketRepository, TechnicianRepository technicianRepository,
			CategoryRepository categoryRepository, TagRepository tagRepository, Clock clock) {
		this.ticketRepository = ticketRepository;
		this.technicianRepository = technicianRepository;
		this.categoryRepository = categoryRepository;
		this.tagRepository = tagRepository;
		this.clock = clock;
	}

	// --- CRUD and search ---

	/** Creates a ticket, always {@code OPEN}; the priority defaults to {@code MEDIUM}. */
	public TicketResponse create(@Valid TicketRequest request) {
		Ticket ticket = new Ticket(singleLine(request.title()), text(request.description()),
				singleLine(request.requesterName()), email(request.requesterEmail()));
		if (request.priority() != null) {
			ticket.setPriority(request.priority());
		}
		return save(ticket);
	}

	/** Edits the descriptive fields only (and the priority if given); refused on a closed ticket. */
	public TicketResponse update(Long id, @Valid TicketRequest request) {
		Ticket ticket = findModifiable(id);
		ticket.setTitle(singleLine(request.title()));
		ticket.setDescription(text(request.description()));
		ticket.setRequesterName(singleLine(request.requesterName()));
		ticket.setRequesterEmail(email(request.requesterEmail()));
		if (request.priority() != null) {
			ticket.setPriority(request.priority());
		}
		return save(ticket);
	}

	/** Deletes the ticket whatever its status; technician, category and tags are kept. */
	public void delete(Long id) {
		Ticket ticket = find(id);
		ticket.assignTechnician(null);
		ticket.assignCategory(null);
		new ArrayList<>(ticket.getTags()).forEach(ticket::removeTag);
		this.ticketRepository.delete(ticket);
	}

	@Transactional(readOnly = true)
	public TicketResponse findById(Long id) {
		return toResponse(find(id));
	}

	@Transactional(readOnly = true)
	public List<TicketResponse> findAll() {
		return search(TicketSearchCriteria.none());
	}

	/** Case-insensitive keyword (title, description, requester) combined with the optional filters. */
	@Transactional(readOnly = true)
	public List<TicketResponse> search(TicketSearchCriteria criteria) {
		return this.ticketRepository
			.search(likePattern(criteria.keyword()), criteria.status(), criteria.priority(), criteria.technicianId(),
					criteria.categoryId(), criteria.tagId())
			.stream()
			.map(this::toResponse)
			.toList();
	}

	/** Open or in-progress tickets past their due date, the most overdue first. */
	@Transactional(readOnly = true)
	public List<TicketResponse> findOverdue() {
		return this.ticketRepository.findByStatusIn(ACTIVE)
			.stream()
			.map(this::toResponse)
			.filter(TicketResponse::overdue)
			.sorted(Comparator.comparing(TicketResponse::dueAt))
			.toList();
	}

	// --- Status ---

	/**
	 * Applies the only allowed transitions: OPEN to IN_PROGRESS (needs an active technician),
	 * IN_PROGRESS to RESOLVED (needs a resolution; sets {@code resolvedAt}) and RESOLVED to CLOSED.
	 */
	public TicketResponse changeStatus(Long id, @Valid TicketStatusRequest request) {
		Ticket ticket = findModifiable(id);
		TicketStatus target = request.status();
		if (NEXT_STATUS.get(ticket.getStatus()) != target) {
			throw new BusinessRuleException("Transition interdite pour le ticket " + id + " : " + ticket.getStatus()
					+ " vers " + target + ".");
		}
		if (target == TicketStatus.IN_PROGRESS) {
			if (ticket.getTechnician() == null || !ticket.getTechnician().isActive()) {
				throw new BusinessRuleException("Le ticket " + id
						+ " ne peut passer en cours que si un technicien actif lui est affecté.");
			}
		}
		else if (target == TicketStatus.RESOLVED) {
			String resolution = text(request.resolution());
			if (resolution == null) {
				throw new BusinessRuleException("Une solution non vide est obligatoire pour résoudre le ticket " + id + ".");
			}
			ticket.setResolution(resolution);
			ticket.setResolvedAt(LocalDateTime.now(this.clock).truncatedTo(ChronoUnit.MICROS));
		}
		ticket.setStatus(target);
		return save(ticket);
	}

	// --- Technician ---

	/** Assigns or reassigns an active technician; only for an open or in-progress ticket. */
	public TicketResponse assignTechnician(Long ticketId, Long technicianId) {
		Ticket ticket = findModifiable(ticketId);
		if (!ACTIVE.contains(ticket.getStatus())) {
			throw new BusinessRuleException("Le technicien du ticket " + ticketId
					+ " ne peut plus être changé : le ticket est " + ticket.getStatus() + ".");
		}
		Technician technician = this.technicianRepository.findById(technicianId)
			.orElseThrow(() -> new ResourceNotFoundException("Technicien", technicianId));
		if (!technician.isActive()) {
			throw new BusinessRuleException("Le technicien " + technician.getFirstName() + " " + technician.getLastName()
					+ " est inactif : il ne peut pas recevoir d'affectation.");
		}
		ticket.assignTechnician(technician);
		return save(ticket);
	}

	/** Removes the technician; only allowed while the ticket is OPEN. */
	public TicketResponse unassignTechnician(Long ticketId) {
		Ticket ticket = findModifiable(ticketId);
		if (ticket.getStatus() != TicketStatus.OPEN) {
			throw new BusinessRuleException("La désaffectation n'est possible que pour un ticket OPEN : le ticket "
					+ ticketId + " est " + ticket.getStatus() + ".");
		}
		ticket.assignTechnician(null);
		return save(ticket);
	}

	// --- Category ---

	/** Associates the category, replacing the current one if any. */
	public TicketResponse assignCategory(Long ticketId, Long categoryId) {
		Ticket ticket = findModifiable(ticketId);
		Category category = this.categoryRepository.findById(categoryId)
			.orElseThrow(() -> new ResourceNotFoundException("Catégorie", categoryId));
		ticket.assignCategory(category);
		return save(ticket);
	}

	public TicketResponse removeCategory(Long ticketId) {
		Ticket ticket = findModifiable(ticketId);
		ticket.assignCategory(null);
		return save(ticket);
	}

	// --- Tags ---

	public TicketResponse addTag(Long ticketId, Long tagId) {
		Ticket ticket = findModifiable(ticketId);
		ticket.addTag(findTag(tagId));
		return save(ticket);
	}

	public TicketResponse removeTag(Long ticketId, Long tagId) {
		Ticket ticket = findModifiable(ticketId);
		ticket.removeTag(findTag(tagId));
		return save(ticket);
	}

	// --- Internals ---

	private Ticket find(Long id) {
		return this.ticketRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
	}

	/** A closed ticket can no longer be modified, by any operation. */
	private Ticket findModifiable(Long id) {
		Ticket ticket = find(id);
		if (ticket.getStatus() == TicketStatus.CLOSED) {
			throw new BusinessRuleException("Le ticket " + id + " est clôturé : il ne peut plus être modifié.");
		}
		return ticket;
	}

	private Tag findTag(Long tagId) {
		return this.tagRepository.findById(tagId).orElseThrow(() -> new ResourceNotFoundException("Tag", tagId));
	}

	/** Flushes first, so that the response shows the generated and updated dates. */
	private TicketResponse save(Ticket ticket) {
		return toResponse(this.ticketRepository.saveAndFlush(ticket));
	}

	private TicketResponse toResponse(Ticket ticket) {
		LocalDateTime dueAt = ticket.getCreatedAt().plus(ticket.getPriority().getResolutionTarget());
		boolean active = ACTIVE.contains(ticket.getStatus());
		boolean overdue = active && LocalDateTime.now(this.clock).isAfter(dueAt);
		Boolean withinTarget = (!active && ticket.getResolvedAt() != null) ? !ticket.getResolvedAt().isAfter(dueAt)
				: null;
		return TicketResponse.from(ticket, dueAt, overdue, withinTarget);
	}

}
