package com.helpdeskpro.repository;

import com.helpdeskpro.entity.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

}
