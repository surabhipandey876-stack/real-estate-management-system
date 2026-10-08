package com.realestate.service;

import com.realestate.dto.RegisterRequest;
import com.realestate.dto.UpdateAgentRequest;
import com.realestate.exception.DuplicateEmailException;
import com.realestate.exception.AgentInUseException;
import com.realestate.repository.FavoriteRepository;
import com.realestate.exception.InvalidPropertyRequestException;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.InquiryRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.PropertyVisitRepository;
import com.realestate.repository.RoleRepository;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private AdminService adminService;
    private PropertyRepository propertyRepository;
    private InquiryRepository inquiryRepository;
    private PropertyVisitRepository visitRepository;
    private FavoriteRepository favoriteRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        propertyRepository = mock(PropertyRepository.class);
        inquiryRepository = mock(InquiryRepository.class);
        visitRepository = mock(PropertyVisitRepository.class);
        favoriteRepository = mock(FavoriteRepository.class);
        adminService = new AdminService(
                userRepository,
                roleRepository,
                passwordEncoder,
                propertyRepository,
                inquiryRepository,
                visitRepository,
                favoriteRepository
        );
    }

    @Test
    void adminCreatesAgentWithNormalizedEmailAndEncodedPassword() {
        Role agentRole = new Role(RoleName.AGENT);
        when(userRepository.existsByEmailIgnoreCase("agent@hearthhome.local")).thenReturn(false);
        when(roleRepository.findByName(RoleName.AGENT)).thenReturn(Optional.of(agentRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = adminService.createAgent(new RegisterRequest(
                " Property ", " Agent ", "AGENT@HearthHome.local ", "Agent@12345", " 555-0101 "
        ));

        assertEquals("agent@hearthhome.local", response.email());
        assertEquals(RoleName.AGENT, response.role());
        var savedAgent = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedAgent.capture());
        assertEquals("Property", savedAgent.getValue().getFirstName());
        assertEquals("555-0101", savedAgent.getValue().getPhoneNumber());
        assertFalse(savedAgent.getValue().getPasswordHash().equals("Agent@12345"));
        assertTrue(passwordEncoder.matches("Agent@12345", savedAgent.getValue().getPasswordHash()));
    }

    @Test
    void adminCannotOverwriteAnExistingAccountWhenCreatingAgent() {
        when(userRepository.existsByEmailIgnoreCase("existing@hearthhome.local")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> adminService.createAgent(new RegisterRequest(
                "Existing", "Account", "existing@hearthhome.local", "Agent@12345", null
        )));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void adminCanEditAgentDetailsWithoutChangingRoleOrPassword() {
        User agent = new User("Old", "Name", "old@example.com", "stored-hash", null, new Role(RoleName.AGENT));
        when(userRepository.findById(9L)).thenReturn(Optional.of(agent));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 9L)).thenReturn(false);
        when(userRepository.save(agent)).thenReturn(agent);

        var response = adminService.updateAgent(9L, new UpdateAgentRequest(
                " New ", " Agent ", "NEW@example.com ", " 555-0100 "
        ));

        assertEquals("New", response.firstName());
        assertEquals("new@example.com", response.email());
        assertEquals("555-0100", response.phoneNumber());
        assertEquals(RoleName.AGENT, response.role());
        assertEquals("stored-hash", agent.getPasswordHash());
        assertTrue(response.active());
    }

    @Test
    void adminCanDeactivateAgentWithoutDeletingTheAccount() {
        User agent = new User("Agent", "One", "agent@example.com", "stored-hash", null, new Role(RoleName.AGENT));
        when(userRepository.findById(9L)).thenReturn(Optional.of(agent));
        when(userRepository.save(agent)).thenReturn(agent);

        var response = adminService.deactivateAgent(9L);

        assertFalse(response.active());
        assertEquals(RoleName.AGENT, response.role());
        verify(userRepository).save(agent);
    }

    @Test
    void adminCannotEditOrDeactivateNonAgentUsers() {
        User customer = new User("Customer", "One", "customer@example.com", "stored-hash", null, new Role(RoleName.CUSTOMER));
        when(userRepository.findById(9L)).thenReturn(Optional.of(customer));

        assertThrows(InvalidPropertyRequestException.class, () -> adminService.updateAgent(9L, new UpdateAgentRequest(
                "Changed", "Name", "changed@example.com", null
        )));
        assertThrows(InvalidPropertyRequestException.class, () -> adminService.deactivateAgent(9L));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void adminCanActivateADeactivatedAgent() {
        User agent = new User("Agent", "One", "agent@example.com", "hash", null, new Role(RoleName.AGENT));
        agent.deactivate();
        when(userRepository.findById(9L)).thenReturn(Optional.of(agent));
        when(userRepository.save(agent)).thenReturn(agent);

        var response = adminService.activateAgent(9L);

        assertTrue(response.active());
        verify(userRepository).save(agent);
    }

    @Test
    void adminCanDeleteAgentWithoutRelatedRecords() {
        User agent = new User("Agent", "One", "agent@example.com", "hash", null, new Role(RoleName.AGENT));
        when(userRepository.findById(9L)).thenReturn(Optional.of(agent));

        adminService.deleteAgent(9L);

        verify(userRepository).delete(agent);
    }

    @Test
    void adminCannotDeleteAgentWithRelatedRecords() {
        User agent = new User("Agent", "One", "agent@example.com", "hash", null, new Role(RoleName.AGENT));
        when(userRepository.findById(9L)).thenReturn(Optional.of(agent));
        when(propertyRepository.existsByAgent_Id(9L)).thenReturn(true);

        assertThrows(AgentInUseException.class, () -> adminService.deleteAgent(9L));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void statisticsIncludeInventoryAndPendingWorkCounts() {
        when(userRepository.count()).thenReturn(8L);
        when(userRepository.countByRole_Name(RoleName.AGENT)).thenReturn(2L);
        when(userRepository.countByRole_Name(RoleName.CUSTOMER)).thenReturn(5L);
        when(propertyRepository.count()).thenReturn(6L);
        when(propertyRepository.countByStatusIgnoreCase("AVAILABLE")).thenReturn(4L);
        when(propertyRepository.countByStatusIgnoreCase("SOLD")).thenReturn(1L);
        when(propertyRepository.countByStatusIgnoreCase("RENTED")).thenReturn(1L);
        when(inquiryRepository.count()).thenReturn(3L);
        when(inquiryRepository.countByStatusIgnoreCase("OPEN")).thenReturn(2L);
        when(visitRepository.count()).thenReturn(4L);
        when(visitRepository.countByStatusIgnoreCase("PENDING")).thenReturn(1L);

        var stats = adminService.getStatistics();

        assertEquals(8L, stats.totalUsers());
        assertEquals(2L, stats.totalAgents());
        assertEquals(5L, stats.totalCustomers());
        assertEquals(6L, stats.totalProperties());
        assertEquals(4L, stats.availableProperties());
        assertEquals(2L, stats.soldOrRentedProperties());
        assertEquals(3L, stats.totalInquiries());
        assertEquals(2L, stats.pendingInquiries());
        assertEquals(4L, stats.totalVisits());
        assertEquals(1L, stats.pendingVisitRequests());
    }
}
