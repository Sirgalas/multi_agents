package ru.sergalas.orchestrator.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.sergalas.orchestrator.dto.request.McpServerRequest;
import ru.sergalas.orchestrator.dto.response.McpResource;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.entity.enums.TransportType;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.service.McpService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: McpController REST Endpoints")
class McpControllerTest {

    @Mock
    private McpService mcpService;

    @Mock
    private ProjectMcpServerRepository mcpServerRepository;

    @InjectMocks
    private McpController mcpController;

    @Test
    @DisplayName("addServer returns 200 with persisted ProjectMcpServer")
    void testAddServer() {
        McpServerRequest request = McpServerRequest.builder()
                .name("Postgres Context")
                .serverUrl("http://context7/pg")
                .transportType(TransportType.HTTP)
                .build();

        ProjectMcpServer created = ProjectMcpServer.builder().id(12L).name("Postgres Context").build();
        when(mcpService.addMcpServer(1L, request)).thenReturn(created);

        ResponseEntity<ProjectMcpServer> response = mcpController.addServer(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(created);
    }

    @Test
    @DisplayName("toggleServer invokes toggle in service and returns 200 OK")
    void testToggleServer() {
        ResponseEntity<Void> response = mcpController.toggleServer(8L, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(mcpService).toggleMcpServer(8L, false);
    }

    @Test
    @DisplayName("getResources returns 200 and available resources list")
    void testGetResources() {
        ProjectMcpServer server = ProjectMcpServer.builder().id(3L).build();
        when(mcpServerRepository.findById(3L)).thenReturn(Optional.of(server));
        when(mcpService.listResources(server)).thenReturn(List.of(
                McpResource.builder().name("Guideline 1").build()
        ));

        ResponseEntity<List<McpResource>> response = mcpController.getResources(3L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
}