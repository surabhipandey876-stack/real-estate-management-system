package com.realestate.service;

import com.realestate.dto.UpdateCustomerProfileRequest;
import com.realestate.exception.CustomerAccessDeniedException;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceTest {

    private UserRepository userRepository;
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        customerService = new CustomerService(userRepository);
    }

    @Test
    void customerCanReadTheirOwnProfile() {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(customer()));

        var response = customerService.getProfile("CUSTOMER@example.com", RoleName.CUSTOMER);

        assertEquals("Casey", response.firstName());
        assertEquals("customer@example.com", response.email());
        assertEquals(RoleName.CUSTOMER, response.role());
    }

    @Test
    void customerCanUpdateNameAndPhoneWithoutChangingEmailOrRole() {
        User customer = customer();
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));
        when(userRepository.save(customer)).thenReturn(customer);

        var response = customerService.updateProfile(
                "customer@example.com",
                RoleName.CUSTOMER,
                new UpdateCustomerProfileRequest("  Casey  ", " Customer ", "555-0199")
        );

        assertEquals("Casey", response.firstName());
        assertEquals("Customer", response.lastName());
        assertEquals("customer@example.com", response.email());
        assertEquals("555-0199", response.phoneNumber());
        assertEquals(RoleName.CUSTOMER, response.role());
        verify(userRepository).save(customer);
    }

    @Test
    void nonCustomerCannotAccessCustomerProfileService() {
        assertThrows(CustomerAccessDeniedException.class,
                () -> customerService.getProfile("agent@example.com", RoleName.AGENT));
        assertThrows(CustomerAccessDeniedException.class,
                () -> customerService.updateProfile(
                        "admin@example.com",
                        RoleName.ADMIN,
                        new UpdateCustomerProfileRequest("Admin", "User", null)
                ));
    }

    private User customer() {
        return new User("Casey", "Customer", "customer@example.com", "hash", null, new Role(RoleName.CUSTOMER));
    }
}
