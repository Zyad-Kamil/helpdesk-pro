package com.helpdeskpro.repository;

import java.util.Collection;
import java.util.List;

import com.helpdeskpro.entity.Ticket;
import com.helpdeskpro.entity.TicketPriority;
import com.helpdeskpro.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	List<Ticket> findByStatusIn(Collection<TicketStatus> statuses);

	boolean existsByTechnicianIdAndStatus(Long technicianId, TicketStatus status);

	long countByTechnicianId(Long technicianId);

	long countByCategoryId(Long categoryId);

	long countByTagsId(Long tagId);

	/** Every parameter is optional ({@code null} means "no filter"); {@code keyword} is a lower-case LIKE pattern. */
	@Query("""
			select t from Ticket t
			where (:keyword is null or lower(t.title) like :keyword escape '!'
					or lower(t.description) like :keyword escape '!'
					or lower(t.requesterName) like :keyword escape '!'
					or lower(t.requesterEmail) like :keyword escape '!')
				and (:status is null or t.status = :status)
				and (:priority is null or t.priority = :priority)
				and (:technicianId is null or t.technician.id = :technicianId)
				and (:categoryId is null or t.category.id = :categoryId)
				and (:tagId is null or exists (select 1 from t.tags g where g.id = :tagId))
			order by t.createdAt desc, t.id desc""")
	List<Ticket> search(@Param("keyword") String keyword, @Param("status") TicketStatus status,
			@Param("priority") TicketPriority priority, @Param("technicianId") Long technicianId,
			@Param("categoryId") Long categoryId, @Param("tagId") Long tagId);

}
