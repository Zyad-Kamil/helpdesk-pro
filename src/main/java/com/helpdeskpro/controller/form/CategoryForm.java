package com.helpdeskpro.controller.form;

import com.helpdeskpro.dto.CategoryRequest;
import com.helpdeskpro.dto.CategoryResponse;

public class CategoryForm {

	private String name;

	private String description;

	public static CategoryForm from(CategoryResponse category) {
		CategoryForm form = new CategoryForm();
		form.name = category.name();
		form.description = category.description();
		return form;
	}

	public CategoryRequest toRequest() {
		return new CategoryRequest(this.name, this.description);
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
