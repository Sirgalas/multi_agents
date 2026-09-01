package ru.sergalas.orchestrator.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.sergalas.orchestrator.enums.UserRole;
import ru.sergalas.orchestrator.model.User;
import ru.sergalas.orchestrator.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-тесты UserDetailsService (AppUserDetailsService)")
class AppUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppUserDetailsService userDetailsService;

    @Test
    @DisplayName("Успешная загрузка пользователя с ролью ROLE_ADMIN")
    void loadUserByUsername_UserFound_ReturnsAppUserDetails() {
        String username = "admin";
        User user = User.builder()
                .id(1L)
                .username(username)
                .password("$2a$12$securehash")
                .role(UserRole.ROLE_ADMIN)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertThat(userDetails).isNotNull().isInstanceOf(AppUserDetails.class);
        AppUserDetails appUserDetails = (AppUserDetails) userDetails;
        assertThat(appUserDetails.getId()).isEqualTo(1L);
        assertThat(appUserDetails.getUsername()).isEqualTo(username);
        assertThat(appUserDetails.getPassword()).isEqualTo("$2a$12$securehash");
        assertThat(appUserDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        assertThat(appUserDetails.isAccountNonExpired()).isTrue();
        assertThat(appUserDetails.isAccountNonLocked()).isTrue();
        assertThat(appUserDetails.isCredentialsNonExpired()).isTrue();
        assertThat(appUserDetails.isEnabled()).isTrue();

        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("Выброс UsernameNotFoundException при отсутствии пользователя")
    void loadUserByUsername_UserNotFound_ThrowsException() {
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Пользователь с именем '" + username + "' не найден");

        verify(userRepository).findByUsername(username);
    }
}