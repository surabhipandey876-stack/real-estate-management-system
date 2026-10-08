package com.realestate.controller;

import com.realestate.config.JwtAuthenticationFilter;
import com.realestate.config.SecurityConfig;
import com.realestate.dto.AdminStatisticsResponse;
import com.realestate.dto.RegisterRequest;
import com.realestate.dto.UpdateAgentRequest;
import com.realestate.dto.UpdateUserRoleRequest;
import com.realestate.dto.UserResponse;
import com.realestate.model.Role;
import com.realestate.model.User;
import com.realestate.model.RoleName;
import com.realestate.service.AdminService;
import com.realestate.service.JwtService;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.List;
import java.security.SecureRandom;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class AdminControllerTest {

    private static final String TEST_JWT_SECRET = createTestJwtSecret();

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> TEST_JWT_SECRET);
    }

    private static String createTestJwtSecret() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void adminCanViewSystemStatisticsAndUserDirectory() throws Exception {
        when(adminService.getStatistics()).thenReturn(new AdminStatisticsResponse(8, 2, 5, 6, 4, 1, 3, 2, 4, 1));
        when(adminService.getUsers()).thenReturn(List.of(
                new UserResponse(1L, "Alex", "Agent", "agent@example.com", null, RoleName.AGENT, true)
        ));
        when(adminService.updateUserRole(1L, new UpdateUserRoleRequest(RoleName.AGENT), "admin@example.com"))
                .thenReturn(new UserResponse(1L, "Alex", "Agent", "agent@example.com", null, RoleName.AGENT, true));

        mockMvc.perform(get("/api/admin/statistics").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(8))
                .andExpect(jsonPath("$.totalAgents").value(2))
                .andExpect(jsonPath("$.totalProperties").value(6))
                .andExpect(jsonPath("$.availableProperties").value(4))
                .andExpect(jsonPath("$.soldOrRentedProperties").value(1))
                .andExpect(jsonPath("$.pendingInquiries").value(2))
                .andExpect(jsonPath("$.pendingVisitRequests").value(1));
        mockMvc.perform(get("/api/admin/users").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("AGENT"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/users/1/role")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "AGENT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    void adminCanCreateAgentAccount() throws Exception {
        when(adminService.createAgent(new RegisterRequest(
                "Property", "Agent", "agent@hearthhome.local", "Agent@12345", "555-0101"
        ))).thenReturn(new UserResponse(
                7L, "Property", "Agent", "agent@hearthhome.local", "555-0101", RoleName.AGENT, true
        ));

        mockMvc.perform(post("/api/admin/agents")
                        .with(user("admin@hearthhome.local").roles("ADMIN"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Property",
                                  "lastName": "Agent",
                                  "email": "agent@hearthhome.local",
                                  "password": "Agent@12345",
                                  "phoneNumber": "555-0101"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("agent@hearthhome.local"))
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    void adminCanEditAndDeactivateAgents() throws Exception {
        when(adminService.updateAgent(7L, new UpdateAgentRequest(
                "Updated", "Agent", "updated.agent@hearthhome.local", "555-0102"
        ))).thenReturn(new UserResponse(
                7L, "Updated", "Agent", "updated.agent@hearthhome.local", "555-0102", RoleName.AGENT, true
        ));
        when(adminService.deactivateAgent(7L)).thenReturn(new UserResponse(
                7L, "Updated", "Agent", "updated.agent@hearthhome.local", "555-0102", RoleName.AGENT, false
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/agents/7")
                        .with(user("admin@hearthhome.local").roles("ADMIN"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Updated","lastName":"Agent","email":"updated.agent@hearthhome.local","phoneNumber":"555-0102"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated.agent@hearthhome.local"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/admin/agents/7/deactivate")
                        .with(user("admin@hearthhome.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        when(adminService.activateAgent(7L)).thenReturn(new UserResponse(
                7L, "Updated", "Agent", "updated.agent@hearthhome.local", "555-0102", RoleName.AGENT, true
        ));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/admin/agents/7/activate")
                        .with(user("admin@hearthhome.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/admin/agents/7")
                        .with(user("admin@hearthhome.local").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void customerCannotAccessAdminDashboardResources() throws Exception {
        mockMvc.perform(get("/api/admin/statistics").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/agents")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"New","lastName":"Agent","email":"new.agent@hearthhome.local","password":"Agent@12345"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/agents/7")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Changed","lastName":"Agent","email":"changed@example.com"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/admin/agents/7/deactivate")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/admin/agents/7/activate")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/admin/agents/7")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/users/1/role")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "AGENT"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCannotAccessAdminDashboardResources() throws Exception {
        mockMvc.perform(get("/api/admin/statistics").with(user("agent@example.com").roles("AGENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/agents")
                        .with(user("agent@example.com").roles("AGENT"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"New","lastName":"Agent","email":"new.agent@hearthhome.local","password":"Agent@12345"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/agents").with(user("agent@example.com").roles("AGENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/agents/7")
                        .with(user("agent@example.com").roles("AGENT"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Changed","lastName":"Agent","email":"changed@example.com"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/admin/agents/7/deactivate")
                        .with(user("agent@example.com").roles("AGENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/admin/agents/7/activate")
                        .with(user("agent@example.com").roles("AGENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/admin/agents/7")
                        .with(user("agent@example.com").roles("AGENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/users/1/role")
                        .with(user("agent@example.com").roles("AGENT"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "ADMIN"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivatedAgentCannotUseAnAlreadyIssuedBearerToken() throws Exception {
        User deactivatedAgent = new User(
                "Property", "Agent", "agent@example.com", "password-hash", null, new Role(RoleName.AGENT)
        );
        deactivatedAgent.deactivate();
        when(userRepository.findByEmailIgnoreCaseWithRole("agent@example.com"))
                .thenReturn(java.util.Optional.of(deactivatedAgent));
        String token = jwtService.issueToken("agent@example.com", RoleName.AGENT).value();

        mockMvc.perform(get("/api/admin/statistics").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
