package com.realestate.service;

import com.realestate.dto.AdminStatisticsResponse;
import com.realestate.dto.RegisterRequest;
import com.realestate.dto.UpdateAgentRequest;
import com.realestate.dto.UpdateUserRoleRequest;
import com.realestate.dto.UserResponse;
import com.realestate.exception.DuplicateEmailException;
import com.realestate.exception.AgentInUseException;
import com.realestate.exception.InvalidPropertyRequestException;
import com.realestate.exception.RoleNotFoundException;
import com.realestate.exception.UserNotFoundException;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.InquiryRepository;
import com.realestate.repository.FavoriteRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.PropertyVisitRepository;
import com.realestate.repository.RoleRepository;
import com.realestate.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PropertyRepository propertyRepository;
    private final InquiryRepository inquiryRepository;
    private final PropertyVisitRepository visitRepository;
    private final FavoriteRepository favoriteRepository;

    public AdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            PropertyRepository propertyRepository,
            InquiryRepository inquiryRepository,
            PropertyVisitRepository visitRepository,
            FavoriteRepository favoriteRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.propertyRepository = propertyRepository;
        this.inquiryRepository = inquiryRepository;
        this.visitRepository = visitRepository;
        this.favoriteRepository = favoriteRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(User::getLastName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAgents() {
        return userRepository.findAllByRole_Name(RoleName.AGENT).stream()
                .sorted(Comparator.comparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(User::getLastName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public UserResponse createAgent(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }
        Role agentRole = roleRepository.findByName(RoleName.AGENT)
                .orElseThrow(() -> new RoleNotFoundException(RoleName.AGENT));
        User agent = new User(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                request.phoneNumber() == null ? null : request.phoneNumber().trim(),
                agentRole
        );
        return toUserResponse(userRepository.save(agent));
    }

    @Transactional
    public UserResponse updateAgent(Long agentId, UpdateAgentRequest request) {
        User agent = getAgent(agentId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, agentId)) {
            throw new DuplicateEmailException();
        }
        agent.updateProfile(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                request.phoneNumber() == null || request.phoneNumber().isBlank()
                        ? null
                        : request.phoneNumber().trim()
        );
        return toUserResponse(userRepository.save(agent));
    }

    @Transactional
    public UserResponse deactivateAgent(Long agentId) {
        User agent = getAgent(agentId);
        agent.deactivate();
        return toUserResponse(userRepository.save(agent));
    }

    @Transactional
    public UserResponse activateAgent(Long agentId) {
        User agent = getAgent(agentId);
        agent.activate();
        return toUserResponse(userRepository.save(agent));
    }

    @Transactional
    public void deleteAgent(Long agentId) {
        User agent = getAgent(agentId);
        if (propertyRepository.existsByAgent_Id(agentId)
                || visitRepository.existsByAgent_Id(agentId)
                || visitRepository.existsByCustomer_Id(agentId)
                || inquiryRepository.existsByCustomer_Id(agentId)
                || favoriteRepository.existsByCustomer_Id(agentId)) {
            throw new AgentInUseException(agentId);
        }
        userRepository.delete(agent);
    }

    @Transactional(readOnly = true)
    public AdminStatisticsResponse getStatistics() {
        return new AdminStatisticsResponse(
                userRepository.count(),
                userRepository.countByRole_Name(RoleName.AGENT),
                userRepository.countByRole_Name(RoleName.CUSTOMER),
                propertyRepository.count(),
                propertyRepository.countByStatusIgnoreCase("AVAILABLE"),
                propertyRepository.countByStatusIgnoreCase("SOLD")
                        + propertyRepository.countByStatusIgnoreCase("RENTED"),
                inquiryRepository.count(),
                inquiryRepository.countByStatusIgnoreCase("OPEN"),
                visitRepository.count(),
                visitRepository.countByStatusIgnoreCase("PENDING")
        );
    }

    @Transactional
    public UserResponse updateUserRole(Long userId, UpdateUserRoleRequest request, String requesterEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        RoleName currentRole = user.getRole().getName();
        RoleName newRole = request.role();
        if (user.getEmail().equalsIgnoreCase(requesterEmail.trim()) && currentRole != newRole) {
            throw new InvalidPropertyRequestException("You cannot change your own role.");
        }
        if (currentRole == RoleName.ADMIN && newRole != RoleName.ADMIN
                && userRepository.countByRole_Name(RoleName.ADMIN) <= 1) {
            throw new InvalidPropertyRequestException("The last administrator cannot be demoted.");
        }
        if (currentRole == RoleName.AGENT && newRole != RoleName.AGENT
                && propertyRepository.existsByAgent_Id(userId)) {
            throw new InvalidPropertyRequestException("Reassign this agent's properties before changing their role.");
        }
        user.changeRole(roleRepository.findByName(newRole)
                .orElseThrow(() -> new InvalidPropertyRequestException("Role is not configured: " + newRole)));
        return toUserResponse(userRepository.save(user));
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

    private User getAgent(Long agentId) {
        User user = userRepository.findById(agentId).orElseThrow(() -> new UserNotFoundException(agentId));
        if (user.getRole().getName() != RoleName.AGENT) {
            throw new InvalidPropertyRequestException("User " + agentId + " is not an agent.");
        }
        return user;
    }
}
