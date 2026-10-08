package com.realestate.dto;

import com.realestate.model.PropertyVisitStatus;
import jakarta.validation.constraints.NotNull;

public record PropertyVisitDecisionRequest(@NotNull PropertyVisitStatus status) {
}
