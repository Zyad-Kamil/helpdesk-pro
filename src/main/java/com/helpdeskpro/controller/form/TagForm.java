package com.helpdeskpro.controller.form;

import com.helpdeskpro.dto.TagRequest;
import com.helpdeskpro.dto.TagResponse;

public class TagForm {

	private String name;

	public static TagForm from(TagResponse tag) {
		TagForm form = new TagForm();
		form.name = tag.name();
		return form;
	}

	public TagRequest toRequest() {
		return new TagRequest(this.name);
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
