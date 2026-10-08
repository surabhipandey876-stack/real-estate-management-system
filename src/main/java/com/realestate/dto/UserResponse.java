package com.realestate.dto;

import com.realestate.model.RoleName;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        RoleName role,
        boolean active
) {
}
