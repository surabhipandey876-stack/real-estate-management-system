package com.realestate.exception;

import com.realestate.model.RoleName;

public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(RoleName roleName) {
        super("Required database role is missing: " + roleName);
    }
}
