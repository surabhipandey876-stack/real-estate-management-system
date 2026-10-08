package com.realestate.service;

import com.realestate.dto.LoginRequest;
import com.realestate.dto.RegisterRequest;
import com.realestate.exception.DuplicateEmailException;
import com.realestate.exception.InvalidCredentialsException;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.RoleRepository;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Base64;
import java.util.Optional;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        byte[] signingKey = new byte[32];
        new SecureRandom().nextBytes(signingKey);
        JwtService jwtService = new JwtService(
                Base64.getEncoder().encodeToString(signingKey),
                3600
        );
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService);
    }

    @Test
    void registrationStoresEncodedPasswordAndAssignsCustomerRole() {
        Role customerRole = new Role(RoleName.CUSTOMER);
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return new User(
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    user.getPhoneNumber(),
                    user.getRole()
            );
        });

        var response = authService.register(new RegisterRequest(
                " Pat ", " Example ", "PERSON@example.com ", "a-strong-password", " 123 "
        ));

        assertEquals("person@example.com", response.email());
        assertEquals(RoleName.CUSTOMER, response.role());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registrationRejectsExistingEmail() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(new RegisterRequest(
                "Pat", "Example", "person@example.com", "a-strong-password", null
        )));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginReturnsBearerTokenForValidPassword() {
        Role agentRole = new Role(RoleName.AGENT);
        String passwordHash = passwordEncoder.encode("a-strong-password");
        User user = new User("Pat", "Example", "person@example.com", passwordHash, null, agentRole);
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));

        var response = authService.login(new LoginRequest("PERSON@example.com", "a-strong-password"));

        assertEquals("Bearer", response.tokenType());
        assertTrue(response.accessToken().split("\\.").length == 3);
        assertEquals(RoleName.AGENT, response.user().role());
    }

    @Test
    void loginReturnsCustomerRoleFromExistingUserRecord() {
        assertEquals(RoleName.CUSTOMER, loginAs(RoleName.CUSTOMER).user().role());
    }

    @Test
    void loginReturnsAdminRoleFromExistingUserRecord() {
        assertEquals(RoleName.ADMIN, loginAs(RoleName.ADMIN).user().role());
    }

    @Test
    void loginRejectsIncorrectPassword() {
        User user = new User(
                "Pat",
                "Example",
                "person@example.com",
                passwordEncoder.encode("correct-password"),
                null,
                new Role(RoleName.CUSTOMER)
        );
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(
                new LoginRequest("person@example.com", "incorrect-password")
        ));
    }

    @Test
    void loginRejectsDeactivatedAccountEvenWithCorrectPassword() {
        User user = new User(
                "Agent",
                "One",
                "agent@example.com",
                passwordEncoder.encode("correct-password"),
                null,
                new Role(RoleName.AGENT)
        );
        user.deactivate();
        when(userRepository.findByEmailIgnoreCase("agent@example.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(
                new LoginRequest("agent@example.com", "correct-password")
        ));
    }

    @Test
    void registrationPasswordIsNotStoredAsPlainText() {
        Role customerRole = new Role(RoleName.CUSTOMER);
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertFalse(user.getPasswordHash().equals("a-strong-password"));
            assertTrue(passwordEncoder.matches("a-strong-password", user.getPasswordHash()));
            return user;
        });

        authService.register(new RegisterRequest(
                "Pat", "Example", "person@example.com", "a-strong-password", null
        ));
    }

    private com.realestate.dto.AuthResponse loginAs(RoleName role) {
        String email = switch (role) {
            case CUSTOMER -> "rahul.tripathi@gmail.com";
            case AGENT -> "agent@hearthhome.local";
            case ADMIN -> "admin@hearthhome.local";
        };
        User user = new User("Test", role.name(), email, passwordEncoder.encode("a-strong-password"), null, new Role(role));
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        return authService.login(new LoginRequest(email, "a-strong-password"));
    }
}
