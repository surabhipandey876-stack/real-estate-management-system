package com.realestate.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PropertySearchCriteria(
        @Size(max = 100) String city,
        @Size(max = 50) String propertyType,
        @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal minPrice,
        @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal maxPrice,
        @Min(0) Integer minBedrooms,
        @Size(max = 30) String status
) {
}
