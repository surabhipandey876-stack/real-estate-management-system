package com.realestate.config;

import com.realestate.model.Property;
import com.realestate.model.PropertyApprovalStatus;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Seeds a few approved listings for the local development/demo environment. */
@Component
@Profile("dev")
public class DevelopmentPropertyInitializer implements org.springframework.boot.ApplicationRunner {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public DevelopmentPropertyInitializer(PropertyRepository propertyRepository, UserRepository userRepository) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (propertyRepository.count() > 0) {
            return;
        }

        User agent = userRepository.findByEmailIgnoreCase("dev.agent@hearthhome.local").orElse(null);
        if (agent == null || agent.getRole().getName() != RoleName.AGENT) {
            return;
        }

        propertyRepository.save(new Property(
                "Modern 3 BHK Apartment",
                "Bright apartment with modern interiors and convenient amenities.",
                "APARTMENT",
                new BigDecimal("8500000"),
                3, 2, 1450,
                "Noida",
                "Sector 137, Noida",
                "AVAILABLE",
                agent,
                PropertyApprovalStatus.APPROVED
        ));
        propertyRepository.save(new Property(
                "Family Villa with Garden",
                "Spacious family villa with parking and a private garden.",
                "VILLA",
                new BigDecimal("15500000"),
                4, 3, 2400,
                "Greater Noida",
                "Sector Alpha, Greater Noida",
                "AVAILABLE",
                agent,
                PropertyApprovalStatus.APPROVED
        ));
        propertyRepository.save(new Property(
                "Cozy 2 BHK Home",
                "Well-connected 2 BHK home suitable for a small family.",
                "HOUSE",
                new BigDecimal("6200000"),
                2, 2, 1100,
                "Lucknow",
                "Gomti Nagar, Lucknow",
                "AVAILABLE",
                agent,
                PropertyApprovalStatus.APPROVED
        ));
    }
}
