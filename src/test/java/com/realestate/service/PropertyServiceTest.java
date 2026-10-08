package com.realestate.service;

import com.realestate.dto.PropertyRequest;
import com.realestate.dto.PropertySearchCriteria;
import com.realestate.exception.InvalidPropertyRequestException;
import com.realestate.exception.PropertyAccessDeniedException;
import com.realestate.model.Property;
import com.realestate.model.PropertyApprovalStatus;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.PropertyImageRepository;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyServiceTest {

    private PropertyRepository propertyRepository;
    private PropertyImageRepository propertyImageRepository;
    private UserRepository userRepository;
    private PropertyImageStorage propertyImageStorage;
    private PropertyService propertyService;

    @BeforeEach
    void setUp() {
        propertyRepository = mock(PropertyRepository.class);
        propertyImageRepository = mock(PropertyImageRepository.class);
        userRepository = mock(UserRepository.class);
        propertyImageStorage = mock(PropertyImageStorage.class);
        propertyService = new PropertyService(
                propertyRepository,
                propertyImageRepository,
                userRepository,
                propertyImageStorage
        );
    }

    @Test
    void agentCreatesPropertyOwnedByTheirAccount() {
        User agent = user("agent@example.com", RoleName.AGENT);
        when(userRepository.findByEmailIgnoreCase("agent@example.com")).thenReturn(Optional.of(agent));
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = propertyService.create(request(null), "agent@example.com", RoleName.AGENT);

        ArgumentCaptor<Property> propertyCaptor = ArgumentCaptor.forClass(Property.class);
        verify(propertyRepository).save(propertyCaptor.capture());
        assertEquals("agent@example.com", propertyCaptor.getValue().getAgent().getEmail());
        assertEquals("Example home", result.title());
        assertEquals(PropertyApprovalStatus.PENDING, result.approvalStatus());
    }

    @Test
    void agentCannotUpdateAnotherAgentsProperty() {
        when(propertyRepository.findByIdAndAgent_EmailIgnoreCase(4L, "agent@example.com"))
                .thenReturn(Optional.empty());
        when(propertyRepository.existsById(4L)).thenReturn(true);

        assertThrows(PropertyAccessDeniedException.class, () -> propertyService.update(
                4L, request(null), "agent@example.com", RoleName.AGENT
        ));
    }

    @Test
    void customerCannotCreateAProperty() {
        assertThrows(PropertyAccessDeniedException.class, () -> propertyService.create(
                request(null), "customer@example.com", RoleName.CUSTOMER
        ));
    }

    @Test
    void adminCanReassignPropertyToAnAgent() {
        User owner = user("agent@example.com", RoleName.AGENT);
        Property property = property(owner);
        User newOwner = user("new-agent@example.com", RoleName.AGENT);
        when(propertyRepository.findById(4L)).thenReturn(Optional.of(property));
        when(userRepository.findById(7L)).thenReturn(Optional.of(newOwner));
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        propertyService.update(4L, request(7L), "admin@example.com", RoleName.ADMIN);

        assertEquals(newOwner, property.getAgent());
    }

    @Test
    void adminCreatedPropertyIsApprovedImmediately() {
        User owner = user("agent@example.com", RoleName.AGENT);
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = propertyService.create(request(7L), "admin@example.com", RoleName.ADMIN);

        assertEquals(PropertyApprovalStatus.APPROVED, result.approvalStatus());
    }

    @Test
    void adminCanApproveOrRejectAnyProperty() {
        Property property = property(user("agent@example.com", RoleName.AGENT));
        when(propertyRepository.findById(4L)).thenReturn(Optional.of(property));
        when(propertyRepository.save(property)).thenReturn(property);

        var approved = propertyService.setApprovalStatus(4L, PropertyApprovalStatus.APPROVED, RoleName.ADMIN);
        assertEquals(PropertyApprovalStatus.APPROVED, approved.approvalStatus());
        var rejected = propertyService.setApprovalStatus(4L, PropertyApprovalStatus.REJECTED, RoleName.ADMIN);
        assertEquals(PropertyApprovalStatus.REJECTED, rejected.approvalStatus());
    }

    @Test
    void agentAndCustomerCannotApproveOrRejectProperties() {
        assertThrows(PropertyAccessDeniedException.class, () -> propertyService.setApprovalStatus(
                4L, PropertyApprovalStatus.APPROVED, RoleName.AGENT
        ));
        assertThrows(PropertyAccessDeniedException.class, () -> propertyService.setApprovalStatus(
                4L, PropertyApprovalStatus.REJECTED, RoleName.CUSTOMER
        ));
    }

    @Test
    void customerCannotViewUnapprovedPropertyById() {
        Property pending = property(user("agent@example.com", RoleName.AGENT));
        pending.setApprovalStatus(PropertyApprovalStatus.PENDING);
        when(propertyRepository.findById(4L)).thenReturn(Optional.of(pending));

        assertThrows(com.realestate.exception.PropertyNotFoundException.class, () -> propertyService.getById(
                4L, "customer@example.com", RoleName.CUSTOMER
        ));
    }

    @Test
    void anonymousCannotViewUnapprovedOrUnavailablePropertyById() {
        Property pending = property(user("agent@example.com", RoleName.AGENT));
        pending.setApprovalStatus(PropertyApprovalStatus.PENDING);
        when(propertyRepository.findById(4L)).thenReturn(Optional.of(pending));

        assertThrows(com.realestate.exception.PropertyNotFoundException.class, () -> propertyService.getById(
                4L, null, null
        ));

        Property sold = property(user("agent@example.com", RoleName.AGENT));
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
        when(propertyRepository.findById(5L)).thenReturn(Optional.of(sold));
        assertThrows(com.realestate.exception.PropertyNotFoundException.class, () -> propertyService.getById(
                5L, null, null
        ));
    }

    @Test
    void agentCannotViewAnotherAgentsPropertyById() {
        when(propertyRepository.findByIdAndAgent_EmailIgnoreCase(4L, "agent@example.com"))
                .thenReturn(Optional.empty());
        when(propertyRepository.existsById(4L)).thenReturn(true);

        assertThrows(PropertyAccessDeniedException.class, () -> propertyService.getById(
                4L, "agent@example.com", RoleName.AGENT
        ));
    }

    @Test
    void searchReturnsMatchingRepositoryResultsInDescendingCreationOrder() {
        when(propertyRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Property>>any(), any(Sort.class)))
                .thenReturn(java.util.List.of(property(user("agent@example.com", RoleName.AGENT))));

        var results = propertyService.search(new PropertySearchCriteria(
                " Sample ", "HOUSE", new BigDecimal("100000"), new BigDecimal("400000"), 2, "AVAILABLE"
        ));

        assertEquals(1, results.size());
        assertEquals("Example home", results.get(0).title());
        verify(propertyRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Property>>any(), any(Sort.class));
    }

    @Test
    void searchRejectsMinimumPriceAboveMaximumPrice() {
        assertThrows(InvalidPropertyRequestException.class, () -> propertyService.search(
                new PropertySearchCriteria(
                        null, null, new BigDecimal("400000"), new BigDecimal("100000"), null, null
                )
        ));
    }

    private PropertyRequest request(Long agentId) {
        return new PropertyRequest(
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
                agentId
        );
    }

    private User user(String email, RoleName roleName) {
        return new User("Pat", "Example", email, "not-used", null, new Role(roleName));
    }

    private Property property(User owner) {
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
                owner
        );
    }
}
