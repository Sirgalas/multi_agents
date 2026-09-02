package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.dto.request.McpServerRequest;
import ru.sergalas.orchestrator.dto.request.ProjectCreateRequest;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.entity.TaskTemplate;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.entity.enums.TransportType;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.repository.TaskTemplateRepository;
import ru.sergalas.orchestrator.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: ProjectServiceImpl")
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskTemplateRepository taskTemplateRepository;
    @Mock
    private ProjectContextRepository contextRepository;
    @Mock
    private ProjectMcpServerRepository mcpServerRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    @DisplayName("createProject initializes project, saves TASK context, and persists MCP servers")
    void testCreateProjectFull() {
        User user = User.builder().id(1L).username("coder").role(Role.ROLE_USER).build();
        TaskTemplate template = TaskTemplate.builder().id(5L).name("Template 5").build();

        when(userRepository.findByUsername("coder")).thenReturn(Optional.of(user));
        when(taskTemplateRepository.findById(5L)).thenReturn(Optional.of(template));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> {
            Project p = i.getArgument(0);
            p.setId(10L);
            return p;
        });

        ProjectCreateRequest request = ProjectCreateRequest.builder()
                .name("New Microservice")
                .description("Service description")
                .taskTemplateId(5L)
                .taskContent("# Technical Task Content")
                .mcpServers(List.of(
                        new McpServerRequest("Guidelines", "http://guidelines", TransportType.HTTP, true)
                ))
                .build();

        Project created = projectService.createProject(request, "coder");

        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getName()).isEqualTo("New Microservice");
        assertThat(created.getUser()).isEqualTo(user);
        assertThat(created.getTaskTemplate()).isEqualTo(template);

        verify(contextRepository).save(any(ProjectContext.class));
        verify(mcpServerRepository).save(any(ProjectMcpServer.class));
    }

    @Test
    @DisplayName("createProject throws IllegalArgumentException when username is not found")
    void testCreateProjectUserNotFoundThrows() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        ProjectCreateRequest request = ProjectCreateRequest.builder()
                .name("Test")
                .taskContent("Content")
                .build();

        assertThatThrownBy(() -> projectService.createProject(request, "unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found: unknown");

        verifyNoInteractions(projectRepository);
    }
}