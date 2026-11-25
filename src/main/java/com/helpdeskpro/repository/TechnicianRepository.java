package com.helpdeskpro.repository;

import java.util.List;

import com.helpdeskpro.entity.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

	/** {@code keyword} is a lower-case LIKE pattern and {@code active} a filter; both optional. */
	@Query("""
			select t from Technician t
			where (:keyword is null or lower(t.firstName) like :keyword escape '!'
					or lower(t.lastName) like :keyword escape '!'
					or lower(t.email) like :keyword escape '!')
				and (:active is null or t.active = :active)
			order by lower(t.lastName), lower(t.firstName), t.id""")
	List<Technician> search(@Param("keyword") String keyword, @Param("active") Boolean active);

}
