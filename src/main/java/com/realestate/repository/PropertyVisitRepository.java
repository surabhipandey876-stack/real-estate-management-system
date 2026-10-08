package com.realestate.repository;

import com.realestate.model.PropertyVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyVisitRepository extends JpaRepository<PropertyVisit, Long> {

    boolean existsByAgent_Id(Long agentId);

    boolean existsByCustomer_Id(Long customerId);

    List<PropertyVisit> findAllByCustomer_EmailIgnoreCaseOrderByScheduledAtAsc(String email);

    List<PropertyVisit> findAllByProperty_Agent_EmailIgnoreCaseOrderByScheduledAtAsc(String email);

    List<PropertyVisit> findAllByOrderByScheduledAtAsc();

    Optional<PropertyVisit> findByIdAndCustomer_EmailIgnoreCase(Long id, String email);

    Optional<PropertyVisit> findByIdAndProperty_Agent_EmailIgnoreCase(Long id, String email);

    long countByStatusIgnoreCase(String status);
}
