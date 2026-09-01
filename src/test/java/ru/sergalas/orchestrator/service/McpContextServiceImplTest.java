package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.sergalas.orchestrator.enums.StepName;
import ru.sergalas.orchestrator.model.ProjectMcpServer;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.service.impl.McpContextServiceImpl;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpContextServiceImpl Unit Tests")
class McpContextServiceImplTest {

    @Mock
    private ProjectMcpServerRepository mcpServerRepository;

    @Mock
    private WebClient webClient;

    @InjectMocks
    private McpContextServiceImpl mcpContextService;

    @Test
    @DisplayName("Should return empty string immediately if no active MCP servers are configured")
    void shouldReturnEmptyStringWhenNoActiveServers() {
        Long projectId = 1L;
        when(mcpServerRepository.findAllByProjectIdAndIsActiveTrue(projectId))
                .thenReturn(Collections.emptyList());

        String result = mcpContextService.fetchMcpContext(projectId, StepName.ARCHITECT);

        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Should aggregate context from active MCP servers successfully")
    void shouldFetchAndAggregateMcpContext() {
        Long projectId = 1L;
        ProjectMcpServer server = ProjectMcpServer.builder()
                .id(10L)
                .name("Postgres Docs")
                .serverUrl("https://context7.com/postgres")
                .isActive(true)
                .build();

        when(mcpServerRepository.findAllByProjectIdAndIsActiveTrue(projectId)).thenReturn(List.of(server));

        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("PostgreSQL best practices context"));

        String context = mcpContextService.fetchMcpContext(projectId, StepName.WORKER);

        assertThat(context)
                .contains("--- MCP SERVER [Postgres Docs] ---")
                .contains("PostgreSQL best practices context");
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Should handle WebClient error gracefully without breaking execution")
    void shouldHandleWebClientErrorGracefully() {
        Long projectId = 1L;
        ProjectMcpServer server = ProjectMcpServer.builder()
                .id(10L)
                .name("Failing Server")
                .serverUrl("https://unreachable.local")
                .isActive(true)
                .build();

        when(mcpServerRepository.findAllByProjectIdAndIsActiveTrue(projectId)).thenReturn(List.of(server));

        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("Connection Refused")));

        String context = mcpContextService.fetchMcpContext(projectId, StepName.HELPER);

        assertThat(context).contains("[MCP Server at https://unreachable.local did not respond]");
    }
}