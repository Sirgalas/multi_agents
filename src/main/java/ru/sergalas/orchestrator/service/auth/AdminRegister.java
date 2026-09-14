package ru.sergalas.orchestrator.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.config.properties.AdminProperties;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminRegister implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;
    
    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            String username = adminProperties.getName();
            String rawPassword = adminProperties.getPassword();
            
            User admin = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .build();
            
            userRepository.save(admin);
            
            log.error("==============================================");
            log.error("ПЕРВЫЙ АДМИНИСТРАТОР СОЗДАН");
            log.error("Логин: {}", username);
            log.error("Пароль: {}", rawPassword);
            log.error("==============================================");
        }
    }
}