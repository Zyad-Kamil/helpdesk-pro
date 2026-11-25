package com.helpdeskpro.repository;

import java.util.List;

import com.helpdeskpro.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	/** {@code keyword} is a lower-case LIKE pattern. */
	@Query("""
			select x from Category x
			where lower(x.name) like :keyword escape '!' or lower(x.description) like :keyword escape '!'
			order by lower(x.name), x.id""")
	List<Category> search(@Param("keyword") String keyword);

}
