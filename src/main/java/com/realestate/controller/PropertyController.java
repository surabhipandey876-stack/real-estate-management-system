package com.realestate.controller;

import com.realestate.dto.PropertyRequest;
import com.realestate.dto.PropertyResponse;
import com.realestate.dto.PropertySearchCriteria;
import com.realestate.dto.PropertyApprovalRequest;
import com.realestate.model.RoleName;
import com.realestate.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse create(
            @Valid @RequestBody PropertyRequest request,
            Authentication authentication
    ) {
        return propertyService.create(request, authentication.getName(), roleOf(authentication));
    }

    @GetMapping
    public List<PropertyResponse> getAll(
            @Valid @ModelAttribute PropertySearchCriteria criteria,
            Authentication authentication
    ) {
        return propertyService.search(
                criteria,
                authentication == null ? null : authentication.getName(),
                roleOf(authentication)
        );
    }

    @GetMapping("/{id}")
    public PropertyResponse getById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return propertyService.getById(
                id,
                authentication == null ? null : authentication.getName(),
                roleOf(authentication)
        );
    }

    @PatchMapping("/{id}/approval")
    public PropertyResponse setApprovalStatus(
            @PathVariable Long id,
            @Valid @RequestBody PropertyApprovalRequest request,
            Authentication authentication
    ) {
        return propertyService.setApprovalStatus(id, request.approvalStatus(), roleOf(authentication));
    }

    @PutMapping("/{id}")
    public PropertyResponse update(
            @PathVariable Long id,
            @Valid @RequestBody PropertyRequest request,
            Authentication authentication
    ) {
        return propertyService.update(id, request, authentication.getName(), roleOf(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        propertyService.delete(id, authentication.getName(), roleOf(authentication));
    }

    private RoleName roleOf(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> RoleName.valueOf(authority.substring("ROLE_".length())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Authenticated account has no recognized role."));
    }
}
