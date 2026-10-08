package com.realestate.dto;

import com.realestate.model.RoleName;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull RoleName role
) {
}
