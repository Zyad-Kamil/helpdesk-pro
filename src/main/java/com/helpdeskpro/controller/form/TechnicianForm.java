package com.helpdeskpro.controller.form;

import com.helpdeskpro.dto.TechnicianRequest;
import com.helpdeskpro.dto.TechnicianResponse;

public class TechnicianForm {

	private String firstName;

	private String lastName;

	private String email;

	public static TechnicianForm from(TechnicianResponse technician) {
		TechnicianForm form = new TechnicianForm();
		form.firstName = technician.firstName();
		form.lastName = technician.lastName();
		form.email = technician.email();
		return form;
	}

	public TechnicianRequest toRequest() {
		return new TechnicianRequest(this.firstName, this.lastName, this.email);
	}

	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
