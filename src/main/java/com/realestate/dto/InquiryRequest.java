package com.realestate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record InquiryRequest(
        @NotNull @Positive Long propertyId,
        @NotBlank @Size(max = 5000) String message
) {
}
