package com.helpdeskpro.service;

import java.util.List;

import com.helpdeskpro.dto.TagRequest;
import com.helpdeskpro.dto.TagResponse;
import com.helpdeskpro.entity.Tag;
import com.helpdeskpro.exception.BusinessRuleException;
import com.helpdeskpro.exception.DuplicateResourceException;
import com.helpdeskpro.exception.ResourceNotFoundException;
import com.helpdeskpro.repository.TagRepository;
import com.helpdeskpro.repository.TicketRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.helpdeskpro.service.TextNormalizer.likePattern;
import static com.helpdeskpro.service.TextNormalizer.singleLine;

@Service
@Validated
@Transactional
public class TagService {

	private final TagRepository tagRepository;

	private final TicketRepository ticketRepository;

	public TagService(TagRepository tagRepository, TicketRepository ticketRepository) {
		this.tagRepository = tagRepository;
		this.ticketRepository = ticketRepository;
	}

	public TagResponse create(@Valid TagRequest request) {
		String name = singleLine(request.name());
		if (this.tagRepository.existsByNameIgnoreCase(name)) {
			throw duplicate(name);
		}
		return TagResponse.from(this.tagRepository.saveAndFlush(new Tag(name)));
	}

	public TagResponse update(Long id, @Valid TagRequest request) {
		Tag tag = find(id);
		String name = singleLine(request.name());
		if (this.tagRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw duplicate(name);
		}
		tag.setName(name);
		return TagResponse.from(this.tagRepository.saveAndFlush(tag));
	}

	/** Refused while at least one ticket uses the tag. */
	public void delete(Long id) {
		Tag tag = find(id);
		long usages = this.ticketRepository.countByTagsId(id);
		if (usages > 0) {
			throw new BusinessRuleException("Le tag « " + tag.getName() + " » ne peut pas être supprimé : il est utilisé par "
					+ usages + " ticket(s).");
		}
		this.tagRepository.delete(tag);
	}

	@Transactional(readOnly = true)
	public TagResponse findById(Long id) {
		return TagResponse.from(find(id));
	}

	@Transactional(readOnly = true)
	public List<TagResponse> findAll() {
		return this.tagRepository.findAll(Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id")))
			.stream()
			.map(TagResponse::from)
			.toList();
	}

	/** Case-insensitive search on the name; a blank keyword returns everything. */
	@Transactional(readOnly = true)
	public List<TagResponse> search(String keyword) {
		String pattern = likePattern(keyword);
		return (pattern == null) ? findAll() : this.tagRepository.search(pattern).stream().map(TagResponse::from).toList();
	}

	private Tag find(Long id) {
		return this.tagRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tag", id));
	}

	private static DuplicateResourceException duplicate(String name) {
		return new DuplicateResourceException("Un tag nommé « " + name + " » existe déjà (sans distinction de casse).");
	}

}
