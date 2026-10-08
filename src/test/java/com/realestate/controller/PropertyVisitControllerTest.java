package com.realestate.controller;

import com.realestate.config.JwtAuthenticationFilter;
import com.realestate.config.SecurityConfig;
import com.realestate.model.Property;
import com.realestate.model.PropertyVisit;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.PropertyVisitRepository;
import com.realestate.repository.UserRepository;
import com.realestate.service.JwtService;
import com.realestate.service.PropertyVisitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PropertyVisitController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        PropertyVisitService.class
})
class PropertyVisitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PropertyVisitRepository visitRepository;

    @MockBean
    private PropertyRepository propertyRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    void customerCanScheduleVisit() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(customer()));
        when(propertyRepository.findById(15L)).thenReturn(Optional.of(property()));
        when(visitRepository.save(any(PropertyVisit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/visits")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"propertyId": 15, "scheduledAt": "2030-05-01T14:30:00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.propertyTitle").value("Example home"))
                .andExpect(jsonPath("$.customerEmail").value("customer@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void invalidVisitDetailsAreRejected() throws Exception {
        mockMvc.perform(post("/api/visits")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"propertyId": 0, "scheduledAt": "2020-01-01T10:00:00"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerViewsOwnVisits() throws Exception {
        when(visitRepository.findAllByCustomer_EmailIgnoreCaseOrderByScheduledAtAsc("customer@example.com"))
                .thenReturn(List.of(visit()));

        mockMvc.perform(get("/api/visits").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].propertyTitle").value("Example home"));
    }

    @Test
    void agentViewsVisitsForManagedProperties() throws Exception {
        when(visitRepository.findAllByProperty_Agent_EmailIgnoreCaseOrderByScheduledAtAsc("agent@example.com"))
                .thenReturn(List.of(visit()));

        mockMvc.perform(get("/api/visits").with(user("agent@example.com").roles("AGENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerEmail").value("customer@example.com"));
    }

    @Test
    void adminViewsAllVisits() throws Exception {
        when(visitRepository.findAllByOrderByScheduledAtAsc()).thenReturn(List.of(visit()));

        mockMvc.perform(get("/api/visits").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void customerCanCancelTheirOwnVisit() throws Exception {
        when(visitRepository.findByIdAndCustomer_EmailIgnoreCase(42L, "customer@example.com"))
                .thenReturn(Optional.of(visit()));

        mockMvc.perform(delete("/api/visits/42").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isNoContent());

        verify(visitRepository).findByIdAndCustomer_EmailIgnoreCase(42L, "customer@example.com");
    }

    @Test
    void agentCannotCancelVisit() throws Exception {
        mockMvc.perform(delete("/api/visits/42").with(user("agent@example.com").roles("AGENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignedAgentCanAcceptVisitRequest() throws Exception {
        when(visitRepository.findByIdAndProperty_Agent_EmailIgnoreCase(42L, "agent@example.com"))
                .thenReturn(Optional.of(visit()));
        when(visitRepository.save(any(PropertyVisit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/api/visits/42/status")
                        .with(user("agent@example.com").roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACCEPTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void assignedAgentCanRejectVisitRequest() throws Exception {
        when(visitRepository.findByIdAndProperty_Agent_EmailIgnoreCase(42L, "agent@example.com"))
                .thenReturn(Optional.of(visit()));
        when(visitRepository.save(any(PropertyVisit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/api/visits/42/status")
                        .with(user("agent@example.com").roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"REJECTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void customerCannotDecideVisitRequest() throws Exception {
        mockMvc.perform(patch("/api/visits/42/status")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACCEPTED"}
                                """))
                .andExpect(status().isForbidden());
    }

    private User customer() {
        return new User("Casey", "Customer", "customer@example.com", "hash", null, new Role(RoleName.CUSTOMER));
    }

    private User agent() {
        return new User("Alex", "Agent", "agent@example.com", "hash", null, new Role(RoleName.AGENT));
    }

    private Property property() {
        return new Property(
                "Example home",
                "Description",
                "HOUSE",
                new BigDecimal("250000.00"),
                3,
                2,
                1400,
                "Sample City",
                "10 Main Street",
                "AVAILABLE",
                agent()
        );
    }

    private PropertyVisit visit() {
        return new PropertyVisit(property(), customer(), LocalDateTime.of(2030, 5, 1, 14, 30));
    }
}
