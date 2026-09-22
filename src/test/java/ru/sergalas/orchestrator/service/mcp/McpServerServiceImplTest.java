package ru.sergalas.orchestrator.service.mcp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.dto.request.CreateMcpServerRequest;
import ru.sergalas.orchestrator.entity.McpServer;
import ru.sergalas.orchestrator.entity.enums.McpTarget;
import ru.sergalas.orchestrator.repository.McpServerRepository;
import ru.sergalas.orchestrator.service.mcp.impl.McpServerServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpServerServiceImplTest {

    @Mock
    private McpServerRepository mcpServerRepository;

    @InjectMocks
    private McpServerServiceImpl mcpServerService;

    @Test
    @DisplayName("getAllServers возвращает список серверов из репозитория")
    void getAllServers_ReturnsList() {
        McpServer server = McpServer.builder().id(1L).name("Spring Boot").target(McpTarget.BACKEND).build();
        when(mcpServerRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(server));

        List<McpServer> result = mcpServerService.getAllServers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Spring Boot");
    }

    @Test
    @DisplayName("getServersByTarget фильтрует серверы по целевому типу")
    void getServersByTarget_ReturnsFilteredList() {
        McpServer server = McpServer.builder().id(2L).name("React").target(McpTarget.FRONTEND).build();
        when(mcpServerRepository.findAllByTarget(McpTarget.FRONTEND)).thenReturn(List.of(server));

        List<McpServer> result = mcpServerService.getServersByTarget(McpTarget.FRONTEND);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTarget()).isEqualTo(McpTarget.FRONTEND);
    }

    @Test
    @DisplayName("createServer сохраняет новый MCP сервер с валидными данными")
    void createServer_ValidData_SavesAndReturns() {
        CreateMcpServerRequest request = CreateMcpServerRequest.builder()
                .name("Vue.js Rules")
                .url("https://context7.com/vuejs")
                .target(McpTarget.FRONTEND)
                .token("vue-token-123")
                .description("Vue 3 guidelines")
                .build();

        when(mcpServerRepository.findByNameIgnoreCase("Vue.js Rules")).thenReturn(Optional.empty());
        when(mcpServerRepository.save(any(McpServer.class))).thenAnswer(i -> {
            McpServer s = i.getArgument(0);
            s.setId(10L);
            return s;
        });

        McpServer created = mcpServerService.createServer(request);

        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getName()).isEqualTo("Vue.js Rules");
        assertThat(created.getUrl()).isEqualTo("https://context7.com/vuejs");
        assertThat(created.getTarget()).isEqualTo(McpTarget.FRONTEND);
        assertThat(created.getToken()).isEqualTo("vue-token-123");
        assertThat(created.getDescription()).isEqualTo("Vue 3 guidelines");
    }

    @Test
    @DisplayName("createServer выбрасывает исключение при дублировании имени")
    void createServer_DuplicateName_ThrowsIllegalArgumentException() {
        CreateMcpServerRequest request = CreateMcpServerRequest.builder()
                .name("Spring Boot")
                .url("https://context7.com/spring-boot")
                .target(McpTarget.BACKEND)
                .build();

        when(mcpServerRepository.findByNameIgnoreCase("Spring Boot"))
                .thenReturn(Optional.of(McpServer.builder().id(1L).name("Spring Boot").build()));

        assertThatThrownBy(() -> mcpServerService.createServer(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("уже существует");

        verify(mcpServerRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteServer удаляет сервер по ID")
    void deleteServer_DeletesById() {
        mcpServerService.deleteServer(5L);
        verify(mcpServerRepository).deleteById(5L);
    }

    @Test
    @DisplayName("getServerById возвращает найденный сервер")
    void getServerById_Found_ReturnsServer() {
        McpServer server = McpServer.builder().id(3L).name("Java Modern").build();
        when(mcpServerRepository.findById(3L)).thenReturn(Optional.of(server));

        McpServer result = mcpServerService.getServerById(3L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Java Modern");
    }

    @Test
    @DisplayName("getServerById выбрасывает исключение если сервер не найден")
    void getServerById_NotFound_ThrowsException() {
        when(mcpServerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcpServerService.getServerById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не найден");
    }
}
