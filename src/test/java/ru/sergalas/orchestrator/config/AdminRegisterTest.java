package ru.sergalas.orchestrator.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: AdminRegister CommandLineRunner")
class AdminRegisterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminRegister adminRegister;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminRegister, "adminUsername", "superadmin");
        ReflectionTestUtils.setField(adminRegister, "adminPassword", "secretPass123");
    }

    @Test
    @DisplayName("Should create default admin when userRepository is completely empty")
    void testCreateAdminWhenDbIsEmpty() {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("secretPass123")).thenReturn("encodedSecret");

        adminRegister.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedAdmin = userCaptor.getValue();
        assertThat(savedAdmin.getUsername()).isEqualTo("superadmin");
        assertThat(savedAdmin.getPassword()).isEqualTo("encodedSecret");
        assertThat(savedAdmin.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(savedAdmin.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should not create admin if database already contains users")
    void testDoNothingWhenDbHasUsers() {
        when(userRepository.count()).thenReturn(3L);

        adminRegister.run();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}