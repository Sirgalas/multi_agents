package ru.sergalas.orchestrator.service.mcp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.entity.enums.TransportType;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpContextProvider: Опрос MCP-серверов и агрегация правил")
class McpContextProviderTest {

    @Mock
    private ProjectMcpServerRepository mcpServerRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    @InjectMocks
    private McpContextProviderImpl mcpContextProvider;

    @Nested
    @DisplayName("Сбор контекста (collectContext)")
    class CollectContextTests {

        @Test
        @DisplayName("Если активных серверов нет, возвращается пустая строка")
        void collectContext_NoActiveServers_ReturnsEmpty() {
            when(mcpServerRepository.findAllByProjectIdAndActiveTrueOrderByCreatedAtAsc(1L))
                    .thenReturn(List.of());

            String result = mcpContextProvider.collectContext(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Успешный сбор документации с HTTP/SSE MCP сервера")
        void collectContext_HttpServer_Success() {
            ProjectMcpServer server = ProjectMcpServer.builder()
                    .name("Spring Boot MCP")
                    .serverUrl("https://context.local/mcp")
                    .transportType(TransportType.SSE)
                    .active(true)
                    .build();

            when(mcpServerRepository.findAllByProjectIdAndActiveTrueOrderByCreatedAtAsc(1L))
                    .thenReturn(List.of(server));

            when(restClient.get()
                    .uri(URI.create("https://context.local/mcp"))
                    .accept(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.ALL)
                    .retrieve()
                    .body(String.class)).thenReturn("Use Java 21 Virtual Threads");

            String result = mcpContextProvider.collectContext(1L);

            assertThat(result)
                    .contains("=== MCP CONTEXT & GUIDELINES ===")
                    .contains("--- MCP Server: Spring Boot MCP (https://context.local/mcp) ---")
                    .contains("Use Java 21 Virtual Threads")
                    .contains("=== END MCP CONTEXT ===");
        }

        @Test
        @DisplayName("STDIO транспорт возвращает отметку о локальном демоне без сетевого запроса")
        void collectContext_StdioTransport_ReturnsStdioNote() {
            ProjectMcpServer server = ProjectMcpServer.builder()
                    .name("Local Stdio MCP")
                    .serverUrl("stdio://local-daemon")
                    .transportType(TransportType.STDIO)
                    .active(true)
                    .build();

            when(mcpServerRepository.findAllByProjectIdAndActiveTrueOrderByCreatedAtAsc(1L))
                    .thenReturn(List.of(server));

            String result = mcpContextProvider.collectContext(1L);

            assertThat(result).contains("[STDIO Transport: configured for local daemon execution]");
        }

        @Test
        @DisplayName("Сбой подключения к MCP-серверу не роняет метод, а записывает ошибку в промпт")
        void collectContext_NetworkError_HandlesGracefully() {
            ProjectMcpServer server = ProjectMcpServer.builder()
                    .name("Failing MCP")
                    .serverUrl("https://unreachable.host/mcp")
                    .transportType(TransportType.HTTP)
                    .active(true)
                    .build();

            when(mcpServerRepository.findAllByProjectIdAndActiveTrueOrderByCreatedAtAsc(1L))
                    .thenReturn(List.of(server));

            when(restClient.get()
                    .uri(any(URI.class))
                    .accept(any(MediaType[].class))
                    .retrieve()
                    .body(String.class)).thenThrow(new RuntimeException("Connection timeout"));

            String result = mcpContextProvider.collectContext(1L);

            assertThat(result)
                    .contains("--- MCP Server: Failing MCP")
                    .contains("[MCP Server reachable, but returned empty documentation]");
        }
    }

    @Nested
    @DisplayName("Проверка доступности (isReachable)")
    class IsReachableTests {

        @Test
        @DisplayName("STDIO всегда доступен (true)")
        void isReachable_Stdio_ReturnsTrue() {
            boolean reachable = mcpContextProvider.isReachable("cmd://run", TransportType.STDIO);
            assertThat(reachable).isTrue();
        }

        @Test
        @DisplayName("HTTP 200 OK возвращает true")
        void isReachable_Http200_ReturnsTrue() {
            when(restClient.get()
                    .uri(URI.create("https://mcp.server/health"))
                    .accept(MediaType.ALL)
                    .retrieve()
                    .toBodilessEntity())
                    .thenReturn(new ResponseEntity<>(HttpStatus.OK));

            boolean reachable = mcpContextProvider.isReachable("https://mcp.server/health", TransportType.HTTP);

            assertThat(reachable).isTrue();
        }

        @Test
        @DisplayName("Сетевое исключение возвращает false")
        void isReachable_NetworkException_ReturnsFalse() {
            when(restClient.get()
                    .uri(any(URI.class))
                    .accept(any(MediaType.class))
                    .retrieve()
                    .toBodilessEntity())
                    .thenThrow(new RuntimeException("Host unreachable"));

            boolean reachable = mcpContextProvider.isReachable("https://mcp.server/health", TransportType.HTTP);

            assertThat(reachable).isFalse();
        }
    }
}