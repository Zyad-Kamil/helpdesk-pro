package com.helpdeskpro.dto;

import com.helpdeskpro.entity.TechnicianProfile;

public record TechnicianProfileResponse(Long id, Long technicianId, String technicianName, String phone,
		String specialization, String biography) {

	public static TechnicianProfileResponse from(TechnicianProfile profile) {
		return new TechnicianProfileResponse(profile.getId(), profile.getTechnician().getId(),
				profile.getTechnician().getFirstName() + " " + profile.getTechnician().getLastName(),
				profile.getPhone(), profile.getSpecialization(), profile.getBiography());
	}

}
