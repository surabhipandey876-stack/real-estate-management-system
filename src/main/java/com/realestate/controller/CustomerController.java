package com.realestate.controller;

import com.realestate.dto.UpdateCustomerProfileRequest;
import com.realestate.dto.UserResponse;
import com.realestate.model.RoleName;
import com.realestate.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers/me")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public UserResponse getProfile(Authentication authentication) {
        return customerService.getProfile(authentication.getName(), roleOf(authentication));
    }

    @PutMapping
    public UserResponse updateProfile(
            @Valid @RequestBody UpdateCustomerProfileRequest request,
            Authentication authentication
    ) {
        return customerService.updateProfile(authentication.getName(), roleOf(authentication), request);
    }

    private RoleName roleOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> RoleName.valueOf(authority.substring("ROLE_".length())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Authenticated account has no recognized role."));
    }
}
