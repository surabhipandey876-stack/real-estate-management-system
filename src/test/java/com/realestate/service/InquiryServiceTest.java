package com.realestate.service;

import com.realestate.dto.InquiryRequest;
import com.realestate.dto.InquiryReplyRequest;
import com.realestate.exception.InquiryAccessDeniedException;
import com.realestate.exception.PropertyNotFoundException;
import com.realestate.model.Inquiry;
import com.realestate.model.Property;
import com.realestate.model.PropertyApprovalStatus;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.InquiryRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InquiryServiceTest {

    private InquiryRepository inquiryRepository;
    private PropertyRepository propertyRepository;
    private UserRepository userRepository;
    private InquiryService inquiryService;

    @BeforeEach
    void setUp() {
        inquiryRepository = mock(InquiryRepository.class);
        propertyRepository = mock(PropertyRepository.class);
        userRepository = mock(UserRepository.class);
        inquiryService = new InquiryService(inquiryRepository, propertyRepository, userRepository);
    }

    @Test
    void submitStoresInquiryForAuthenticatedCustomerAndProperty() {
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer()));
        when(propertyRepository.findById(9L)).thenReturn(Optional.of(property()));
        when(inquiryRepository.saveAndFlush(any(Inquiry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = inquiryService.submit(
                new InquiryRequest(9L, "  Please call me.  "),
                "CUSTOMER@example.com",
                RoleName.CUSTOMER
        );

        assertEquals("Please call me.", result.message());
        assertEquals("OPEN", result.status());
        verify(inquiryRepository).saveAndFlush(any(Inquiry.class));
    }

    @Test
    void customerCannotSubmitInquiryForMissingProperty() {
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer()));
        when(propertyRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(PropertyNotFoundException.class, () -> inquiryService.submit(
                new InquiryRequest(9L, "Please call me."),
                "customer@example.com",
                RoleName.CUSTOMER
        ));
        verify(inquiryRepository, never()).saveAndFlush(any(Inquiry.class));
    }

    @Test
    void customerCannotSubmitInquiryForUnapprovedProperty() {
        Property pending = property();
        pending.setApprovalStatus(PropertyApprovalStatus.PENDING);
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer()));
        when(propertyRepository.findById(9L)).thenReturn(Optional.of(pending));

        assertThrows(PropertyNotFoundException.class, () -> inquiryService.submit(
                new InquiryRequest(9L, "Please call me."),
                "customer@example.com",
                RoleName.CUSTOMER
        ));
        verify(inquiryRepository, never()).saveAndFlush(any(Inquiry.class));
    }

    @Test
    void onlyCustomersCanSubmitInquiries() {
        assertThrows(InquiryAccessDeniedException.class, () -> inquiryService.submit(
                new InquiryRequest(9L, "Please call me."),
                "agent@example.com",
                RoleName.AGENT
        ));
        verify(userRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void customerListUsesOnlyTheirOwnEmail() {
        when(inquiryRepository.findAllByCustomer_EmailIgnoreCaseOrderByCreatedAtDesc("customer@example.com"))
                .thenReturn(List.of(inquiry()));

        var results = inquiryService.getVisibleInquiries("CUSTOMER@example.com", RoleName.CUSTOMER);

        assertEquals(1, results.size());
        verify(inquiryRepository).findAllByCustomer_EmailIgnoreCaseOrderByCreatedAtDesc("customer@example.com");
    }

    @Test
    void agentListIsScopedToPropertiesTheyOwn() {
        when(inquiryRepository.findAllByProperty_Agent_EmailIgnoreCaseOrderByCreatedAtDesc("agent@example.com"))
                .thenReturn(List.of(inquiry()));

        var results = inquiryService.getVisibleInquiries("AGENT@example.com", RoleName.AGENT);

        assertEquals(1, results.size());
        verify(inquiryRepository).findAllByProperty_Agent_EmailIgnoreCaseOrderByCreatedAtDesc("agent@example.com");
    }

    @Test
    void adminCanListAllInquiries() {
        when(inquiryRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(inquiry()));

        var results = inquiryService.getVisibleInquiries("admin@example.com", RoleName.ADMIN);

        assertEquals(1, results.size());
        verify(inquiryRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void agentCanReplyOnlyToInquiriesForTheirProperties() {
        when(inquiryRepository.findByIdAndProperty_Agent_EmailIgnoreCase(17L, "agent@example.com"))
                .thenReturn(Optional.of(inquiry()));
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = inquiryService.reply(
                17L,
                new InquiryReplyRequest("  I will call you shortly.  "),
                "AGENT@example.com",
                RoleName.AGENT
        );

        assertEquals("I will call you shortly.", result.responseMessage());
        assertEquals("ANSWERED", result.status());
        verify(inquiryRepository).findByIdAndProperty_Agent_EmailIgnoreCase(17L, "agent@example.com");
    }

    @Test
    void customerCannotReplyToAnInquiry() {
        assertThrows(InquiryAccessDeniedException.class, () -> inquiryService.reply(
                17L,
                new InquiryReplyRequest("No customer replies allowed."),
                "customer@example.com",
                RoleName.CUSTOMER
        ));
        verify(inquiryRepository, never()).findById(any());
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

    private Inquiry inquiry() {
        return new Inquiry(customer(), property(), "Please contact me about this home.");
    }
}
