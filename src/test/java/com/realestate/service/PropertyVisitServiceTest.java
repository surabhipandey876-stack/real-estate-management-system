package com.realestate.service;

import com.realestate.dto.PropertyVisitRequest;
import com.realestate.exception.PropertyNotFoundException;
import com.realestate.exception.VisitAccessDeniedException;
import com.realestate.exception.VisitStatusConflictException;
import com.realestate.exception.VisitNotFoundException;
import com.realestate.model.Property;
import com.realestate.model.PropertyApprovalStatus;
import com.realestate.model.PropertyVisit;
import com.realestate.model.PropertyVisitStatus;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.PropertyVisitRepository;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyVisitServiceTest {

    private PropertyVisitRepository visitRepository;
    private PropertyRepository propertyRepository;
    private UserRepository userRepository;
    private PropertyVisitService visitService;

    @BeforeEach
    void setUp() {
        visitRepository = mock(PropertyVisitRepository.class);
        propertyRepository = mock(PropertyRepository.class);
        userRepository = mock(UserRepository.class);
        visitService = new PropertyVisitService(visitRepository, propertyRepository, userRepository);
    }

    @Test
    void customerCanScheduleVisitForExistingProperty() {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(customer()));
        when(propertyRepository.findById(15L)).thenReturn(Optional.of(property()));
        when(visitRepository.save(any(PropertyVisit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime scheduledAt = LocalDateTime.of(2030, 5, 1, 14, 30);
        var result = visitService.schedule(
                new PropertyVisitRequest(15L, scheduledAt),
                "CUSTOMER@example.com",
                RoleName.CUSTOMER
        );

        assertEquals(scheduledAt, result.scheduledAt());
        assertEquals("PENDING", result.status());
        assertEquals("customer@example.com", result.customerEmail());
        verify(visitRepository).save(any(PropertyVisit.class));
    }

    @Test
    void schedulingUnknownPropertyFails() {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(customer()));
        when(propertyRepository.findById(15L)).thenReturn(Optional.empty());

        assertThrows(PropertyNotFoundException.class, () -> visitService.schedule(
                new PropertyVisitRequest(15L, LocalDateTime.of(2030, 5, 1, 14, 30)),
                "customer@example.com",
                RoleName.CUSTOMER
        ));
        verify(visitRepository, never()).save(any(PropertyVisit.class));
    }

    @Test
    void customerCannotScheduleVisitForUnapprovedProperty() {
        Property pending = property();
        pending.setApprovalStatus(PropertyApprovalStatus.PENDING);
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(customer()));
        when(propertyRepository.findById(15L)).thenReturn(Optional.of(pending));

        assertThrows(PropertyNotFoundException.class, () -> visitService.schedule(
                new PropertyVisitRequest(15L, LocalDateTime.of(2030, 5, 1, 14, 30)),
                "customer@example.com",
                RoleName.CUSTOMER
        ));
        verify(visitRepository, never()).save(any(PropertyVisit.class));
    }

    @Test
    void onlyCustomerCanScheduleVisit() {
        assertThrows(VisitAccessDeniedException.class, () -> visitService.schedule(
                new PropertyVisitRequest(15L, LocalDateTime.of(2030, 5, 1, 14, 30)),
                "agent@example.com",
                RoleName.AGENT
        ));
        verify(userRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void customersSeeOnlyTheirOwnVisits() {
        when(visitRepository.findAllByCustomer_EmailIgnoreCaseOrderByScheduledAtAsc("customer@example.com"))
                .thenReturn(List.of(visit()));

        var result = visitService.getVisibleVisits("CUSTOMER@example.com", RoleName.CUSTOMER);

        assertEquals(1, result.size());
        verify(visitRepository).findAllByCustomer_EmailIgnoreCaseOrderByScheduledAtAsc("customer@example.com");
    }

    @Test
    void agentsSeeVisitsForPropertiesTheyManage() {
        when(visitRepository.findAllByProperty_Agent_EmailIgnoreCaseOrderByScheduledAtAsc("agent@example.com"))
                .thenReturn(List.of(visit()));

        var result = visitService.getVisibleVisits("AGENT@example.com", RoleName.AGENT);

        assertEquals(1, result.size());
        verify(visitRepository).findAllByProperty_Agent_EmailIgnoreCaseOrderByScheduledAtAsc("agent@example.com");
    }

    @Test
    void adminSeesAllVisits() {
        when(visitRepository.findAllByOrderByScheduledAtAsc()).thenReturn(List.of(visit()));

        var result = visitService.getVisibleVisits("admin@example.com", RoleName.ADMIN);

        assertEquals(1, result.size());
        verify(visitRepository).findAllByOrderByScheduledAtAsc();
    }

    @Test
    void customerCanCancelTheirOwnPendingVisit() {
        PropertyVisit visit = visit();
        when(visitRepository.findByIdAndCustomer_EmailIgnoreCase(42L, "customer@example.com"))
                .thenReturn(Optional.of(visit));

        visitService.cancel(42L, "CUSTOMER@example.com", RoleName.CUSTOMER);

        assertEquals("CANCELLED", visit.getStatus());
    }

    @Test
    void customerCannotCancelAnotherCustomersVisit() {
        when(visitRepository.findByIdAndCustomer_EmailIgnoreCase(42L, "customer@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(VisitNotFoundException.class, () -> visitService.cancel(
                42L, "customer@example.com", RoleName.CUSTOMER
        ));
    }

    @Test
    void agentCannotCancelVisits() {
        assertThrows(VisitAccessDeniedException.class, () -> visitService.cancel(
                42L, "agent@example.com", RoleName.AGENT
        ));
        verify(visitRepository, never()).findByIdAndCustomer_EmailIgnoreCase(any(), any());
    }

    @Test
    void assignedAgentCanAcceptPendingVisit() {
        PropertyVisit visit = visit();
        when(visitRepository.findByIdAndProperty_Agent_EmailIgnoreCase(42L, "agent@example.com"))
                .thenReturn(Optional.of(visit));
        when(visitRepository.save(visit)).thenReturn(visit);

        var result = visitService.decide(42L, PropertyVisitStatus.ACCEPTED, "AGENT@example.com", RoleName.AGENT);

        assertEquals("ACCEPTED", result.status());
        verify(visitRepository).findByIdAndProperty_Agent_EmailIgnoreCase(42L, "agent@example.com");
    }

    @Test
    void assignedAgentCanRejectPendingVisit() {
        PropertyVisit visit = visit();
        when(visitRepository.findByIdAndProperty_Agent_EmailIgnoreCase(42L, "agent@example.com"))
                .thenReturn(Optional.of(visit));
        when(visitRepository.save(visit)).thenReturn(visit);

        var result = visitService.decide(42L, PropertyVisitStatus.REJECTED, "agent@example.com", RoleName.AGENT);

        assertEquals("REJECTED", result.status());
    }

    @Test
    void agentCannotDecideVisitForAnotherAgentsProperty() {
        when(visitRepository.findByIdAndProperty_Agent_EmailIgnoreCase(42L, "other.agent@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(VisitNotFoundException.class, () -> visitService.decide(
                42L, PropertyVisitStatus.ACCEPTED, "other.agent@example.com", RoleName.AGENT
        ));
    }

    @Test
    void customerCannotDecideVisitAndResolvedVisitsCannotBeChangedAgain() {
        assertThrows(VisitAccessDeniedException.class, () -> visitService.decide(
                42L, PropertyVisitStatus.ACCEPTED, "customer@example.com", RoleName.CUSTOMER
        ));

        PropertyVisit acceptedVisit = visit();
        acceptedVisit.decide(PropertyVisitStatus.ACCEPTED);
        when(visitRepository.findByIdAndProperty_Agent_EmailIgnoreCase(42L, "agent@example.com"))
                .thenReturn(Optional.of(acceptedVisit));
        assertThrows(VisitStatusConflictException.class, () -> visitService.decide(
                42L, PropertyVisitStatus.REJECTED, "agent@example.com", RoleName.AGENT
        ));
        verify(visitRepository, never()).save(acceptedVisit);
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
