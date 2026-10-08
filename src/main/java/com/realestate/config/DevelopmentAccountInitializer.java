package com.realestate.config;

import com.realestate.exception.RoleNotFoundException;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.RoleRepository;
import com.realestate.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
@Profile("dev")
public class DevelopmentAccountInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DevelopmentAccountInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final DevelopmentAccount admin;
    private final DevelopmentAccount agent;
    private final DevelopmentAccount customer;

    public DevelopmentAccountInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.development-accounts.admin.first-name}") String adminFirstName,
            @Value("${app.development-accounts.admin.last-name}") String adminLastName,
            @Value("${app.development-accounts.admin.email}") String adminEmail,
            @Value("${app.development-accounts.admin.password}") String adminPassword,
            @Value("${app.development-accounts.agent.first-name}") String agentFirstName,
            @Value("${app.development-accounts.agent.last-name}") String agentLastName,
            @Value("${app.development-accounts.agent.email}") String agentEmail,
            @Value("${app.development-accounts.agent.password}") String agentPassword,
            @Value("${app.development-accounts.customer.first-name}") String customerFirstName,
            @Value("${app.development-accounts.customer.last-name}") String customerLastName,
            @Value("${app.development-accounts.customer.email}") String customerEmail,
            @Value("${app.development-accounts.customer.password}") String customerPassword
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.admin = new DevelopmentAccount(
                adminFirstName, adminLastName, adminEmail, adminPassword, RoleName.ADMIN
        );
        this.agent = new DevelopmentAccount(
                agentFirstName, agentLastName, agentEmail, agentPassword, RoleName.AGENT
        );
        this.customer = new DevelopmentAccount(
                customerFirstName, customerLastName, customerEmail, customerPassword, RoleName.CUSTOMER
        );
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        configure(admin);
        configure(agent);
        configure(customer);
    }

    private void configure(DevelopmentAccount account) {
        String email = account.email().trim().toLowerCase(Locale.ROOT);
        Role role = roleRepository.findByName(account.role())
                .orElseThrow(() -> new RoleNotFoundException(account.role()));
        String passwordHash = passwordEncoder.encode(account.password());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> new User(
                        account.firstName().trim(),
                        account.lastName().trim(),
                        email,
                        passwordHash,
                        null,
                        role
                ));
        boolean isNewAccount = user.getId() == null;
        user.configureDevelopmentLogin(passwordHash, role);
        userRepository.save(user);
        logger.info("{} development {} account {}", isNewAccount ? "Created" : "Updated", account.role(), email);
    }

    private record DevelopmentAccount(
            String firstName,
            String lastName,
            String email,
            String password,
            RoleName role
    ) {
    }
}
