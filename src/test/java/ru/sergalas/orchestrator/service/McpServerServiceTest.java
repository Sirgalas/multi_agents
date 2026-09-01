package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import ru.sergalas.orchestrator.dto.request.McpServerRequest;
import ru.sergalas.orchestrator.dto.response.McpServerResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.entity.enums.TransportType;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.security.UserPrincipal;
import ru.sergalas.orchestrator.service.impl.McpServerServiceImpl;
import ru.sergalas.orchestrator.service.mcp.McpContextProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpServerService: Управление конфигурациями MCP-серверов")
class McpServerServiceTest {

    @Mock
    private ProjectMcpServerRepository mcpServerRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private McpContextProvider mcpContextProvider;

    @InjectMocks
    private McpServerServiceImpl mcpServerService;

    private User ownerUser;
    private User otherUser;
    private Authentication ownerAuth;
    private Authentication otherAuth;
    private Project project;
    private ProjectMcpServer server;

    @BeforeEach
    void setUp() {
        ownerUser = User.builder().id(1L).username("owner").role(Role.ROLE_USER).build();
        otherUser = User.builder().id(2L).username("other").role(Role.ROLE_USER).build();
        ownerAuth = new UsernamePasswordAuthenticationToken(new UserPrincipal(ownerUser), null);
        otherAuth = new UsernamePasswordAuthenticationToken(new UserPrincipal(otherUser), null);

        project = Project.builder().id(10L).user(ownerUser).build();
        server = ProjectMcpServer.builder()
                .id(100L)
                .project(project)
                .name("Context Server")
                .serverUrl("https://context.example.com")
                .transportType(TransportType.SSE)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Получение серверов проекта")
    void getByProject_Success() {
        when(projectService.getProjectEntity(10L, ownerAuth)).thenReturn(project);
        when(mcpServerRepository.findAllByProjectIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(server));

        List<McpServerResponse> result = mcpServerService.getByProject(10L, ownerAuth);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Context Server");
    }

    @Test
    @DisplayName("Создание MCP-сервера")
    void create_Success() {
        McpServerRequest request = new McpServerRequest("New Server", "https://new.server.com", TransportType.HTTP, true);

        when(projectService.getProjectEntity(10L, ownerAuth)).thenReturn(project);
        when(mcpServerRepository.save(any(ProjectMcpServer.class))).thenAnswer(inv -> {
            ProjectMcpServer s = inv.getArgument(0);
            s.setId(200L);
            return s;
        });

        McpServerResponse response = mcpServerService.create(10L, request, ownerAuth);

        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.name()).isEqualTo("New Server");
        verify(mcpServerRepository).save(any(ProjectMcpServer.class));
    }

    @Test
    @DisplayName("Переключение активности (toggleActive)")
    void toggleActive_Owner_TogglesState() {
        when(mcpServerRepository.findById(100L)).thenReturn(Optional.of(server));

        mcpServerService.toggleActive(100L, ownerAuth);

        assertThat(server.isActive()).isFalse();
        verify(mcpServerRepository).save(server);
    }

    @Test
    @DisplayName("Попытка изменения MCP-сервера чужим пользователем вызывает AccessDeniedException")
    void update_NonOwner_ThrowsAccessDenied() {
        McpServerRequest request = new McpServerRequest("Update", "https://url.com", TransportType.SSE, true);
        when(mcpServerRepository.findById(100L)).thenReturn(Optional.of(server));

        assertThatThrownBy(() -> mcpServerService.update(100L, request, otherAuth))
                .isInstanceOf(AccessDeniedException.class);
    }
}