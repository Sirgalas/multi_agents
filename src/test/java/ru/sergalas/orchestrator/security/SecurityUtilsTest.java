package ru.sergalas.orchestrator.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.sergalas.orchestrator.enums.UserRole;
import ru.sergalas.orchestrator.model.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit-тесты утилит контекста безопасности (SecurityUtils)")
class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Получение текущего userId при наличии валидного AppUserDetails в контексте")
    void getCurrentUserId_Authenticated_ReturnsUserId() {
        User user = User.builder()
                .id(42L)
                .username("john_doe")
                .password("hash")
                .role(UserRole.ROLE_USER)
                .build();
        AppUserDetails principal = new AppUserDetails(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Long userId = SecurityUtils.getCurrentUserId();

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    @DisplayName("Выброс IllegalStateException при запросе userId неаутентифицированного контекста")
    void getCurrentUserId_Unauthenticated_ThrowsIllegalStateException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(SecurityUtils::getCurrentUserId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Аутентифицированный пользователь не обнаружен в контексте безопасности");
    }

    @Test
    @DisplayName("Выброс IllegalStateException, если principal не является AppUserDetails")
    void getCurrentUserId_AnonymousUser_ThrowsIllegalStateException() {
        Authentication anonymousAuth = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        );
        SecurityContextHolder.getContext().setAuthentication(anonymousAuth);

        assertThatThrownBy(SecurityUtils::getCurrentUserId)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Получение имени пользователя")
    void getCurrentUsername_AuthenticatedAndAnonymous() {
        User user = User.builder().id(1L).username("alice").password("p").role(UserRole.ROLE_USER).build();
        AppUserDetails principal = new AppUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThat(SecurityUtils.getCurrentUsername()).isEqualTo("alice");

        SecurityContextHolder.clearContext();
        assertThat(SecurityUtils.getCurrentUsername()).isEqualTo("anonymous");
    }
}