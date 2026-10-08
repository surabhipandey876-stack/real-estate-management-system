package com.realestate.config;

import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.RoleRepository;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevelopmentAccountInitializerTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private DevelopmentAccountInitializer initializer;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        for (RoleName roleName : RoleName.values()) {
            when(roleRepository.findByName(roleName)).thenReturn(Optional.of(new Role(roleName)));
        }
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        initializer = new DevelopmentAccountInitializer(
                userRepository,
                roleRepository,
                passwordEncoder,
                "Development", "Administrator", "dev.admin@hearthhome.local", "AdminDev@12345",
                "Development", "Agent", "dev.agent@hearthhome.local", "AgentDev@12345",
                "Development", "Customer", "dev.customer@hearthhome.local", "CustomerDev@12345"
        );
    }

    @Test
    void createsMissingDevelopmentAccountsWithHashedPasswordsAndDatabaseRoles() {
        initializer.run(new DefaultApplicationArguments(new String[0]));

        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertEquals(
                java.util.Set.of(RoleName.ADMIN, RoleName.AGENT, RoleName.CUSTOMER),
                captor.getAllValues().stream().map(user -> user.getRole().getName()).collect(java.util.stream.Collectors.toSet())
        );
        assertTrue(captor.getAllValues().stream().allMatch(user ->
                !user.getPasswordHash().equals("AdminDev@12345")
                        && !user.getPasswordHash().equals("AgentDev@12345")
                        && !user.getPasswordHash().equals("CustomerDev@12345")));
        assertTrue(captor.getAllValues().stream().anyMatch(user ->
                passwordEncoder.matches("AdminDev@12345", user.getPasswordHash())));
        assertTrue(captor.getAllValues().stream().anyMatch(user ->
                passwordEncoder.matches("AgentDev@12345", user.getPasswordHash())));
        assertTrue(captor.getAllValues().stream().anyMatch(user ->
                passwordEncoder.matches("CustomerDev@12345", user.getPasswordHash())));
    }

    @Test
    void resetsPasswordAndRoleForExistingDevelopmentAccounts() {
        Role wrongRole = new Role(RoleName.CUSTOMER);
        User existingAdmin = new User(
                "Existing", "Admin", "dev.admin@hearthhome.local", "old-hash", null, wrongRole
        );
        User existingAgent = new User(
                "Existing", "Agent", "dev.agent@hearthhome.local", "old-hash", null, wrongRole
        );
        User existingCustomer = new User(
                "Existing", "Customer", "dev.customer@hearthhome.local", "old-hash", null, wrongRole
        );
        existingAdmin.deactivate();
        existingAgent.deactivate();
        when(userRepository.findByEmailIgnoreCase("dev.admin@hearthhome.local"))
                .thenReturn(Optional.of(existingAdmin));
        when(userRepository.findByEmailIgnoreCase("dev.agent@hearthhome.local"))
                .thenReturn(Optional.of(existingAgent));
        when(userRepository.findByEmailIgnoreCase("dev.customer@hearthhome.local"))
                .thenReturn(Optional.of(existingCustomer));

        initializer.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(RoleName.ADMIN, existingAdmin.getRole().getName());
        assertEquals(RoleName.AGENT, existingAgent.getRole().getName());
        assertEquals(RoleName.CUSTOMER, existingCustomer.getRole().getName());
        assertTrue(existingAdmin.isActive());
        assertTrue(existingAgent.isActive());
        assertTrue(passwordEncoder.matches("AdminDev@12345", existingAdmin.getPasswordHash()));
        assertTrue(passwordEncoder.matches("AgentDev@12345", existingAgent.getPasswordHash()));
        assertTrue(passwordEncoder.matches("CustomerDev@12345", existingCustomer.getPasswordHash()));
        verify(userRepository, org.mockito.Mockito.times(3)).save(any(User.class));
    }
}
