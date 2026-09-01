package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.sergalas.orchestrator.dto.request.UserCreateRequest;
import ru.sergalas.orchestrator.dto.response.UserResponse;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.repository.UserRepository;
import ru.sergalas.orchestrator.service.impl.UserServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService: Управление пользователями системы")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Успешное создание нового пользователя")
    void createUser_Success() {
        UserCreateRequest request = new UserCreateRequest("new_dev", "secret123", Role.ROLE_USER);

        when(userRepository.existsByUsername("new_dev")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed_secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        UserResponse response = userService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.username()).isEqualTo("new_dev");
        assertThat(response.role()).isEqualTo(Role.ROLE_USER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Ошибка создания пользователя с дублирующимся логином")
    void createUser_DuplicateUsername_ThrowsIllegalArgumentException() {
        UserCreateRequest request = new UserCreateRequest("existing_dev", "secret123", Role.ROLE_USER);

        when(userRepository.existsByUsername("existing_dev")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken: existing_dev");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Получение списка всех пользователей")
    void getAllUsers_ReturnsMappedList() {
        User user1 = User.builder().id(1L).username("u1").role(Role.ROLE_USER).build();
        User user2 = User.builder().id(2L).username("u2").role(Role.ROLE_ADMIN).build();

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).username()).isEqualTo("u1");
        assertThat(result.get(1).username()).isEqualTo("u2");
    }

    @Test
    @DisplayName("Получение пользователя по ID: Успех")
    void getById_Success() {
        User user = User.builder().id(5L).username("u5").role(Role.ROLE_USER).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        UserResponse result = userService.getById(5L);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.username()).isEqualTo("u5");
    }

    @Test
    @DisplayName("Получение пользователя по ID: Не найден")
    void getById_NotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found with id: 99");
    }
}