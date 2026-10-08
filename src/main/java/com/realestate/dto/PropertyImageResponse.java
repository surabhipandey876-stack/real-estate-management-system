package com.realestate.dto;

import java.time.LocalDateTime;

public record PropertyImageResponse(
        Long id,
        Long propertyId,
        String imageUrl,
        boolean primary,
        LocalDateTime createdAt
) {
}
