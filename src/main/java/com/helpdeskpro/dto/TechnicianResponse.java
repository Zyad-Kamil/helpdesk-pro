package com.helpdeskpro.dto;

import com.helpdeskpro.entity.Technician;

/** Read model of a technician, with its profile (or {@code null}). */
public record TechnicianResponse(Long id, String firstName, String lastName, String email, boolean active,
		TechnicianProfileResponse profile) {

	public static TechnicianResponse from(Technician technician) {
		return new TechnicianResponse(technician.getId(), technician.getFirstName(), technician.getLastName(),
				technician.getEmail(), technician.isActive(),
				(technician.getProfile() != null) ? TechnicianProfileResponse.from(technician.getProfile()) : null);
	}

}
