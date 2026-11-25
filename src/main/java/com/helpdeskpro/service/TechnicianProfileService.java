package com.helpdeskpro.service;

import java.util.List;

import com.helpdeskpro.dto.TechnicianProfileRequest;
import com.helpdeskpro.dto.TechnicianProfileResponse;
import com.helpdeskpro.entity.Technician;
import com.helpdeskpro.entity.TechnicianProfile;
import com.helpdeskpro.exception.BusinessRuleException;
import com.helpdeskpro.exception.ResourceNotFoundException;
import com.helpdeskpro.repository.TechnicianProfileRepository;
import com.helpdeskpro.repository.TechnicianRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.helpdeskpro.service.TextNormalizer.likePattern;
import static com.helpdeskpro.service.TextNormalizer.singleLine;
import static com.helpdeskpro.service.TextNormalizer.text;

/** The profile is always handled through its technician: there is at most one per technician. */
@Service
@Validated
@Transactional
public class TechnicianProfileService {

	private final TechnicianProfileRepository profileRepository;

	private final TechnicianRepository technicianRepository;

	public TechnicianProfileService(TechnicianProfileRepository profileRepository,
			TechnicianRepository technicianRepository) {
		this.profileRepository = profileRepository;
		this.technicianRepository = technicianRepository;
	}

	public TechnicianProfileResponse create(Long technicianId, @Valid TechnicianProfileRequest request) {
		Technician technician = findTechnician(technicianId);
		if (technician.getProfile() != null) {
			throw new BusinessRuleException("Le technicien " + technician.getFirstName() + " " + technician.getLastName()
					+ " a déjà un profil : modifiez-le ou supprimez-le d'abord.");
		}
		TechnicianProfile profile = new TechnicianProfile(null, null, null);
		apply(profile, request);
		technician.assignProfile(profile);
		return TechnicianProfileResponse.from(this.profileRepository.saveAndFlush(profile));
	}

	public TechnicianProfileResponse update(Long technicianId, @Valid TechnicianProfileRequest request) {
		TechnicianProfile profile = findProfile(technicianId);
		apply(profile, request);
		return TechnicianProfileResponse.from(this.profileRepository.saveAndFlush(profile));
	}

	public void delete(Long technicianId) {
		Technician technician = findTechnician(technicianId);
		if (technician.getProfile() == null) {
			throw new ResourceNotFoundException("Profil du technicien", technicianId);
		}
		technician.removeProfile();
		this.technicianRepository.flush();
	}

	@Transactional(readOnly = true)
	public TechnicianProfileResponse findByTechnicianId(Long technicianId) {
		return TechnicianProfileResponse.from(findProfile(technicianId));
	}

	@Transactional(readOnly = true)
	public TechnicianProfileResponse findById(Long id) {
		return TechnicianProfileResponse.from(
				this.profileRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Profil", id)));
	}

	@Transactional(readOnly = true)
	public List<TechnicianProfileResponse> findAll() {
		return this.profileRepository.findAll(Sort.by("id")).stream().map(TechnicianProfileResponse::from).toList();
	}

	/** Case-insensitive search on the specialization and on the technician's name or e-mail. */
	@Transactional(readOnly = true)
	public List<TechnicianProfileResponse> search(String keyword) {
		String pattern = likePattern(keyword);
		return (pattern == null) ? findAll()
				: this.profileRepository.search(pattern).stream().map(TechnicianProfileResponse::from).toList();
	}

	/** Optional fields: blank values are stored as null. */
	private static void apply(TechnicianProfile profile, TechnicianProfileRequest request) {
		profile.setPhone(text(singleLine(request.phone())));
		profile.setSpecialization(text(singleLine(request.specialization())));
		profile.setBiography(text(request.biography()));
	}

	private Technician findTechnician(Long technicianId) {
		return this.technicianRepository.findById(technicianId)
			.orElseThrow(() -> new ResourceNotFoundException("Technicien", technicianId));
	}

	private TechnicianProfile findProfile(Long technicianId) {
		return this.profileRepository.findByTechnicianId(technicianId)
			.orElseThrow(() -> new ResourceNotFoundException("Profil du technicien", technicianId));
	}

}
