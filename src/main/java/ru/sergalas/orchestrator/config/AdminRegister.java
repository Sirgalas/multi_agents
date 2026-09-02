package ru.sergalas.orchestrator.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.repository.UserRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminRegister implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.user.name:admin}")
    private String adminUsername;

    @Value("${app.admin.user.password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ROLE_ADMIN)
                    .createdAt(LocalDateTime.now())
                    .build();

            userRepository.save(admin);

            log.error("=".repeat(80));
            log.error("INITIAL ADMIN CREATED");
            log.error("Username: {}", adminUsername);
            log.error("Password: {}", adminPassword);
            log.error("Please change the password immediately after first login!");
            log.error("=".repeat(80));
        }
    }
}