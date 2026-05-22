package com.securebank.config;

import com.securebank.model.Role;
import com.securebank.model.User;
import com.securebank.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedEnabled;
    private final String adminEmail;
    private final String adminPassword;

    public AdminUserSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.seed.enabled:false}") boolean seedEnabled,
            @Value("${admin.seed.email:}") String adminEmail,
            @Value("${admin.seed.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEnabled = seedEnabled;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        String normalizedEmail = normalizeEmail(adminEmail);
        if (normalizedEmail.isBlank() || adminPassword.isBlank()) {
            throw new IllegalStateException("Admin seed is enabled but ADMIN_EMAIL or ADMIN_PASSWORD is missing");
        }

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.info("Admin seed skipped because admin user already exists");
            return;
        }

        User admin = User.builder()
                .fullName("SecureBank Admin")
                .email(normalizedEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("Admin seed created admin user {}", normalizedEmail);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
