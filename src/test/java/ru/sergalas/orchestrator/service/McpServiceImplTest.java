package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.sergalas.orchestrator.dto.request.McpServerRequest;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.entity.enums.TransportType;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: McpServiceImpl Integration and Fallback Logic")
class McpServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ProjectMcpServerRepository mcpServerRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private McpServiceImpl mcpService;

    @Test
    @DisplayName("fetchMcpContext returns empty string if server is inactive")
    void testFetchWhenInactiveReturnsEmpty() {
        ProjectMcpServer server = ProjectMcpServer.builder()
                .isActive(false)
                .serverUrl("http://example.com")
                .transportType(TransportType.HTTP)
                .build();

        String result = mcpService.fetchMcpContext(server, "rules");

        assertThat(result).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("fetchMcpContext fetches over HTTP and returns body")
    void testFetchHttpSuccess() {
        ProjectMcpServer server = ProjectMcpServer.builder()
                .name("Spring Guide")
                .isActive(true)
                .serverUrl("https://context7.com/spring")
                .transportType(TransportType.HTTP)
                .build();

        when(restTemplate.getForEntity(eq("https://context7.com/spring/resources/architecture"), eq(String.class)))
                .thenReturn(ResponseEntity.ok("Clean Architecture Rules"));

        String result = mcpService.fetchMcpContext(server, "architecture");

        assertThat(result).isEqualTo("Clean Architecture Rules");
    }

    @Test
    @DisplayName("fetchMcpContext catches RestClientException and returns fallback documentation")
    void testFetchHttpFailureReturnsFallback() {
        ProjectMcpServer server = ProjectMcpServer.builder()
                .name("Postgres Guide")
                .isActive(true)
                .serverUrl("https://context7.com/postgres")
                .transportType(TransportType.HTTP)
                .build();

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        String result = mcpService.fetchMcpContext(server, "indexing");

        assertThat(result).contains("Standard Best Practices for Postgres Guide");
    }

    @Test
    @DisplayName("addMcpServer links server to project and saves to database")
    void testAddMcpServer() {
        Project project = Project.builder().id(10L).name("Project 10").build();
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        McpServerRequest request = McpServerRequest.builder()
                .name("Hibernate MCP")
                .serverUrl("https://context7.com/hibernate")
                .transportType(TransportType.HTTP)
                .isActive(true)
                .build();

        when(mcpServerRepository.save(any(ProjectMcpServer.class))).thenAnswer(i -> {
            ProjectMcpServer s = i.getArgument(0);
            s.setId(100L);
            return s;
        });

        ProjectMcpServer saved = mcpService.addMcpServer(10L, request);

        assertThat(saved.getId()).isEqualTo(100L);
        assertThat(saved.getName()).isEqualTo("Hibernate MCP");
        assertThat(saved.getProject()).isEqualTo(project);
    }

    @Test
    @DisplayName("toggleMcpServer updates active status")
    void testToggleMcpServer() {
        ProjectMcpServer server = ProjectMcpServer.builder().id(5L).isActive(false).build();
        when(mcpServerRepository.findById(5L)).thenReturn(Optional.of(server));

        mcpService.toggleMcpServer(5L, true);

        assertThat(server.getIsActive()).isTrue();
        verify(mcpServerRepository).save(server);
    }
}