package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.sergalas.orchestrator.enums.UserRole;
import ru.sergalas.orchestrator.model.User;
import ru.sergalas.orchestrator.repository.UserRepository;
import ru.sergalas.orchestrator.service.impl.UserServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Should successfully register a new user with encoded password")
    void shouldRegisterNewUser() {
        String username = "johndoe";
        String rawPassword = "password123";
        String encodedHash = "$2a$10$encodedHashString";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedHash);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(100L);
            return u;
        });

        User savedUser = userService.registerUser(username, rawPassword, UserRole.ROLE_ADMIN);

        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isEqualTo(100L);
        assertThat(savedUser.getUsername()).isEqualTo(username);
        assertThat(savedUser.getPasswordHash()).isEqualTo(encodedHash);
        assertThat(savedUser.getRole()).isEqualTo(UserRole.ROLE_ADMIN);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should default to ROLE_USER when role is null during registration")
    void shouldDefaultToRoleUserWhenNull() {
        String username = "janedoe";
        String rawPassword = "secretPassword";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.registerUser(username, rawPassword, null);

        assertThat(savedUser.getRole()).isEqualTo(UserRole.ROLE_USER);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when username is already taken")
    void shouldThrowExceptionWhenUsernameExists() {
        String username = "existingUser";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(username, "pass", UserRole.ROLE_USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken: " + username);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return Optional of User when username exists")
    void shouldFindUserByUsername() {
        User user = User.builder().id(1L).username("testuser").build();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        Optional<User> found = userService.findByUsername("testuser");

        assertThat(found).isPresent().contains(user);
    }

    @Test
    @DisplayName("Should return empty Optional when username does not exist")
    void shouldReturnEmptyWhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        Optional<User> found = userService.findByUsername("unknown");

        assertThat(found).isEmpty();
    }
}