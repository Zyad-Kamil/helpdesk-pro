package com.helpdeskpro.service;

import com.helpdeskpro.dto.CategoryRequest;
import com.helpdeskpro.dto.CategoryResponse;
import com.helpdeskpro.dto.TagRequest;
import com.helpdeskpro.dto.TagResponse;
import com.helpdeskpro.exception.DuplicateResourceException;
import com.helpdeskpro.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Categories and tags. */
@ServiceTest
class ReferenceDataServiceTests extends AbstractServiceTests {

	@Test
	void categoryCrud() {
		CategoryResponse created = this.categoryService.create(new CategoryRequest("  Hard   ware ", "  Physical equipment "));
		assertThat(created.name()).isEqualTo("Hard ware");
		assertThat(created.description()).isEqualTo("Physical equipment");

		CategoryResponse updated = this.categoryService.update(created.id(), new CategoryRequest("Hardware", "   "));
		assertThat(updated.name()).isEqualTo("Hardware");
		assertThat(updated.description()).isNull();
		assertThat(this.categoryService.findById(created.id())).isEqualTo(updated);

		this.categoryService.delete(created.id());
		assertThat(this.categoryService.findAll()).isEmpty();
		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> this.categoryService.findById(created.id()))
			.withMessageContaining(String.valueOf(created.id()));
	}

	@Test
	void categoryNamesAreUniqueIgnoringCase() {
		CategoryResponse hardware = newCategory("Hardware");
		CategoryResponse software = newCategory("Software");

		assertThatExceptionOfType(DuplicateResourceException.class)
			.isThrownBy(() -> this.categoryService.create(new CategoryRequest("  HARDWARE ", null)))
			.withMessageContaining("HARDWARE");
		assertThatExceptionOfType(DuplicateResourceException.class)
			.isThrownBy(() -> this.categoryService.update(software.id(), new CategoryRequest("hardware", null)));
		// Renaming a category to a different case of its own name is not a duplicate
		assertThat(this.categoryService.update(hardware.id(), new CategoryRequest("HARDWARE", null)).name())
			.isEqualTo("HARDWARE");
	}

	@Test
	void categorySearchIsCaseInsensitiveOnNameAndDescriptionAndSorted() {
		this.categoryService.create(new CategoryRequest("Software", "Applications and licences"));
		this.categoryService.create(new CategoryRequest("hardware", "Physical equipment"));
		this.categoryService.create(new CategoryRequest("Access", null));

		assertThat(this.categoryService.findAll()).extracting(CategoryResponse::name)
			.containsExactly("Access", "hardware", "Software");
		assertThat(this.categoryService.search("WARE")).extracting(CategoryResponse::name)
			.containsExactly("hardware", "Software");
		assertThat(this.categoryService.search("licence")).extracting(CategoryResponse::name).containsExactly("Software");
		assertThat(this.categoryService.search("  ")).hasSize(3);
		assertThat(this.categoryService.search("zzz")).isEmpty();
	}

	@Test
	void invalidCategoryIsRejected() {
		assertThatExceptionOfType(ConstraintViolationException.class)
			.isThrownBy(() -> this.categoryService.create(new CategoryRequest(" ", null)));
		assertThatExceptionOfType(ConstraintViolationException.class)
			.isThrownBy(() -> this.categoryService.create(new CategoryRequest("x".repeat(81), null)));
	}

	@Test
	void tagCrudAndUniquenessIgnoringCase() {
		TagResponse created = this.tagService.create(new TagRequest("  Very   urgent "));
		assertThat(created.name()).isEqualTo("Very urgent");
		TagResponse other = newTag("network");

		assertThatExceptionOfType(DuplicateResourceException.class)
			.isThrownBy(() -> this.tagService.create(new TagRequest("VERY URGENT")));
		assertThatExceptionOfType(DuplicateResourceException.class)
			.isThrownBy(() -> this.tagService.update(other.id(), new TagRequest("very urgent")));

		assertThat(this.tagService.update(created.id(), new TagRequest("Urgent")).name()).isEqualTo("Urgent");
		this.tagService.delete(created.id());
		assertThat(this.tagService.findAll()).extracting(TagResponse::name).containsExactly("network");
		assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.tagService.findById(created.id()));
		assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> this.tagService.create(new TagRequest("")));
	}

	@Test
	void tagSearchIsCaseInsensitive() {
		newTag("Network");
		newTag("urgent");
		newTag("100% sure");

		assertThat(this.tagService.search("NET")).extracting(TagResponse::name).containsExactly("Network");
		assertThat(this.tagService.search("%")).extracting(TagResponse::name).containsExactly("100% sure");
		assertThat(this.tagService.search("_")).isEmpty();
		assertThat(this.tagService.search(null)).hasSize(3);
	}

}
