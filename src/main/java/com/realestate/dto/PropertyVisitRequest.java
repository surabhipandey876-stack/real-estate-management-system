package com.realestate.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record PropertyVisitRequest(
        @NotNull @Positive Long propertyId,
        @NotNull @Future LocalDateTime scheduledAt
) {
}
