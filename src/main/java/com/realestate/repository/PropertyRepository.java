package com.realestate.repository;

import com.realestate.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    Optional<Property> findByIdAndAgent_EmailIgnoreCase(Long id, String email);

    boolean existsByAgent_Id(Long agentId);

    long countByStatusIgnoreCase(String status);
}
