package com.realestate.controller;

import com.realestate.dto.PropertyImageResponse;
import com.realestate.model.RoleName;
import com.realestate.service.PropertyImageService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/properties/{propertyId}/images")
public class PropertyImageController {

    private final PropertyImageService propertyImageService;

    public PropertyImageController(PropertyImageService propertyImageService) {
        this.propertyImageService = propertyImageService;
    }

    @GetMapping
    public List<PropertyImageResponse> getForProperty(
            @PathVariable Long propertyId,
            Authentication authentication
    ) {
        return propertyImageService.getForProperty(
                propertyId,
                authentication.getName(),
                roleOf(authentication)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyImageResponse upload(
            @PathVariable Long propertyId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        return propertyImageService.upload(
                propertyId,
                file,
                authentication.getName(),
                roleOf(authentication)
        );
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long propertyId,
            @PathVariable Long imageId,
            Authentication authentication
    ) {
        propertyImageService.delete(
                propertyId,
                imageId,
                authentication.getName(),
                roleOf(authentication)
        );
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
