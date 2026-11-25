package com.helpdeskpro.repository;

import java.util.List;
import java.util.Optional;

import com.helpdeskpro.entity.TechnicianProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TechnicianProfileRepository extends JpaRepository<TechnicianProfile, Long> {

	Optional<TechnicianProfile> findByTechnicianId(Long technicianId);

	/** Matches the specialization or the technician's name or e-mail; {@code keyword} is a lower-case LIKE pattern. */
	@Query("""
			select p from TechnicianProfile p join p.technician t
			where lower(p.specialization) like :keyword escape '!'
				or lower(t.firstName) like :keyword escape '!'
				or lower(t.lastName) like :keyword escape '!'
				or lower(t.email) like :keyword escape '!'
			order by lower(t.lastName), lower(t.firstName), p.id""")
	List<TechnicianProfile> search(@Param("keyword") String keyword);

}
