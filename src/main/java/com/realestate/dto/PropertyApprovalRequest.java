package com.realestate.dto;

import com.realestate.model.PropertyApprovalStatus;
import jakarta.validation.constraints.NotNull;

public record PropertyApprovalRequest(@NotNull PropertyApprovalStatus approvalStatus) {
}
