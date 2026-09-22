package ru.sergalas.orchestrator.controller.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.sergalas.orchestrator.dto.request.CreateMcpServerRequest;
import ru.sergalas.orchestrator.entity.McpServer;
import ru.sergalas.orchestrator.entity.enums.McpTarget;
import ru.sergalas.orchestrator.service.mcp.McpServerService;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminMcpServerController.class)
@AutoConfigureMockMvc
class AdminMcpServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private McpServerService mcpServerService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/mcp-servers отображает страницу управления MCP серверами")
    void listMcpServers_AdminRole_RendersAdminPage() throws Exception {
        McpServer server = McpServer.builder().id(1L).name("Spring Boot").target(McpTarget.BACKEND).build();
        when(mcpServerService.getAllServers()).thenReturn(List.of(server));

        mockMvc.perform(get("/admin/mcp-servers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/mcp-servers"))
                .andExpect(model().attributeExists("servers"))
                .andExpect(model().attributeExists("createServerRequest"))
                .andExpect(model().attributeExists("targets"));
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/mcp-servers с валидными данными регистрирует сервер и выполняет редирект")
    void createMcpServer_ValidData_Redirects() throws Exception {
        mockMvc.perform(post("/admin/mcp-servers")
                        .with(csrf())
                        .param("name", "Docker Guidelines")
                        .param("url", "https://context7.com/docker")
                        .param("target", "COMMON")
                        .param("token", "secret123")
                        .param("description", "Containerization standards"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/mcp-servers"));

        verify(mcpServerService).createServer(any(CreateMcpServerRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/mcp-servers с пустыми полями возвращает ту же форму с ошибками валидации")
    void createMcpServer_InvalidData_ReturnsFormWithErrors() throws Exception {
        when(mcpServerService.getAllServers()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/admin/mcp-servers")
                        .with(csrf())
                        .param("name", "")
                        .param("url", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/mcp-servers"))
                .andExpect(model().hasErrors());

        verify(mcpServerService, never()).createServer(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/mcp-servers с дубликатом имени возвращает форму с ошибкой поля name")
    void createMcpServer_DuplicateName_ReturnsFormWithFieldError() throws Exception {
        when(mcpServerService.getAllServers()).thenReturn(Collections.emptyList());
        when(mcpServerService.createServer(any(CreateMcpServerRequest.class)))
                .thenThrow(new IllegalArgumentException("MCP сервер с таким именем уже существует"));

        mockMvc.perform(post("/admin/mcp-servers")
                        .with(csrf())
                        .param("name", "Spring Boot Guidelines")
                        .param("url", "https://context7.com/spring-boot")
                        .param("target", "BACKEND"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/mcp-servers"))
                .andExpect(model().attributeHasFieldErrors("createServerRequest", "name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/mcp-servers/{id}/delete удаляет сервер и выполняет редирект")
    void deleteMcpServer_AdminRole_Redirects() throws Exception {
        mockMvc.perform(post("/admin/mcp-servers/5/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/mcp-servers"));

        verify(mcpServerService).deleteServer(5L);
    }
}
