package com.realestate.repository;

import com.realestate.model.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    boolean existsByCustomer_Id(Long customerId);

    List<Inquiry> findAllByCustomer_EmailIgnoreCaseOrderByCreatedAtDesc(String email);

    List<Inquiry> findAllByProperty_Agent_EmailIgnoreCaseOrderByCreatedAtDesc(String agentEmail);

    List<Inquiry> findAllByOrderByCreatedAtDesc();

    Optional<Inquiry> findByIdAndProperty_Agent_EmailIgnoreCase(Long id, String agentEmail);

    long countByStatusIgnoreCase(String status);
}
