package com.realestate.controller;

import com.realestate.config.JwtAuthenticationFilter;
import com.realestate.config.SecurityConfig;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.UserRepository;
import com.realestate.service.CustomerService;
import com.realestate.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CustomerService.class})
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @Test
    void customerCanReadAndUpdateTheirProfile() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(customer()));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(get("/api/customers/me").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        mockMvc.perform(put("/api/customers/me")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Casey Updated","lastName":"Customer","phoneNumber":"555-0199"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Casey Updated"))
                .andExpect(jsonPath("$.phoneNumber").value("555-0199"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void agentAndAdminCannotReadOrUpdateCustomerProfile() throws Exception {
        for (String role : new String[]{"AGENT", "ADMIN"}) {
            mockMvc.perform(get("/api/customers/me")
                            .with(user(role.toLowerCase() + "@example.com").roles(role)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/api/customers/me")
                            .with(user(role.toLowerCase() + "@example.com").roles(role))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName":"Changed","lastName":"Name","phoneNumber":"555-0100"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    private User customer() {
        return new User(
                "Casey",
                "Customer",
                "customer@example.com",
                "hash",
                null,
                new Role(RoleName.CUSTOMER)
        );
    }
}
