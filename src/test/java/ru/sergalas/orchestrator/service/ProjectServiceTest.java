package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import ru.sergalas.orchestrator.dto.request.ProjectCreateRequest;
import ru.sergalas.orchestrator.dto.response.ProjectResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.repository.UserRepository;
import ru.sergalas.orchestrator.security.UserPrincipal;
import ru.sergalas.orchestrator.service.impl.ProjectServiceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService: Управление проектами и контроль прав доступа")
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private User ownerUser;
    private User otherUser;
    private User adminUser;
    private Authentication ownerAuth;
    private Authentication otherAuth;
    private Authentication adminAuth;
    private Project project;

    @BeforeEach
    void setUp() {
        ownerUser = User.builder().id(1L).username("owner").role(Role.ROLE_USER).build();
        otherUser = User.builder().id(2L).username("other").role(Role.ROLE_USER).build();
        adminUser = User.builder().id(3L).username("admin").role(Role.ROLE_ADMIN).build();

        ownerAuth = new UsernamePasswordAuthenticationToken(new UserPrincipal(ownerUser), null);
        otherAuth = new UsernamePasswordAuthenticationToken(new UserPrincipal(otherUser), null);
        adminAuth = new UsernamePasswordAuthenticationToken(new UserPrincipal(adminUser), null);

        project = Project.builder()
                .id(100L)
                .name("Test Project")
                .description("Test Description")
                .user(ownerUser)
                .createdAt(LocalDateTime.now())
                .contexts(new ArrayList<>())
                .mcpServers(new ArrayList<>())
                .steps(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("Получение списка проектов")
    class GetProjects {

        @Test
        @DisplayName("Обычный пользователь получает только свои проекты")
        void getProjectsForUser_RegularUser() {
            when(projectRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(project));

            List<ProjectResponse> result = projectService.getProjectsForUser(ownerAuth);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo("Test Project");
            verify(projectRepository).findAllByUserIdOrderByCreatedAtDesc(1L);
        }

        @Test
        @DisplayName("Администратор видит все проекты системы")
        void getProjectsForUser_Admin() {
            when(projectRepository.findAll()).thenReturn(List.of(project));

            List<ProjectResponse> result = projectService.getProjectsForUser(adminAuth);

            assertThat(result).hasSize(1);
            verify(projectRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Получение и верификация проекта")
    class GetProjectEntity {

        @Test
        @DisplayName("Владелец проекта может получить его сущность")
        void getProjectEntity_Owner_Success() {
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));

            Project result = projectService.getProjectEntity(100L, ownerAuth);

            assertThat(result).isEqualTo(project);
        }

        @Test
        @DisplayName("Администратор может получить проект другого пользователя")
        void getProjectEntity_Admin_Success() {
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));

            Project result = projectService.getProjectEntity(100L, adminAuth);

            assertThat(result).isEqualTo(project);
        }

        @Test
        @DisplayName("Попытка доступа чужого пользователя вызывает AccessDeniedException")
        void getProjectEntity_OtherUser_ThrowsAccessDenied() {
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.getProjectEntity(100L, otherAuth))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("permission");
        }

        @Test
        @DisplayName("Запрос несуществующего проекта вызывает IllegalArgumentException")
        void getProjectEntity_NotFound_ThrowsIllegalArgumentException() {
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.getProjectEntity(999L, ownerAuth))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Project not found with id: 999");
        }
    }

    @Nested
    @DisplayName("Создание и удаление проекта")
    class CreateAndDeleteProject {

        @Test
        @DisplayName("Успешное создание проекта")
        void createProject_Success() {
            ProjectCreateRequest request = new ProjectCreateRequest("New AI Project", "AI Desc");

            when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUser));
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
                Project p = inv.getArgument(0);
                p.setId(200L);
                p.setCreatedAt(LocalDateTime.now());
                return p;
            });

            ProjectResponse response = projectService.createProject(request, ownerAuth);

            assertThat(response.id()).isEqualTo(200L);
            assertThat(response.name()).isEqualTo("New AI Project");
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("Удаление проекта владельцем")
        void deleteProject_Success() {
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));

            projectService.deleteProject(100L, ownerAuth);

            verify(projectRepository).delete(project);
        }
    }
}