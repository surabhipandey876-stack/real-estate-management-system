package com.realestate.controller;

import com.realestate.dto.PropertyVisitRequest;
import com.realestate.dto.PropertyVisitResponse;
import com.realestate.dto.PropertyVisitDecisionRequest;
import com.realestate.model.RoleName;
import com.realestate.service.PropertyVisitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/visits")
public class PropertyVisitController {

    private final PropertyVisitService visitService;

    public PropertyVisitController(PropertyVisitService visitService) {
        this.visitService = visitService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyVisitResponse schedule(
            @Valid @RequestBody PropertyVisitRequest request,
            Authentication authentication
    ) {
        return visitService.schedule(request, authentication.getName(), roleOf(authentication));
    }

    @GetMapping
    public List<PropertyVisitResponse> getVisibleVisits(Authentication authentication) {
        return visitService.getVisibleVisits(authentication.getName(), roleOf(authentication));
    }

    @PatchMapping("/{visitId}/status")
    public PropertyVisitResponse decide(
            @PathVariable Long visitId,
            @Valid @RequestBody PropertyVisitDecisionRequest request,
            Authentication authentication
    ) {
        return visitService.decide(
                visitId,
                request.status(),
                authentication.getName(),
                roleOf(authentication)
        );
    }

    @DeleteMapping("/{visitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long visitId, Authentication authentication) {
        visitService.cancel(visitId, authentication.getName(), roleOf(authentication));
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
