package com.example.saccoauthservice.config;

import com.example.saccoauthservice.entity.User;
import com.example.saccoauthservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.phone:}")
    private String bootstrapPhone;

    @Value("${app.bootstrap.admin.password:}")
    private String bootstrapPassword;

    @Value("${app.bootstrap.admin.pin:1234}")
    private String bootstrapPin;

    @Value("${app.bootstrap.admin.email:admin@example.com}")
    private String bootstrapEmail;

    @Override
    public void run(String... args) {
        if (bootstrapPhone == null || bootstrapPhone.isBlank()) {
            log.info("Admin bootstrap disabled (no phone configured)");
            return;
        }

        // Normalize phone to match your DB format (no leading +)
        String phone = bootstrapPhone.startsWith("+")
                ? bootstrapPhone.substring(1)
                : bootstrapPhone;

        if (userRepo.existsByPhoneNumber(phone)) {
            log.info("Admin bootstrap skipped — user {} already exists", phone);
            return;
        }

        User admin = User.builder()
                .id(UUID.randomUUID())     // ← ADD THIS LINE
                .phoneNumber(phone)
                .email(bootstrapEmail)
                .role(User.Role.ADMIN)
                .status(User.Status.ACTIVE)
                .passwordHash(passwordEncoder.encode(bootstrapPassword))
                .pinHash(passwordEncoder.encode(bootstrapPin))
                .loginAttempts(0)
                .build();

        userRepo.save(admin);

        log.info("═══════════════════════════════════════════════════════════");
        log.info("  BOOTSTRAP ADMIN CREATED");
        log.info("  Phone:    {}", phone);
        log.info("  Password: {} (change in production!)", bootstrapPassword);
        log.info("  PIN:      {}", bootstrapPin);
        log.info("  Role:     ADMIN");
        log.info("═══════════════════════════════════════════════════════════");
    }
}