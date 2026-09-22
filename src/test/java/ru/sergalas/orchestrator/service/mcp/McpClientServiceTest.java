package ru.sergalas.orchestrator.service.mcp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import ru.sergalas.orchestrator.entity.McpServer;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.entity.enums.McpTarget;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpClientServiceTest {

    @Mock
    private ProjectMcpServerRepository mcpServerRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    @InjectMocks
    private McpClientService mcpClientService;

    private ProjectMcpServer createServer(String name, String url, McpTarget target, String description) {
        return ProjectMcpServer.builder()
                .mcpServer(McpServer.builder()
                        .name(name)
                        .url(url != null ? url : "https://mcp.org/" + name)
                        .target(target != null ? target : McpTarget.COMMON)
                        .description(description)
                        .build())
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Edge Case: при отсутствии активных MCP серверов возвращается пустая строка")
    void aggregateMcpContext_WhenNoServers_ReturnsEmptyString() {
        // Arrange
        Project project = Project.builder().id(1L).build();
        when(mcpServerRepository.findAllByProjectAndIsActiveTrue(project)).thenReturn(Collections.emptyList());

        // Act
        String context = mcpClientService.aggregateMcpContext(project);

        // Assert
        assertThat(context).isEmpty();
    }

    @Test
    @DisplayName("Успешная агрегация MCP контекста со серверов документации")
    void aggregateMcpContext_WithActiveServers_ReturnsAggregatedRules() {
        // Arrange
        Project project = Project.builder().id(1L).build();
        ProjectMcpServer server = createServer("Spring Boot Guidelines", "https://context7.com/spring-boot", McpTarget.BACKEND, null);

        when(mcpServerRepository.findAllByProjectAndIsActiveTrue(project)).thenReturn(List.of(server));
        when(restClient.get().uri("https://context7.com/spring-boot").retrieve().body(String.class))
                .thenReturn("Use constructor injection and records for DTOs.");

        // Act
        String result = mcpClientService.aggregateMcpContext(project);

        // Assert
        assertThat(result)
                .contains("MCP ARCHITECTURAL RULES & CONVENTIONS")
                .contains("Source: Spring Boot Guidelines")
                .contains("Use constructor injection and records for DTOs.");
    }

    @Test
    @DisplayName("Edge Case: при сбое HTTP вызова MCP сервера используется безопасный fallback")
    void aggregateMcpContext_WhenServerFails_UsesFallbackGuidelines() {
        // Arrange
        Project project = Project.builder().id(1L).build();
        ProjectMcpServer failingServer = createServer("Failing Guidelines", "https://offline-mcp.org", McpTarget.COMMON, null);

        when(mcpServerRepository.findAllByProjectAndIsActiveTrue(project)).thenReturn(List.of(failingServer));
        when(restClient.get().uri("https://offline-mcp.org").retrieve().body(String.class))
                .thenThrow(new RuntimeException("Connection timed out"));

        // Act
        String result = mcpClientService.aggregateMcpContext(project);

        // Assert
        assertThat(result)
                .contains("Fallback Guidelines")
                .contains("Follow Clean Architecture, idiomatic patterns");
    }

    @Test
    @DisplayName("aggregateBackendMcpContext включает только BACKEND и COMMON серверы")
    void aggregateBackendMcpContext_FiltersCorrectTargets() {
        Project project = Project.builder().id(1L).build();
        ProjectMcpServer backendSrv = createServer("Spring Boot Guidelines", "https://context7.com/spring-boot", McpTarget.BACKEND, null);
        ProjectMcpServer frontendSrv = createServer("React Guidelines & Hooks", "https://context7.com/react", McpTarget.FRONTEND, null);

        when(mcpServerRepository.findAllByProjectAndIsActiveTrue(project)).thenReturn(List.of(backendSrv, frontendSrv));
        when(restClient.get().uri("https://context7.com/spring-boot").retrieve().body(String.class))
                .thenReturn("Spring Boot rule content");

        String result = mcpClientService.aggregateBackendMcpContext(project);

        assertThat(result)
                .contains("Spring Boot Guidelines")
                .doesNotContain("React Guidelines & Hooks");
    }

    @Test
    @DisplayName("aggregateFrontendMcpContext включает только FRONTEND и COMMON серверы")
    void aggregateFrontendMcpContext_FiltersCorrectTargets() {
        Project project = Project.builder().id(1L).build();
        ProjectMcpServer backendSrv = createServer("Spring Boot Guidelines", "https://context7.com/spring-boot", McpTarget.BACKEND, null);
        ProjectMcpServer frontendSrv = createServer("React Guidelines & Hooks", "https://context7.com/react", McpTarget.FRONTEND, null);

        when(mcpServerRepository.findAllByProjectAndIsActiveTrue(project)).thenReturn(List.of(backendSrv, frontendSrv));
        when(restClient.get().uri("https://context7.com/react").retrieve().body(String.class))
                .thenReturn("React hooks rule content");

        String result = mcpClientService.aggregateFrontendMcpContext(project);

        assertThat(result)
                .contains("React Guidelines & Hooks")
                .doesNotContain("Spring Boot Guidelines");
    }

    @Test
    @DisplayName("Edge Case: при сбое HTTP вызова MCP сервера используется description из ProjectMcpServer если он задан")
    void aggregateMcpContext_WhenServerFailsWithCustomDescription_UsesServerDescription() {
        // Arrange
        Project project = Project.builder().id(1L).build();
        ProjectMcpServer failingServer = createServer("Custom Tech Rules", "https://offline-mcp.org", McpTarget.COMMON, "Custom strict rules for high-load systems.");

        when(mcpServerRepository.findAllByProjectAndIsActiveTrue(project)).thenReturn(List.of(failingServer));
        when(restClient.get().uri("https://offline-mcp.org").retrieve().body(String.class))
                .thenThrow(new RuntimeException("Connection timed out"));

        // Act
        String result = mcpClientService.aggregateMcpContext(project);

        // Assert
        assertThat(result)
                .contains("Custom Tech Rules")
                .contains("Custom strict rules for high-load systems.");
    }

    @Test
    @DisplayName("buildRulesMap формирует правила напрямую из описаний серверов проекта")
    void buildRulesMap_PopulatesRulesFromServerDescriptions() {
        ProjectMcpServer customServer = createServer("Spring Boot Guidelines", null, McpTarget.COMMON, "Overridden Spring Boot rules for company X.");
        ProjectMcpServer newServer = createServer("Redis Caching Rules", null, McpTarget.COMMON, "Use Redis Cluster with Lettuce connection pool.");
        ProjectMcpServer serverWithoutDesc = createServer("Generic Server", null, McpTarget.COMMON, null);

        var rulesMap = mcpClientService.buildRulesMap(List.of(customServer, newServer, serverWithoutDesc));

        assertThat(rulesMap.get("Spring Boot Guidelines"))
                .isEqualTo("Overridden Spring Boot rules for company X.");
        assertThat(rulesMap.get("Redis Caching Rules"))
                .isEqualTo("Use Redis Cluster with Lettuce connection pool.");
        assertThat(rulesMap.get("Generic Server"))
                .contains("Clean Architecture");
    }

    @Test
    @DisplayName("getRulesMap загружает активные серверы проекта и возвращает актуальную мапу правил")
    void getRulesMap_LoadsActiveServersFromRepository() {
        Project project = Project.builder().id(1L).build();
        ProjectMcpServer srv = createServer("GraphQL Rules", null, McpTarget.COMMON, "Use DataLoader to avoid N+1 queries.");

        when(mcpServerRepository.findAllByProjectAndIsActiveTrue(project)).thenReturn(List.of(srv));

        var rulesMap = mcpClientService.getRulesMap(project);

        assertThat(rulesMap.get("GraphQL Rules"))
                .isEqualTo("Use DataLoader to avoid N+1 queries.");
    }
}