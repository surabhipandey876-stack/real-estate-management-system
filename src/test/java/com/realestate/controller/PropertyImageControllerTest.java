package com.realestate.controller;

import com.realestate.config.JwtAuthenticationFilter;
import com.realestate.config.SecurityConfig;
import com.realestate.dto.PropertyImageResponse;
import com.realestate.service.JwtService;
import com.realestate.service.PropertyImageService;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PropertyImageController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class PropertyImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PropertyImageService propertyImageService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void agentCanUploadAnImageAndReceivesItsUrl() throws Exception {
        when(propertyImageService.upload(eq(5L), any(), eq("agent@example.com"), any()))
                .thenReturn(new PropertyImageResponse(8L, 5L, "/uploads/property-images/photo.png", true, null));

        mockMvc.perform(multipart("/api/properties/5/images")
                        .file(new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1}))
                        .with(user("agent@example.com").roles("AGENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value("/uploads/property-images/photo.png"));
    }

    @Test
    void customerCannotUploadAnImage() throws Exception {
        mockMvc.perform(multipart("/api/properties/5/images")
                        .file(new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1}))
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedCustomerCanViewPropertyImages() throws Exception {
        when(propertyImageService.getForProperty(5L, "customer@example.com", com.realestate.model.RoleName.CUSTOMER))
                .thenReturn(List.of(new PropertyImageResponse(8L, 5L, "/uploads/property-images/photo.png", true, null)));

        mockMvc.perform(get("/api/properties/5/images")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].primary").value(true));
        verify(propertyImageService).getForProperty(5L, "customer@example.com", com.realestate.model.RoleName.CUSTOMER);
    }
}
