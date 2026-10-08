package com.realestate.dto;

import java.time.LocalDateTime;

public record InquiryResponse(
        Long id,
        Long propertyId,
        String propertyTitle,
        Long customerId,
        String customerEmail,
        String message,
        String status,
        LocalDateTime createdAt,
        String responseMessage,
        LocalDateTime respondedAt
) {
}
