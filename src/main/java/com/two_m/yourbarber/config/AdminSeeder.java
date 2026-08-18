package com.two_m.yourbarber.config;

import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default-email:admin@yourbarber.com}")
    private String adminEmail;

    @Value("${admin.default-password:ChangeMe123!}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin =
                User.builder()
                        .name("Administrator")
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .role(UserRole.ADMIN)
                        .build();
        userRepository.save(admin);
        log.info("Seeded default admin user: {}", adminEmail);
    }
}
