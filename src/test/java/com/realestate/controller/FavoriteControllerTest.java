package com.realestate.controller;

import com.realestate.config.JwtAuthenticationFilter;
import com.realestate.config.SecurityConfig;
import com.realestate.model.Favorite;
import com.realestate.model.Property;
import com.realestate.model.PropertyApprovalStatus;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.FavoriteRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.UserRepository;
import com.realestate.service.FavoriteService;
import com.realestate.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriteController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        FavoriteService.class
})
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FavoriteRepository favoriteRepository;

    @MockBean
    private PropertyRepository propertyRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    void customerCanAddFavorite() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(customer()));
        when(favoriteRepository.existsByCustomer_EmailIgnoreCaseAndProperty_Id("customer@example.com", 9L))
                .thenReturn(false);
        when(propertyRepository.findById(9L)).thenReturn(Optional.of(property()));
        when(favoriteRepository.saveAndFlush(any(Favorite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/favorites/9").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Favorite house"));
    }

    @Test
    void customerCanViewOnlyTheirFavorites() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(customer()));
        when(favoriteRepository.findAllByCustomer_EmailIgnoreCaseAndProperty_ApprovalStatusOrderByCreatedAtDesc(
                "customer@example.com", PropertyApprovalStatus.APPROVED
        ))
                .thenReturn(List.of(new Favorite(customer(), property())));

        mockMvc.perform(get("/api/favorites").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Favorite house"));
    }

    @Test
    void customerCanRemoveTheirFavorite() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(customer()));
        when(favoriteRepository.findByCustomer_EmailIgnoreCaseAndProperty_Id("customer@example.com", 9L))
                .thenReturn(Optional.of(new Favorite(customer(), property())));

        mockMvc.perform(delete("/api/favorites/9").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isNoContent());
    }

    @Test
    void agentsCannotAccessFavoritesEndpoints() throws Exception {
        mockMvc.perform(get("/api/favorites").with(user("agent@example.com").roles("AGENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotFavoriteAnUnapprovedProperty() throws Exception {
        Property pending = property();
        pending.setApprovalStatus(PropertyApprovalStatus.PENDING);
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(customer()));
        when(favoriteRepository.existsByCustomer_EmailIgnoreCaseAndProperty_Id("customer@example.com", 9L))
                .thenReturn(false);
        when(propertyRepository.findById(9L)).thenReturn(Optional.of(pending));

        mockMvc.perform(post("/api/favorites/9").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isNotFound());
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
                "Favorite house",
                "A test property",
                "HOUSE",
                new BigDecimal("325000.00"),
                3,
                2,
                1500,
                "Sample City",
                "15 Main Street",
                "AVAILABLE",
                agent()
        );
    }
}
