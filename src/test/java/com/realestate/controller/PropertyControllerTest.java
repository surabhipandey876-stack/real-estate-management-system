package com.realestate.controller;

import com.realestate.config.JwtAuthenticationFilter;
import com.realestate.config.SecurityConfig;
import com.realestate.model.Property;
import com.realestate.model.PropertyApprovalStatus;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.service.JwtService;
import com.realestate.service.PropertyService;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.PropertyImageRepository;
import com.realestate.repository.UserRepository;
import com.realestate.service.PropertyImageStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.security.SecureRandom;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PropertyController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, PropertyService.class})
class PropertyControllerTest {

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
    private PropertyRepository propertyRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PropertyImageRepository propertyImageRepository;

    @MockBean
    private PropertyImageStorage propertyImageStorage;

    @Test
    void customerCanViewPropertyList() throws Exception {
        when(propertyRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Property>>any(), any(Sort.class)))
                .thenReturn(List.of(property(agent())));

        mockMvc.perform(get("/api/properties").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Example home"));
    }

    @Test
    void anonymousCanViewPublicPropertyList() throws Exception {
        when(propertyRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Property>>any(), any(Sort.class)))
                .thenReturn(List.of(property(agent())));

        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Example home"));
    }

    @Test
    void anonymousCanViewApprovedAvailablePropertyDetails() throws Exception {
        when(propertyRepository.findById(12L)).thenReturn(Optional.of(property(agent())));

        mockMvc.perform(get("/api/properties/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Example home"))
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
    }

    @Test
    void anonymousCannotViewUnapprovedOrUnavailablePropertyDetails() throws Exception {
        Property pending = property(agent());
        pending.setApprovalStatus(PropertyApprovalStatus.PENDING);
        when(propertyRepository.findById(12L)).thenReturn(Optional.of(pending));

        mockMvc.perform(get("/api/properties/12"))
                .andExpect(status().isNotFound());

        Property sold = property(agent());
        sold.updateDetails(
                sold.getTitle(),
                sold.getDescription(),
                sold.getPropertyType(),
                sold.getPrice(),
                sold.getBedrooms(),
                sold.getBathrooms(),
                sold.getAreaSqft(),
                sold.getCity(),
                sold.getAddress(),
                "SOLD"
        );
        when(propertyRepository.findById(13L)).thenReturn(Optional.of(sold));
        mockMvc.perform(get("/api/properties/13"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousCannotManagePropertiesOrReadImageMetadata() throws Exception {
        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/properties/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/properties/12"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/properties/12/images"))
                .andExpect(status().isForbidden());
    }

    @Test
    void propertyListAcceptsSearchFilters() throws Exception {
        when(propertyRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Property>>any(), any(Sort.class)))
                .thenReturn(List.of(property(agent())));

        mockMvc.perform(get("/api/properties")
                        .param("city", "Sample")
                        .param("propertyType", "house")
                        .param("minPrice", "200000")
                        .param("maxPrice", "300000")
                        .param("minBedrooms", "2")
                        .param("status", "available")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Example home"));
    }

    @Test
    void customerCannotCreateProperty() throws Exception {
        mockMvc.perform(post("/api/properties")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotManageProperties() throws Exception {
        mockMvc.perform(put("/api/properties/12")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/properties/12")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidBearerTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void agentCanCreateProperty() throws Exception {
        when(userRepository.findByEmailIgnoreCase("agent@example.com")).thenReturn(Optional.of(agent()));
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/properties")
                        .with(user("agent@example.com").roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Example home"));
    }

    @Test
    void adminCanCreatePropertyForAnAgent() throws Exception {
        when(userRepository.findById(5L)).thenReturn(Optional.of(agent()));
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/properties")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestWithAgentId()))
                .andExpect(status().isCreated());
    }

    @Test
    void adminCanApproveAndRejectProperties() throws Exception {
        Property property = property(agent());
        property.setApprovalStatus(PropertyApprovalStatus.PENDING);
        when(propertyRepository.findById(12L)).thenReturn(Optional.of(property));
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/api/properties/12/approval")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approvalStatus":"APPROVED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));

        mockMvc.perform(patch("/api/properties/12/approval")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approvalStatus":"REJECTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("REJECTED"));
    }

    @Test
    void agentAndCustomerCannotApproveOrRejectProperties() throws Exception {
        for (String role : List.of("AGENT", "CUSTOMER")) {
            mockMvc.perform(patch("/api/properties/12/approval")
                            .with(user(role.toLowerCase() + "@example.com").roles(role))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"approvalStatus":"APPROVED"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void customerCannotViewUnapprovedPropertyAndAgentCannotViewAnotherAgentsProperty() throws Exception {
        Property pending = property(agent());
        pending.setApprovalStatus(PropertyApprovalStatus.PENDING);
        when(propertyRepository.findById(12L)).thenReturn(Optional.of(pending));
        mockMvc.perform(get("/api/properties/12").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isNotFound());

        when(propertyRepository.findByIdAndAgent_EmailIgnoreCase(12L, "other.agent@example.com"))
                .thenReturn(Optional.empty());
        when(propertyRepository.existsById(12L)).thenReturn(true);
        mockMvc.perform(get("/api/properties/12").with(user("other.agent@example.com").roles("AGENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCanUpdateAndDeleteProperties() throws Exception {
        Property property = property(agent());
        when(propertyRepository.findByIdAndAgent_EmailIgnoreCase(12L, "agent@example.com"))
                .thenReturn(Optional.of(property));
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/properties/12")
                        .with(user("agent@example.com").roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        mockMvc.perform(delete("/api/properties/12")
                        .with(user("agent@example.com").roles("AGENT")))
                .andExpect(status().isNoContent());
    }

    @Test
    void agentCannotUpdateOrDeleteAnotherAgentsProperty() throws Exception {
        when(propertyRepository.findByIdAndAgent_EmailIgnoreCase(12L, "agent@example.com"))
                .thenReturn(Optional.empty());
        when(propertyRepository.existsById(12L)).thenReturn(true);

        mockMvc.perform(put("/api/properties/12")
                        .with(user("agent@example.com").roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/properties/12")
                        .with(user("agent@example.com").roles("AGENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingPropertiesAreNotFound() throws Exception {
        when(propertyRepository.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/properties/404").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Property not found: 404"));
    }

    @Test
    void propertySearchRejectsInvertedPriceRange() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .param("minPrice", "300000")
                        .param("maxPrice", "200000")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("minPrice cannot be greater than maxPrice."));
    }

    @Test
    void propertySearchRejectsNegativeMinimumBedrooms() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .param("minBedrooms", "-1")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isBadRequest());
    }

    private Property property(User agent) {
        return new Property(
                "Example home",
                "A test property",
                "HOUSE",
                new BigDecimal("250000.00"),
                3,
                2,
                1400,
                "Sample City",
                "10 Main Street",
                "AVAILABLE",
                agent
        );
    }

    private User agent() {
        return new User("Pat", "Agent", "agent@example.com", "hash", null, new Role(RoleName.AGENT));
    }

    private String validRequest() {
        return """
                {
                  "title": "Example home",
                  "description": "A test property",
                  "propertyType": "HOUSE",
                  "price": 250000,
                  "bedrooms": 3,
                  "bathrooms": 2,
                  "areaSqft": 1400,
                  "city": "Sample City",
                  "address": "10 Main Street",
                  "status": "AVAILABLE"
                }
                """;
    }

    private String validRequestWithAgentId() {
        return """
                {
                  "title": "Example home",
                  "description": "A test property",
                  "propertyType": "HOUSE",
                  "price": 250000,
                  "bedrooms": 3,
                  "bathrooms": 2,
                  "areaSqft": 1400,
                  "city": "Sample City",
                  "address": "10 Main Street",
                  "status": "AVAILABLE",
                  "agentId": 5
                }
                """;
    }
}
