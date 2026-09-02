package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.sergalas.orchestrator.dto.request.UserCreateRequest;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: UserServiceImpl")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("findAll returns list of users")
    void testFindAll() {
        List<User> users = List.of(
                User.builder().id(1L).username("u1").role(Role.ROLE_USER).build(),
                User.builder().id(2L).username("u2").role(Role.ROLE_ADMIN).build()
        );
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.findAll();

        assertThat(result).hasSize(2).isEqualTo(users);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("findById returns user if exists, otherwise throws IllegalArgumentException")
    void testFindById() {
        User user = User.builder().id(10L).username("dev").build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.findById(10L)).isEqualTo(user);
        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    @Test
    @DisplayName("createUser succeeds with valid payload and encodes password")
    void testCreateUserSuccess() {
        UserCreateRequest request = UserCreateRequest.builder()
                .username("newdev")
                .password("plain123")
                .role(Role.ROLE_USER)
                .build();

        when(userRepository.existsByUsername("newdev")).thenReturn(false);
        when(passwordEncoder.encode("plain123")).thenReturn("encoded_plain123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(100L);
            return u;
        });

        User created = userService.createUser(request);

        assertThat(created.getId()).isEqualTo(100L);
        assertThat(created.getUsername()).isEqualTo("newdev");
        assertThat(created.getPassword()).isEqualTo("encoded_plain123");
        assertThat(created.getRole()).isEqualTo(Role.ROLE_USER);

        verify(userRepository).existsByUsername("newdev");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser throws IllegalArgumentException on duplicate username")
    void testCreateUserDuplicateThrows() {
        UserCreateRequest request = UserCreateRequest.builder()
                .username("existingUser")
                .password("pass")
                .role(Role.ROLE_USER)
                .build();

        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists: existingUser");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteUser delegates to repository deleteById")
    void testDeleteUser() {
        userService.deleteUser(5L);
        verify(userRepository).deleteById(5L);
    }
}