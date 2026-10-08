package com.realestate.controller;

import com.realestate.dto.AuthResponse;
import com.realestate.dto.UserResponse;
import com.realestate.model.RoleName;
import com.realestate.service.AuthService;
import com.realestate.service.JwtService;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void registrationIsAvailableAtApiAuthRegister() throws Exception {
        when(authService.register(any())).thenReturn(new UserResponse(
                42L,
                "Pat",
                "Example",
                "pat@example.com",
                null,
                RoleName.CUSTOMER,
                true
        ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Pat",
                                  "lastName": "Example",
                                  "email": "pat@example.com",
                                  "password": "choose-a-strong-password",
                                  "phoneNumber": "555-0100"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("pat@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void loginIsAvailableAtApiAuthLogin() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse(
                "test-token",
                "Bearer",
                Instant.parse("2030-05-01T15:30:00Z"),
                new UserResponse(42L, "Pat", "Example", "pat@example.com", null, RoleName.CUSTOMER, true)
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "pat@example.com",
                                  "password": "choose-a-strong-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("test-token"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    void loginEndpointReturnsAgentRoleForAnAgentAccount() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse(
                "agent-token",
                "Bearer",
                Instant.parse("2030-05-01T15:30:00Z"),
                new UserResponse(43L, "Property", "Agent", "agent@hearthhome.local", "555-0101", RoleName.AGENT, true)
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "agent@hearthhome.local", "password": "Agent@12345"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("AGENT"));
    }

    @Test
    void loginEndpointReturnsAdminRoleForAnAdminAccount() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse(
                "admin-token",
                "Bearer",
                Instant.parse("2030-05-01T15:30:00Z"),
                new UserResponse(44L, "System", "Administrator", "admin@hearthhome.local", null, RoleName.ADMIN, true)
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@hearthhome.local", "password": "Admin@12345"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }
}
