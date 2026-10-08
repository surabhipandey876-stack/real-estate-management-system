package com.realestate.service;

import com.realestate.dto.InquiryRequest;
import com.realestate.dto.InquiryReplyRequest;
import com.realestate.dto.InquiryResponse;
import com.realestate.exception.InquiryAccessDeniedException;
import com.realestate.exception.InquiryNotFoundException;
import com.realestate.exception.PropertyNotFoundException;
import com.realestate.model.Inquiry;
import com.realestate.model.PropertyApprovalStatus;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.InquiryRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public InquiryService(
            InquiryRepository inquiryRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository
    ) {
        this.inquiryRepository = inquiryRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public InquiryResponse submit(InquiryRequest request, String customerEmail, RoleName role) {
        if (role != RoleName.CUSTOMER) {
            throw new InquiryAccessDeniedException();
        }

        String normalizedEmail = normalizeEmail(customerEmail);
        User customer = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(user -> user.getRole().getName() == RoleName.CUSTOMER)
                .orElseThrow(InquiryAccessDeniedException::new);
        var property = propertyRepository.findById(request.propertyId())
                .filter(candidate -> candidate.getApprovalStatus() == PropertyApprovalStatus.APPROVED)
                .orElseThrow(() -> new PropertyNotFoundException(request.propertyId()));

        Inquiry inquiry = inquiryRepository.saveAndFlush(
                new Inquiry(customer, property, request.message().trim())
        );
        return toResponse(inquiry);
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> getVisibleInquiries(String email, RoleName role) {
        String normalizedEmail = normalizeEmail(email);
        List<Inquiry> inquiries = switch (role) {
            case ADMIN -> inquiryRepository.findAllByOrderByCreatedAtDesc();
            case AGENT -> inquiryRepository.findAllByProperty_Agent_EmailIgnoreCaseOrderByCreatedAtDesc(normalizedEmail);
            case CUSTOMER -> inquiryRepository.findAllByCustomer_EmailIgnoreCaseOrderByCreatedAtDesc(normalizedEmail);
        };
        return inquiries.stream().map(this::toResponse).toList();
    }

    @Transactional
    public InquiryResponse reply(Long inquiryId, InquiryReplyRequest request, String requesterEmail, RoleName role) {
        Inquiry inquiry;
        if (role == RoleName.ADMIN) {
            inquiry = inquiryRepository.findById(inquiryId)
                    .orElseThrow(() -> new InquiryNotFoundException(inquiryId));
        } else if (role == RoleName.AGENT) {
            inquiry = inquiryRepository.findByIdAndProperty_Agent_EmailIgnoreCase(
                            inquiryId,
                            normalizeEmail(requesterEmail)
                    )
                    .orElseThrow(() -> new InquiryNotFoundException(inquiryId));
        } else {
            throw new InquiryAccessDeniedException();
        }
        inquiry.reply(request.message().trim());
        return toResponse(inquiryRepository.save(inquiry));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private InquiryResponse toResponse(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getProperty().getId(),
                inquiry.getProperty().getTitle(),
                inquiry.getCustomer().getId(),
                inquiry.getCustomer().getEmail(),
                inquiry.getMessage(),
                inquiry.getStatus(),
                inquiry.getCreatedAt(),
                inquiry.getResponseMessage(),
                inquiry.getRespondedAt()
        );
    }
}
