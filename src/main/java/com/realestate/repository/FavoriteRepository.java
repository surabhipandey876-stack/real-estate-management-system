package com.realestate.repository;

import com.realestate.model.Favorite;
import com.realestate.model.PropertyApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByCustomer_Id(Long customerId);

    boolean existsByCustomer_EmailIgnoreCaseAndProperty_Id(String email, Long propertyId);

    List<Favorite> findAllByCustomer_EmailIgnoreCaseOrderByCreatedAtDesc(String email);

    List<Favorite> findAllByCustomer_EmailIgnoreCaseAndProperty_ApprovalStatusOrderByCreatedAtDesc(
            String email,
            PropertyApprovalStatus approvalStatus
    );

    Optional<Favorite> findByCustomer_EmailIgnoreCaseAndProperty_Id(String email, Long propertyId);
}
