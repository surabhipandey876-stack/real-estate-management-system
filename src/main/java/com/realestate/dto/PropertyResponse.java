package com.realestate.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.realestate.model.PropertyApprovalStatus;

public record PropertyResponse(
        Long id,
        String title,
        String description,
        String propertyType,
        BigDecimal price,
        Integer bedrooms,
        Integer bathrooms,
        Integer areaSqft,
        String city,
        String address,
        String status,
        PropertyApprovalStatus approvalStatus,
        Long agentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
