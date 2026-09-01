package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.dto.request.AddMcpServerRequest;
import ru.sergalas.orchestrator.enums.TransportType;
import ru.sergalas.orchestrator.exception.ProjectNotFoundException;
import ru.sergalas.orchestrator.exception.ResourceNotFoundException;
import ru.sergalas.orchestrator.model.Project;
import ru.sergalas.orchestrator.model.ProjectMcpServer;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.impl.ProjectMcpServerServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectMcpServerServiceImpl Unit Tests")
class ProjectMcpServerServiceImplTest {

    @Mock
    private ProjectMcpServerRepository mcpServerRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectMcpServerServiceImpl projectMcpServerService;

    @Test
    @DisplayName("Should successfully add MCP server to project")
    void shouldAddMcpServerSuccessfully() {
        Long projectId = 1L;
        Long userId = 2L;
        Project project = Project.builder().id(projectId).build();
        AddMcpServerRequest req = new AddMcpServerRequest(
                "Spring Context", "https://context7.com/spring", TransportType.SSE, true
        );

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(mcpServerRepository.save(any(ProjectMcpServer.class))).thenAnswer(invocation -> {
            ProjectMcpServer s = invocation.getArgument(0);
            s.setId(20L);
            return s;
        });

        ProjectMcpServer saved = projectMcpServerService.addServer(projectId, userId, req);

        assertThat(saved.getId()).isEqualTo(20L);
        assertThat(saved.getName()).isEqualTo("Spring Context");
        assertThat(saved.getServerUrl()).isEqualTo("https://context7.com/spring");
        assertThat(saved.getTransportType()).isEqualTo(TransportType.SSE);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw ProjectNotFoundException when adding MCP server to unauthorized project")
    void shouldThrowExceptionWhenProjectUnauthorized() {
        Long projectId = 1L;
        Long userId = 2L;
        AddMcpServerRequest req = new AddMcpServerRequest("Name", "http://url", TransportType.HTTP, true);

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMcpServerService.addServer(projectId, userId, req))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    @DisplayName("Should toggle MCP server active status from active to inactive and back")
    void shouldToggleServerActiveStatus() {
        Long projectId = 1L;
        Long serverId = 10L;
        Long userId = 2L;
        Project project = Project.builder().id(projectId).build();
        ProjectMcpServer server = ProjectMcpServer.builder()
                .id(serverId)
                .project(project)
                .isActive(true)
                .build();

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(mcpServerRepository.findByIdAndProjectId(serverId, projectId)).thenReturn(Optional.of(server));
        when(mcpServerRepository.save(any(ProjectMcpServer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectMcpServer updated = projectMcpServerService.toggleActive(serverId, projectId, userId);

        assertThat(updated.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when toggling non-existent server")
    void shouldThrowExceptionWhenTogglingNonExistentServer() {
        Long projectId = 1L;
        Long serverId = 999L;
        Long userId = 2L;
        Project project = Project.builder().id(projectId).build();

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(mcpServerRepository.findByIdAndProjectId(serverId, projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMcpServerService.toggleActive(serverId, projectId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MCP Server not found: 999");
    }

    @Test
    @DisplayName("Should retrieve only active servers for given project")
    void shouldGetActiveServers() {
        Long projectId = 1L;
        List<ProjectMcpServer> activeList = List.of(
                ProjectMcpServer.builder().id(1L).isActive(true).build()
        );

        when(mcpServerRepository.findAllByProjectIdAndIsActiveTrue(projectId)).thenReturn(activeList);

        List<ProjectMcpServer> result = projectMcpServerService.getActiveServers(projectId);

        assertThat(result).hasSize(1).isEqualTo(activeList);
    }

    @Test
    @DisplayName("Should delete MCP server when user is project owner")
    void shouldDeleteServerSuccessfully() {
        Long projectId = 1L;
        Long serverId = 10L;
        Long userId = 2L;
        Project project = Project.builder().id(projectId).build();
        ProjectMcpServer server = ProjectMcpServer.builder().id(serverId).project(project).build();

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(mcpServerRepository.findByIdAndProjectId(serverId, projectId)).thenReturn(Optional.of(server));

        projectMcpServerService.deleteServer(serverId, projectId, userId);

        verify(mcpServerRepository).delete(server);
    }
}