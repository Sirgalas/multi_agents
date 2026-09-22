package ru.sergalas.orchestrator.service.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.sergalas.orchestrator.config.properties.AdminProperties;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminRegisterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminProperties adminProperties;

    @InjectMocks
    private AdminRegister adminRegister;

    @Test
    @DisplayName("Edge Case: при пустой БД создается администратор с захешированным паролем и ROLE_ADMIN")
    void run_WhenNoUsersExist_ShouldCreateAdmin() {
        // Arrange
        when(userRepository.count()).thenReturn(0L);
        when(adminProperties.getName()).thenReturn("superadmin");
        when(adminProperties.getPassword()).thenReturn("adminSecretPass");
        when(passwordEncoder.encode("adminSecretPass")).thenReturn("$2a$10$encodedHashPass");

        // Act
        adminRegister.run();

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("superadmin");
        assertThat(savedUser.getPassword()).isEqualTo("$2a$10$encodedHashPass");
        assertThat(savedUser.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(savedUser.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("Edge Case: если в БД уже есть пользователи, новый администратор не создается")
    void run_WhenUsersAlreadyExist_ShouldDoNothing() {
        // Arrange
        when(userRepository.count()).thenReturn(2L);

        // Act
        adminRegister.run();

        // Assert
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }
}