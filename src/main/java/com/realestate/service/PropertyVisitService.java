package com.realestate.service;

import com.realestate.dto.PropertyVisitRequest;
import com.realestate.dto.PropertyVisitResponse;
import com.realestate.exception.VisitStatusConflictException;
import com.realestate.exception.PropertyNotFoundException;
import com.realestate.exception.VisitAccessDeniedException;
import com.realestate.exception.VisitCannotBeCancelledException;
import com.realestate.exception.VisitNotFoundException;
import com.realestate.model.PropertyVisit;
import com.realestate.model.PropertyApprovalStatus;
import com.realestate.model.PropertyVisitStatus;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.PropertyVisitRepository;
import com.realestate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class PropertyVisitService {

    private final PropertyVisitRepository visitRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public PropertyVisitService(
            PropertyVisitRepository visitRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository
    ) {
        this.visitRepository = visitRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PropertyVisitResponse schedule(PropertyVisitRequest request, String customerEmail, RoleName role) {
        if (role != RoleName.CUSTOMER) {
            throw new VisitAccessDeniedException();
        }

        String email = normalizeEmail(customerEmail);
        User customer = userRepository.findByEmailIgnoreCase(email)
                .filter(user -> user.getRole().getName() == RoleName.CUSTOMER)
                .orElseThrow(VisitAccessDeniedException::new);
        var property = propertyRepository.findById(request.propertyId())
                .filter(candidate -> candidate.getApprovalStatus() == PropertyApprovalStatus.APPROVED)
                .orElseThrow(() -> new PropertyNotFoundException(request.propertyId()));
        PropertyVisit visit = visitRepository.save(new PropertyVisit(property, customer, request.scheduledAt()));
        return toResponse(visit);
    }

    @Transactional(readOnly = true)
    public List<PropertyVisitResponse> getVisibleVisits(String userEmail, RoleName role) {
        String email = normalizeEmail(userEmail);
        List<PropertyVisit> visits = switch (role) {
            case CUSTOMER -> visitRepository.findAllByCustomer_EmailIgnoreCaseOrderByScheduledAtAsc(email);
            case AGENT -> visitRepository.findAllByProperty_Agent_EmailIgnoreCaseOrderByScheduledAtAsc(email);
            case ADMIN -> visitRepository.findAllByOrderByScheduledAtAsc();
        };
        return visits.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void cancel(Long visitId, String customerEmail, RoleName role) {
        if (role != RoleName.CUSTOMER) {
            throw new VisitAccessDeniedException();
        }

        PropertyVisit visit = visitRepository.findByIdAndCustomer_EmailIgnoreCase(
                        visitId,
                        normalizeEmail(customerEmail)
                )
                .orElseThrow(() -> new VisitNotFoundException(visitId));
        if (!"PENDING".equals(visit.getStatus())) {
            throw new VisitCannotBeCancelledException(visitId);
        }
        visit.cancel();
    }

    @Transactional
    public PropertyVisitResponse decide(
            Long visitId,
            PropertyVisitStatus decision,
            String requesterEmail,
            RoleName role
    ) {
        if (decision != PropertyVisitStatus.ACCEPTED && decision != PropertyVisitStatus.REJECTED) {
            throw new VisitAccessDeniedException("Agents can only accept or reject visit requests.");
        }
        String email = normalizeEmail(requesterEmail);
        PropertyVisit visit;
        if (role == RoleName.ADMIN) {
            visit = visitRepository.findById(visitId)
                    .orElseThrow(() -> new VisitNotFoundException(visitId));
        } else if (role == RoleName.AGENT) {
            visit = visitRepository.findByIdAndProperty_Agent_EmailIgnoreCase(visitId, email)
                    .orElseThrow(() -> new VisitNotFoundException(visitId));
        } else {
            throw new VisitAccessDeniedException("Only the assigned agent or an admin can decide a visit request.");
        }
        if (!"PENDING".equals(visit.getStatus())) {
            throw new VisitStatusConflictException(visitId);
        }
        visit.decide(decision);
        return toResponse(visitRepository.save(visit));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private PropertyVisitResponse toResponse(PropertyVisit visit) {
        return new PropertyVisitResponse(
                visit.getId(),
                visit.getProperty().getId(),
                visit.getProperty().getTitle(),
                visit.getCustomer().getId(),
                visit.getCustomer().getEmail(),
                visit.getAgent().getId(),
                visit.getScheduledAt(),
                visit.getStatus(),
                visit.getCreatedAt()
        );
    }
}
