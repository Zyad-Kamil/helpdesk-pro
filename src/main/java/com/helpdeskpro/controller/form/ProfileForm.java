package com.helpdeskpro.controller.form;

import com.helpdeskpro.dto.TechnicianProfileRequest;
import com.helpdeskpro.dto.TechnicianProfileResponse;

public class ProfileForm {

	private String phone;

	private String specialization;

	private String biography;

	public static ProfileForm from(TechnicianProfileResponse profile) {
		ProfileForm form = new ProfileForm();
		form.phone = profile.phone();
		form.specialization = profile.specialization();
		form.biography = profile.biography();
		return form;
	}

	public TechnicianProfileRequest toRequest() {
		return new TechnicianProfileRequest(this.phone, this.specialization, this.biography);
	}

	public String getPhone() {
		return this.phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getSpecialization() {
		return this.specialization;
	}

	public void setSpecialization(String specialization) {
		this.specialization = specialization;
	}

	public String getBiography() {
		return this.biography;
	}

	public void setBiography(String biography) {
		this.biography = biography;
	}

}
