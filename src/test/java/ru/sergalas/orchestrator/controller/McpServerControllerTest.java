package ru.sergalas.orchestrator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.sergalas.orchestrator.dto.request.McpServerRequest;
import ru.sergalas.orchestrator.dto.response.McpServerResponse;
import ru.sergalas.orchestrator.entity.enums.TransportType;
import ru.sergalas.orchestrator.service.McpServerService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpServerController: REST API конфигурации MCP-серверов")
class McpServerControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private McpServerService mcpServerService;

    @InjectMocks
    private McpServerController mcpServerController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mcpServerController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /projects/{id}/mcp-servers: добавление MCP сервера")
    void createMcpServer_Created() throws Exception {
        McpServerRequest req = new McpServerRequest("MCP 1", "https://mcp.host.com", TransportType.SSE, true);
        McpServerResponse res = new McpServerResponse(1L, "MCP 1", "https://mcp.host.com", TransportType.SSE, true, LocalDateTime.now());

        when(mcpServerService.create(eq(10L), any(McpServerRequest.class), any())).thenReturn(res);

        mockMvc.perform(post("/projects/10/mcp-servers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("MCP 1"));
    }

    @Test
    @DisplayName("PUT /projects/{id}/mcp-servers/{sid}: обновление настроек MCP")
    void updateMcpServer_Ok() throws Exception {
        McpServerRequest req = new McpServerRequest("Updated", "https://mcp.host.com", TransportType.HTTP, false);
        McpServerResponse res = new McpServerResponse(1L, "Updated", "https://mcp.host.com", TransportType.HTTP, false, LocalDateTime.now());

        when(mcpServerService.update(eq(1L), any(McpServerRequest.class), any())).thenReturn(res);

        mockMvc.perform(put("/projects/10/mcp-servers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    @DisplayName("PATCH /projects/{id}/mcp-servers/{sid}/toggle: переключение флага активности")
    void toggleMcpServer_Ok() throws Exception {
        mockMvc.perform(patch("/projects/10/mcp-servers/1/toggle"))
                .andExpect(status().isOk());

        verify(mcpServerService).toggleActive(eq(1L), any());
    }

    @Test
    @DisplayName("DELETE /projects/{id}/mcp-servers/{sid}: удаление MCP сервера")
    void deleteMcpServer_NoContent() throws Exception {
        mockMvc.perform(delete("/projects/10/mcp-servers/1"))
                .andExpect(status().isNoContent());

        verify(mcpServerService).delete(eq(1L), any());
    }
}