package com.realestate.controller;

import com.realestate.dto.AdminStatisticsResponse;
import com.realestate.dto.RegisterRequest;
import com.realestate.dto.UpdateAgentRequest;
import com.realestate.dto.UpdateUserRoleRequest;
import com.realestate.dto.UserResponse;
import com.realestate.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<UserResponse> getUsers() {
        return adminService.getUsers();
    }

    @GetMapping("/agents")
    public List<UserResponse> getAgents() {
        return adminService.getAgents();
    }

    @PostMapping("/agents")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createAgent(@Valid @RequestBody RegisterRequest request) {
        return adminService.createAgent(request);
    }

    @PutMapping("/agents/{agentId}")
    public UserResponse updateAgent(
            @PathVariable Long agentId,
            @Valid @RequestBody UpdateAgentRequest request
    ) {
        return adminService.updateAgent(agentId, request);
    }

    @PatchMapping("/agents/{agentId}/deactivate")
    public UserResponse deactivateAgent(@PathVariable Long agentId) {
        return adminService.deactivateAgent(agentId);
    }

    @PatchMapping("/agents/{agentId}/activate")
    public UserResponse activateAgent(@PathVariable Long agentId) {
        return adminService.activateAgent(agentId);
    }

    @DeleteMapping("/agents/{agentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAgent(@PathVariable Long agentId) {
        adminService.deleteAgent(agentId);
    }

    @GetMapping("/statistics")
    public AdminStatisticsResponse getStatistics() {
        return adminService.getStatistics();
    }

    @PutMapping("/users/{userId}/role")
    public UserResponse updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication
    ) {
        return adminService.updateUserRole(userId, request, authentication.getName());
    }
}
