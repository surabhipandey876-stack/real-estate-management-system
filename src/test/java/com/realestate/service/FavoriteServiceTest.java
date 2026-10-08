package com.realestate.service;

import com.realestate.exception.FavoriteAccessDeniedException;
import com.realestate.exception.FavoriteAlreadyExistsException;
import com.realestate.exception.FavoriteNotFoundException;
import com.realestate.exception.PropertyNotFoundException;
import com.realestate.model.Favorite;
import com.realestate.model.Property;
import com.realestate.model.PropertyApprovalStatus;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.FavoriteRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

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

class FavoriteServiceTest {

    private FavoriteRepository favoriteRepository;
    private PropertyRepository propertyRepository;
    private UserRepository userRepository;
    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteRepository = mock(FavoriteRepository.class);
        propertyRepository = mock(PropertyRepository.class);
        userRepository = mock(UserRepository.class);
        favoriteService = new FavoriteService(favoriteRepository, propertyRepository, userRepository);
    }

    @Test
    void addingFavoriteAssociatesItWithTheAuthenticatedCustomer() {
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer()));
        when(favoriteRepository.existsByCustomer_EmailIgnoreCaseAndProperty_Id("customer@example.com", 9L))
                .thenReturn(false);
        when(propertyRepository.findById(9L)).thenReturn(Optional.of(property()));

        favoriteService.add("CUSTOMER@example.com", 9L);

        verify(favoriteRepository).saveAndFlush(any(Favorite.class));
    }

    @Test
    void duplicateFavoriteIsRejected() {
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer()));
        when(favoriteRepository.existsByCustomer_EmailIgnoreCaseAndProperty_Id("customer@example.com", 9L))
                .thenReturn(true);

        assertThrows(FavoriteAlreadyExistsException.class, () -> favoriteService.add("customer@example.com", 9L));
        verify(favoriteRepository, never()).saveAndFlush(any(Favorite.class));
    }

    @Test
    void uniqueConstraintRaceIsReportedAsDuplicateFavorite() {
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer()));
        when(favoriteRepository.existsByCustomer_EmailIgnoreCaseAndProperty_Id("customer@example.com", 9L))
                .thenReturn(false);
        when(propertyRepository.findById(9L)).thenReturn(Optional.of(property()));
        when(favoriteRepository.saveAndFlush(any(Favorite.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate pair"));

        assertThrows(FavoriteAlreadyExistsException.class, () -> favoriteService.add("customer@example.com", 9L));
    }

    @Test
    void customerCanReadOnlyTheirOwnFavorites() {
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer()));
        when(favoriteRepository.findAllByCustomer_EmailIgnoreCaseAndProperty_ApprovalStatusOrderByCreatedAtDesc(
                "customer@example.com", PropertyApprovalStatus.APPROVED
        ))
                .thenReturn(List.of(new Favorite(customer(), property())));

        var result = favoriteService.getMine("customer@example.com");

        assertEquals(1, result.size());
        assertEquals("Favorite house", result.get(0).title());
    }

    @Test
    void customerCanRemoveTheirOwnFavoriteOnly() {
        Favorite favorite = new Favorite(customer(), property());
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer()));
        when(favoriteRepository.findByCustomer_EmailIgnoreCaseAndProperty_Id("customer@example.com", 9L))
                .thenReturn(Optional.of(favorite));

        favoriteService.remove("customer@example.com", 9L);

        verify(favoriteRepository).delete(favorite);
    }

    @Test
    void anotherCustomersFavoriteCannotBeRemoved() {
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer()));
        when(favoriteRepository.findByCustomer_EmailIgnoreCaseAndProperty_Id("customer@example.com", 9L))
                .thenReturn(Optional.empty());

        assertThrows(FavoriteNotFoundException.class, () -> favoriteService.remove("customer@example.com", 9L));
    }

    @Test
    void addingUnknownPropertyReturnsNotFound() {
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer()));
        when(favoriteRepository.existsByCustomer_EmailIgnoreCaseAndProperty_Id("customer@example.com", 9L))
                .thenReturn(false);
        when(propertyRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(PropertyNotFoundException.class, () -> favoriteService.add("customer@example.com", 9L));
    }

    @Test
    void nonCustomerAccountCannotUseFavoriteService() {
        when(userRepository.findByEmailIgnoreCase("agent@example.com"))
                .thenReturn(Optional.of(new User(
                        "Alex", "Agent", "agent@example.com", "hash", null, new Role(RoleName.AGENT)
                )));

        assertThrows(FavoriteAccessDeniedException.class, () -> favoriteService.getMine("agent@example.com"));
    }

    private User customer() {
        return new User("Casey", "Customer", "customer@example.com", "hash", null, new Role(RoleName.CUSTOMER));
    }

    private User agent() {
        return new User("Alex", "Agent", "agent@example.com", "hash", null, new Role(RoleName.AGENT));
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
