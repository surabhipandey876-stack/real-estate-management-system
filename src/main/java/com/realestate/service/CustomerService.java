package com.realestate.service;

import com.realestate.dto.UpdateCustomerProfileRequest;
import com.realestate.dto.UserResponse;
import com.realestate.exception.CustomerAccessDeniedException;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class CustomerService {

    private final UserRepository userRepository;

    public CustomerService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email, RoleName requesterRole) {
        return toResponse(findCustomer(email, requesterRole));
    }

    @Transactional
    public UserResponse updateProfile(
            String email,
            RoleName requesterRole,
            UpdateCustomerProfileRequest request
    ) {
        User customer = findCustomer(email, requesterRole);
        customer.updateProfile(
                request.firstName().trim(),
                request.lastName().trim(),
                customer.getEmail(),
                request.phoneNumber() == null ? null : request.phoneNumber().trim()
        );
        return toResponse(userRepository.save(customer));
    }

    private User findCustomer(String email, RoleName requesterRole) {
        if (requesterRole != RoleName.CUSTOMER) {
            throw new CustomerAccessDeniedException();
        }
        return userRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .filter(user -> user.getRole().getName() == RoleName.CUSTOMER)
                .orElseThrow(CustomerAccessDeniedException::new);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole().getName(),
                user.isActive()
        );
    }
}
