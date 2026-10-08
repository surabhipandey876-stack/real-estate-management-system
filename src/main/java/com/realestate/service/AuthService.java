package com.realestate.service;

import com.realestate.dto.AuthResponse;
import com.realestate.dto.LoginRequest;
import com.realestate.dto.RegisterRequest;
import com.realestate.dto.UserResponse;
import com.realestate.exception.DuplicateEmailException;
import com.realestate.exception.InvalidCredentialsException;
import com.realestate.exception.RoleNotFoundException;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.RoleRepository;
import com.realestate.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new RoleNotFoundException(RoleName.CUSTOMER));
        User user = new User(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                request.phoneNumber() == null ? null : request.phoneNumber().trim(),
                customerRole
        );

        return toUserResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        JwtService.IssuedToken token = jwtService.issueToken(user.getEmail(), user.getRole().getName());
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), toUserResponse(user));
    }

    private UserResponse toUserResponse(User user) {
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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
