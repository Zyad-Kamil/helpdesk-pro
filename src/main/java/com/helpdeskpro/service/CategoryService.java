package com.helpdeskpro.service;

import java.util.List;

import com.helpdeskpro.dto.CategoryRequest;
import com.helpdeskpro.dto.CategoryResponse;
import com.helpdeskpro.entity.Category;
import com.helpdeskpro.exception.BusinessRuleException;
import com.helpdeskpro.exception.DuplicateResourceException;
import com.helpdeskpro.exception.ResourceNotFoundException;
import com.helpdeskpro.repository.CategoryRepository;
import com.helpdeskpro.repository.TicketRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.helpdeskpro.service.TextNormalizer.likePattern;
import static com.helpdeskpro.service.TextNormalizer.singleLine;
import static com.helpdeskpro.service.TextNormalizer.text;

@Service
@Validated
@Transactional
public class CategoryService {

	private final CategoryRepository categoryRepository;

	private final TicketRepository ticketRepository;

	public CategoryService(CategoryRepository categoryRepository, TicketRepository ticketRepository) {
		this.categoryRepository = categoryRepository;
		this.ticketRepository = ticketRepository;
	}

	public CategoryResponse create(@Valid CategoryRequest request) {
		String name = singleLine(request.name());
		if (this.categoryRepository.existsByNameIgnoreCase(name)) {
			throw duplicate(name);
		}
		return CategoryResponse.from(this.categoryRepository.saveAndFlush(new Category(name, text(request.description()))));
	}

	public CategoryResponse update(Long id, @Valid CategoryRequest request) {
		Category category = find(id);
		String name = singleLine(request.name());
		if (this.categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw duplicate(name);
		}
		category.setName(name);
		category.setDescription(text(request.description()));
		return CategoryResponse.from(this.categoryRepository.saveAndFlush(category));
	}

	/** Refused while at least one ticket uses the category. */
	public void delete(Long id) {
		Category category = find(id);
		long usages = this.ticketRepository.countByCategoryId(id);
		if (usages > 0) {
			throw new BusinessRuleException("La catégorie « " + category.getName() + " » ne peut pas être supprimée : elle est utilisée par "
					+ usages + " ticket(s).");
		}
		this.categoryRepository.delete(category);
	}

	@Transactional(readOnly = true)
	public CategoryResponse findById(Long id) {
		return CategoryResponse.from(find(id));
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> findAll() {
		return this.categoryRepository.findAll(Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id")))
			.stream()
			.map(CategoryResponse::from)
			.toList();
	}

	/** Case-insensitive search on the name; a blank keyword returns everything. */
	@Transactional(readOnly = true)
	public List<CategoryResponse> search(String keyword) {
		String pattern = likePattern(keyword);
		return (pattern == null) ? findAll() : this.categoryRepository.search(pattern).stream().map(CategoryResponse::from).toList();
	}

	private Category find(Long id) {
		return this.categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));
	}

	private static DuplicateResourceException duplicate(String name) {
		return new DuplicateResourceException("Une catégorie nommée « " + name + " » existe déjà (sans distinction de casse).");
	}

}
