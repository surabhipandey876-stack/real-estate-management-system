package com.realestate.service;

import com.realestate.dto.PropertyResponse;
import com.realestate.exception.FavoriteAccessDeniedException;
import com.realestate.exception.FavoriteAlreadyExistsException;
import com.realestate.exception.FavoriteNotFoundException;
import com.realestate.exception.PropertyNotFoundException;
import com.realestate.model.Favorite;
import com.realestate.model.PropertyApprovalStatus;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.FavoriteRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository
    ) {
        this.favoriteRepository = favoriteRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PropertyResponse add(String customerEmail, Long propertyId) {
        String normalizedEmail = normalizeEmail(customerEmail);
        User customer = findCustomer(normalizedEmail);
        if (favoriteRepository.existsByCustomer_EmailIgnoreCaseAndProperty_Id(normalizedEmail, propertyId)) {
            throw new FavoriteAlreadyExistsException(propertyId);
        }
        var property = propertyRepository.findById(propertyId)
                .filter(candidate -> candidate.getApprovalStatus() == PropertyApprovalStatus.APPROVED)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));

        try {
            favoriteRepository.saveAndFlush(new Favorite(customer, property));
        } catch (DataIntegrityViolationException exception) {
            throw new FavoriteAlreadyExistsException(propertyId);
        }
        return toResponse(property);
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> getMine(String customerEmail) {
        String normalizedEmail = normalizeEmail(customerEmail);
        findCustomer(normalizedEmail);
        return favoriteRepository.findAllByCustomer_EmailIgnoreCaseAndProperty_ApprovalStatusOrderByCreatedAtDesc(
                        normalizedEmail, PropertyApprovalStatus.APPROVED
                ).stream()
                .map(favorite -> toResponse(favorite.getProperty()))
                .toList();
    }

    @Transactional
    public void remove(String customerEmail, Long propertyId) {
        String normalizedEmail = normalizeEmail(customerEmail);
        findCustomer(normalizedEmail);
        Favorite favorite = favoriteRepository.findByCustomer_EmailIgnoreCaseAndProperty_Id(
                        normalizedEmail,
                        propertyId
                )
                .orElseThrow(() -> new FavoriteNotFoundException(propertyId));
        favoriteRepository.delete(favorite);
    }

    private User findCustomer(String email) {
        User customer = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(FavoriteAccessDeniedException::new);
        if (customer.getRole().getName() != RoleName.CUSTOMER) {
            throw new FavoriteAccessDeniedException();
        }
        return customer;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private PropertyResponse toResponse(com.realestate.model.Property property) {
        return new PropertyResponse(
                property.getId(),
                property.getTitle(),
                property.getDescription(),
                property.getPropertyType(),
                property.getPrice(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getAreaSqft(),
                property.getCity(),
                property.getAddress(),
                property.getStatus(),
                property.getApprovalStatus(),
                property.getAgent().getId(),
                property.getCreatedAt(),
                property.getUpdatedAt()
        );
    }
}
