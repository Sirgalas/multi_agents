package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.dto.request.CreateProjectRequest;
import ru.sergalas.orchestrator.exception.ProjectNotFoundException;
import ru.sergalas.orchestrator.exception.ResourceNotFoundException;
import ru.sergalas.orchestrator.model.Project;
import ru.sergalas.orchestrator.model.User;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.repository.UserRepository;
import ru.sergalas.orchestrator.service.impl.ProjectServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectServiceImpl Unit Tests")
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    @DisplayName("Should successfully create project for existing user")
    void shouldCreateProjectSuccessfully() {
        Long userId = 1L;
        User user = User.builder().id(userId).username("architect_user").build();
        CreateProjectRequest request = new CreateProjectRequest("Orchestrator Pro", "AI Pipeline project");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            p.setId(10L);
            return p;
        });

        Project created = projectService.createProject(userId, request);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getName()).isEqualTo("Orchestrator Pro");
        assertThat(created.getDescription()).isEqualTo("AI Pipeline project");
        assertThat(created.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating project for non-existent user")
    void shouldThrowExceptionWhenUserNotFoundOnCreate() {
        Long userId = 999L;
        CreateProjectRequest request = new CreateProjectRequest("Project X", "Desc");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.createProject(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
    }

    @Test
    @DisplayName("Should retrieve projects ordered by creation date for given user")
    void shouldGetProjectsByUser() {
        Long userId = 1L;
        List<Project> projects = List.of(
                Project.builder().id(2L).name("P2").build(),
                Project.builder().id(1L).name("P1").build()
        );
        when(projectRepository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(projects);

        List<Project> result = projectService.getProjectsByUser(userId);

        assertThat(result).hasSize(2).isEqualTo(projects);
    }

    @Test
    @DisplayName("Should return project when ID and User ID match")
    void shouldGetProjectByIdAndUserId() {
        Long projectId = 5L;
        Long userId = 1L;
        Project project = Project.builder().id(projectId).name("Secure Project").build();

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));

        Project result = projectService.getProjectById(projectId, userId);

        assertThat(result).isEqualTo(project);
    }

    @Test
    @DisplayName("Should throw ProjectNotFoundException when project does not exist or belongs to another user")
    void shouldThrowExceptionWhenProjectNotOwnedByUser() {
        Long projectId = 5L;
        Long userId = 2L;

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(projectId, userId))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("Project with id 5 not found or access denied");
    }

    @Test
    @DisplayName("Should delete project when user is the owner")
    void shouldDeleteProjectSuccessfully() {
        Long projectId = 5L;
        Long userId = 1L;
        Project project = Project.builder().id(projectId).name("ToDelete").build();

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));

        projectService.deleteProject(projectId, userId);

        verify(projectRepository).delete(project);
    }
}