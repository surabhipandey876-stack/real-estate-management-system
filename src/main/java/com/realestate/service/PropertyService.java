package com.realestate.service;

import com.realestate.dto.PropertyRequest;
import com.realestate.dto.PropertyResponse;
import com.realestate.dto.PropertySearchCriteria;
import com.realestate.exception.InvalidPropertyRequestException;
import com.realestate.exception.PropertyAccessDeniedException;
import com.realestate.exception.PropertyInUseException;
import com.realestate.exception.PropertyNotFoundException;
import com.realestate.model.Property;
import com.realestate.model.PropertyApprovalStatus;
import com.realestate.model.PropertyImage;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.PropertyImageRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final UserRepository userRepository;
    private final PropertyImageStorage propertyImageStorage;

    public PropertyService(
            PropertyRepository propertyRepository,
            PropertyImageRepository propertyImageRepository,
            UserRepository userRepository,
            PropertyImageStorage propertyImageStorage
    ) {
        this.propertyRepository = propertyRepository;
        this.propertyImageRepository = propertyImageRepository;
        this.userRepository = userRepository;
        this.propertyImageStorage = propertyImageStorage;
    }

    @Transactional
    public PropertyResponse create(PropertyRequest request, String requesterEmail, RoleName requesterRole) {
        User owner;
        if (requesterRole == RoleName.AGENT) {
            if (request.agentId() != null) {
                throw new PropertyAccessDeniedException();
            }
            owner = findAgentByEmail(requesterEmail);
        } else if (requesterRole == RoleName.ADMIN) {
            if (request.agentId() == null) {
                throw new InvalidPropertyRequestException("agentId is required when an admin creates a property.");
            }
            owner = findAgentById(request.agentId());
        } else {
            throw new PropertyAccessDeniedException();
        }

        Property property = new Property(
                request.title().trim(),
                request.description(),
                request.propertyType().trim(),
                request.price(),
                request.bedrooms(),
                request.bathrooms(),
                request.areaSqft(),
                request.city().trim(),
                request.address().trim(),
                request.status().trim(),
                owner,
                requesterRole == RoleName.AGENT
                        ? PropertyApprovalStatus.PENDING
                        : PropertyApprovalStatus.APPROVED
        );
        return toResponse(propertyRepository.save(property));
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> search(PropertySearchCriteria criteria) {
        return search(criteria, null, RoleName.ADMIN);
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> search(PropertySearchCriteria criteria, String requesterEmail, RoleName requesterRole) {
        if (criteria.minPrice() != null && criteria.maxPrice() != null
                && criteria.minPrice().compareTo(criteria.maxPrice()) > 0) {
            throw new InvalidPropertyRequestException("minPrice cannot be greater than maxPrice.");
        }

        Specification<Property> specification = (root, query, builder) -> builder.conjunction();
        if (hasText(criteria.city())) {
            String city = criteria.city().trim().toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, builder) ->
                    builder.or(
                            builder.like(builder.lower(root.get("city")), "%" + city + "%"),
                            builder.like(builder.lower(root.get("address")), "%" + city + "%")
                    ));
        }
        if (hasText(criteria.propertyType())) {
            String propertyType = criteria.propertyType().trim().toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, builder) ->
                    builder.equal(builder.lower(root.get("propertyType")), propertyType));
        }
        if (criteria.minPrice() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
        }
        if (criteria.maxPrice() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
        }
        if (criteria.minBedrooms() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("bedrooms"), criteria.minBedrooms()));
        }
        if (hasText(criteria.status())) {
            String status = criteria.status().trim().toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, builder) ->
                    builder.equal(builder.lower(root.get("status")), status));
        }
        if (requesterRole == RoleName.AGENT) {
            String email = normalizeEmail(requesterEmail);
            specification = specification.and((root, query, builder) ->
                    builder.equal(builder.lower(root.get("agent").get("email")), email));
        }
        if (requesterRole == RoleName.CUSTOMER || requesterRole == null) {
            specification = specification.and((root, query, builder) ->
                    builder.and(
                            builder.equal(root.get("approvalStatus"), PropertyApprovalStatus.APPROVED),
                            builder.equal(builder.lower(root.get("status")), "available")
                    ));
        }

        return propertyRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PropertyResponse getById(Long id) {
        return toResponse(propertyRepository.findById(id).orElseThrow(() -> new PropertyNotFoundException(id)));
    }

    @Transactional(readOnly = true)
    public PropertyResponse getById(Long id, String requesterEmail, RoleName requesterRole) {
        Property property;
        if (requesterRole == RoleName.AGENT) {
            property = propertyRepository.findByIdAndAgent_EmailIgnoreCase(id, normalizeEmail(requesterEmail))
                    .orElseGet(() -> {
                        if (propertyRepository.existsById(id)) {
                            throw new PropertyAccessDeniedException();
                        }
                        throw new PropertyNotFoundException(id);
                    });
        } else {
            property = propertyRepository.findById(id)
                    .orElseThrow(() -> new PropertyNotFoundException(id));
        }
        if ((requesterRole == RoleName.CUSTOMER || requesterRole == null)
                && (property.getApprovalStatus() != PropertyApprovalStatus.APPROVED
                || !"AVAILABLE".equalsIgnoreCase(property.getStatus()))) {
            throw new PropertyNotFoundException(id);
        }
        return toResponse(property);
    }

    @Transactional
    public PropertyResponse setApprovalStatus(
            Long propertyId,
            PropertyApprovalStatus approvalStatus,
            RoleName requesterRole
    ) {
        if (requesterRole != RoleName.ADMIN
                || approvalStatus == PropertyApprovalStatus.PENDING) {
            throw new PropertyAccessDeniedException();
        }
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));
        property.setApprovalStatus(approvalStatus);
        return toResponse(propertyRepository.save(property));
    }

    @Transactional
    public PropertyResponse update(
            Long id,
            PropertyRequest request,
            String requesterEmail,
            RoleName requesterRole
    ) {
        Property property = findManageableProperty(id, requesterEmail, requesterRole);
        property.updateDetails(
                request.title().trim(),
                request.description(),
                request.propertyType().trim(),
                request.price(),
                request.bedrooms(),
                request.bathrooms(),
                request.areaSqft(),
                request.city().trim(),
                request.address().trim(),
                request.status().trim()
        );

        if (request.agentId() != null) {
            if (requesterRole != RoleName.ADMIN) {
                throw new PropertyAccessDeniedException();
            }
            property.assignAgent(findAgentById(request.agentId()));
        }
        return toResponse(propertyRepository.save(property));
    }

    @Transactional
    public void delete(Long id, String requesterEmail, RoleName requesterRole) {
        Property property = findManageableProperty(id, requesterEmail, requesterRole);
        List<PropertyImage> images = propertyImageRepository.findAllByProperty_Id(id);
        try {
            propertyRepository.delete(property);
            propertyRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new PropertyInUseException(id, exception);
        }
        images.forEach(image -> propertyImageStorage.delete(image.getImageUrl()));
    }

    public Property findManageableProperty(Long id, String requesterEmail, RoleName requesterRole) {
        if (requesterRole == RoleName.ADMIN) {
            return propertyRepository.findById(id).orElseThrow(() -> new PropertyNotFoundException(id));
        }
        if (requesterRole != RoleName.AGENT) {
            throw new PropertyAccessDeniedException();
        }
        return propertyRepository.findByIdAndAgent_EmailIgnoreCase(id, normalizeEmail(requesterEmail))
                .orElseGet(() -> {
                    if (propertyRepository.existsById(id)) {
                        throw new PropertyAccessDeniedException();
                    }
                    throw new PropertyNotFoundException(id);
                });
    }

    private User findAgentById(Long agentId) {
        User user = userRepository.findById(agentId)
                .orElseThrow(() -> new InvalidPropertyRequestException("No agent exists with id " + agentId + "."));
        if (user.getRole().getName() != RoleName.AGENT) {
            throw new InvalidPropertyRequestException("The property owner must have the AGENT role.");
        }
        return user;
    }

    private User findAgentByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(PropertyAccessDeniedException::new);
        if (user.getRole().getName() != RoleName.AGENT) {
            throw new PropertyAccessDeniedException();
        }
        return user;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private PropertyResponse toResponse(Property property) {
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
