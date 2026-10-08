package com.realestate.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PropertyRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotBlank @Size(max = 50) String propertyType,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal price,
        @Min(0) Integer bedrooms,
        @Min(0) Integer bathrooms,
        @Positive Integer areaSqft,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 255) String address,
        @NotBlank @Size(max = 30) String status,
        @Positive Long agentId
) {
}
