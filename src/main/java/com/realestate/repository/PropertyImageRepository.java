package com.realestate.repository;

import com.realestate.model.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

    List<PropertyImage> findAllByProperty_IdOrderByPrimaryDescCreatedAtAsc(Long propertyId);

    List<PropertyImage> findAllByProperty_Id(Long propertyId);

    long countByProperty_Id(Long propertyId);

    Optional<PropertyImage> findByIdAndProperty_Id(Long id, Long propertyId);
}
