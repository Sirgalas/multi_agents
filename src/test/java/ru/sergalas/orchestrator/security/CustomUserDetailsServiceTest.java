package ru.sergalas.orchestrator.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService: Загрузка UserDetails для аутентификации")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Успешная загрузка существующего пользователя")
    void loadUserByUsername_Success() {
        User user = User.builder()
                .id(1L)
                .username("dev_user")
                .password("encoded_pass")
                .role(Role.ROLE_USER)
                .build();

        when(userRepository.findByUsername("dev_user")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("dev_user");

        assertThat(details).isNotNull();
        assertThat(details.getUsername()).isEqualTo("dev_user");
        assertThat(details.getPassword()).isEqualTo("encoded_pass");
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
        verify(userRepository).findByUsername("dev_user");
    }

    @Test
    @DisplayName("Выброс исключения UsernameNotFoundException, если пользователь не найден")
    void loadUserByUsername_NotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}