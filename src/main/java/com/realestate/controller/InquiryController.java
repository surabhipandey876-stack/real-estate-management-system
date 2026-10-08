package com.realestate.controller;

import com.realestate.dto.InquiryRequest;
import com.realestate.dto.InquiryReplyRequest;
import com.realestate.dto.InquiryResponse;
import com.realestate.model.RoleName;
import com.realestate.service.InquiryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InquiryResponse submit(
            @Valid @RequestBody InquiryRequest request,
            Authentication authentication
    ) {
        return inquiryService.submit(request, authentication.getName(), roleOf(authentication));
    }

    @GetMapping
    public List<InquiryResponse> getVisibleInquiries(Authentication authentication) {
        return inquiryService.getVisibleInquiries(authentication.getName(), roleOf(authentication));
    }

    @PutMapping("/{inquiryId}/reply")
    public InquiryResponse reply(
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryReplyRequest request,
            Authentication authentication
    ) {
        return inquiryService.reply(
                inquiryId,
                request,
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
