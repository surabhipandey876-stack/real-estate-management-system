package com.realestate.controller;

import com.realestate.config.JwtAuthenticationFilter;
import com.realestate.config.SecurityConfig;
import com.realestate.model.Inquiry;
import com.realestate.model.Property;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.InquiryRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.UserRepository;
import com.realestate.service.InquiryService;
import com.realestate.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InquiryController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        InquiryService.class
})
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InquiryRepository inquiryRepository;

    @MockBean
    private PropertyRepository propertyRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    void customerCanSubmitInquiryForProperty() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(customer()));
        when(propertyRepository.findById(9L)).thenReturn(Optional.of(property()));
        when(inquiryRepository.saveAndFlush(any(Inquiry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/inquiries")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"propertyId": 9, "message": "I would like to arrange a viewing."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.propertyTitle").value("Example home"))
                .andExpect(jsonPath("$.customerEmail").value("customer@example.com"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void customerCanViewOwnInquiries() throws Exception {
        when(inquiryRepository.findAllByCustomer_EmailIgnoreCaseOrderByCreatedAtDesc("customer@example.com"))
                .thenReturn(List.of(inquiry()));

        mockMvc.perform(get("/api/inquiries").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("Please contact me about this home."));

        verify(inquiryRepository).findAllByCustomer_EmailIgnoreCaseOrderByCreatedAtDesc("customer@example.com");
    }

    @Test
    void agentCanViewInquiriesForTheirProperties() throws Exception {
        when(inquiryRepository.findAllByProperty_Agent_EmailIgnoreCaseOrderByCreatedAtDesc("agent@example.com"))
                .thenReturn(List.of(inquiry()));

        mockMvc.perform(get("/api/inquiries").with(user("agent@example.com").roles("AGENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].propertyTitle").value("Example home"));
    }

    @Test
    void adminCanViewAllInquiries() throws Exception {
        when(inquiryRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(inquiry()));

        mockMvc.perform(get("/api/inquiries").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerEmail").value("customer@example.com"));
    }

    @Test
    void agentCanReplyToInquiryForTheirProperty() throws Exception {
        when(inquiryRepository.findByIdAndProperty_Agent_EmailIgnoreCase(17L, "agent@example.com"))
                .thenReturn(Optional.of(inquiry()));
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/inquiries/17/reply")
                        .with(user("agent@example.com").roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "I will call you shortly."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseMessage").value("I will call you shortly."))
                .andExpect(jsonPath("$.status").value("ANSWERED"));
    }

    @Test
    void customerCannotReplyToInquiry() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/inquiries/17/reply")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "I will call you shortly."}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCannotSubmitInquiry() throws Exception {
        mockMvc.perform(post("/api/inquiries")
                        .with(user("agent@example.com").roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"propertyId": 9, "message": "Not a customer inquiry."}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void inquiryRequiresPropertyAndNonBlankMessage() throws Exception {
        mockMvc.perform(post("/api/inquiries")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"propertyId": 0, "message": " "}
                                """))
                .andExpect(status().isBadRequest());
    }

    private User customer() {
        return new User(
                "Casey",
                "Customer",
                "customer@example.com",
                "not-used",
                null,
                new Role(RoleName.CUSTOMER)
        );
    }

    private User agent() {
        return new User("Alex", "Agent", "agent@example.com", "not-used", null, new Role(RoleName.AGENT));
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

    private Inquiry inquiry() {
        return new Inquiry(customer(), property(), "Please contact me about this home.");
    }
}
