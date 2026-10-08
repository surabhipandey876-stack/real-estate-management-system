package com.realestate.dto;

import java.time.LocalDateTime;

public record PropertyVisitResponse(
        Long id,
        Long propertyId,
        String propertyTitle,
        Long customerId,
        String customerEmail,
        Long agentId,
        LocalDateTime scheduledAt,
        String status,
        LocalDateTime createdAt
) {
}
