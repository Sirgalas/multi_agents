package ru.sergalas.orchestrator.service.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.sergalas.orchestrator.dto.request.CreateUserRequest;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.repository.UserRepository;
import ru.sergalas.orchestrator.service.user.impl.UserServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Успешное создание нового пользователя")
    void createUser_Success() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("johndoe");
        request.setPassword("plainPassword123");
        request.setRole(Role.ROLE_USER);

        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword123")).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User created = userService.createUser(request);

        // Assert
        assertThat(created.getUsername()).isEqualTo("johndoe");
        assertThat(created.getPassword()).isEqualTo("hashedPass");
        assertThat(created.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(created.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("Edge Case: ошибка при создании пользователя с дублирующимся username")
    void createUser_DuplicateUsername_ThrowsIllegalArgumentException() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existingUser");
        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists: existingUser");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Edge Case: getCurrentUser возвращает текущего авторизованного пользователя из SecurityContext")
    void getCurrentUser_Success() {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("auth_user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
        User mockUser = User.builder().id(1L).username("auth_user").build();
        when(userRepository.findByUsername("auth_user")).thenReturn(Optional.of(mockUser));

        // Act
        User current = userService.getCurrentUser();

        // Assert
        assertThat(current).isNotNull();
        assertThat(current.getUsername()).isEqualTo("auth_user");
    }

    @Test
    @DisplayName("Edge Case: getCurrentUser выбрасывает исключение, если контекст авторизации пуст")
    void getCurrentUser_NoAuth_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    @DisplayName("Удаление пользователя по ID")
    void deleteUser_Success() {
        // Act
        userService.deleteUser(99L);

        // Assert
        verify(userRepository).deleteById(99L);
    }
}