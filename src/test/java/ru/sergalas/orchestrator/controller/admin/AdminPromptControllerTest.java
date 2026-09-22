package ru.sergalas.orchestrator.controller.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.sergalas.orchestrator.dto.request.CreateAgentPromptRequest;
import ru.sergalas.orchestrator.dto.request.UpdateAgentPromptRequest;
import ru.sergalas.orchestrator.entity.AgentPrompt;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.service.mcp.McpServerService;
import ru.sergalas.orchestrator.service.prompt.AgentPromptService;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminPromptController.class)
@AutoConfigureMockMvc
class AdminPromptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentPromptService agentPromptService;

    @MockitoBean
    private McpServerService mcpServerService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/prompts отображает страницу управления промптами")
    void listPrompts_AdminRole_RendersPage() throws Exception {
        AgentPrompt prompt = AgentPrompt.builder()
                .id(1L)
                .name("Архитектор")
                .stepName(StepName.ARCHITECT)
                .prompt("Промпт")
                .mcpServers(new HashSet<>())
                .build();
        when(agentPromptService.getAllPrompts()).thenReturn(List.of(prompt));
        when(mcpServerService.getAllServers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/prompts"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/prompts"))
                .andExpect(model().attributeExists("prompts"))
                .andExpect(model().attributeExists("steps"))
                .andExpect(model().attributeExists("allMcpServers"))
                .andExpect(model().attributeExists("createPromptRequest"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/prompts с валидными данными создает промпт и выполняет редирект")
    void createPrompt_ValidData_Redirects() throws Exception {
        mockMvc.perform(post("/admin/prompts")
                        .with(csrf())
                        .param("name", "Custom Architect")
                        .param("stepName", "ARCHITECT")
                        .param("prompt", "Analyze requirements...")
                        .param("description", "Custom prompt description"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/prompts"));

        verify(agentPromptService).createPrompt(any(CreateAgentPromptRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/prompts с пустыми обязательными полями возвращает ошибки валидации")
    void createPrompt_InvalidData_ReturnsFormWithErrors() throws Exception {
        when(agentPromptService.getAllPrompts()).thenReturn(Collections.emptyList());
        when(mcpServerService.getAllServers()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/admin/prompts")
                        .with(csrf())
                        .param("name", "")
                        .param("prompt", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/prompts"))
                .andExpect(model().hasErrors());

        verify(agentPromptService, never()).createPrompt(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/prompts/{id}/edit отображает форму редактирования промпта")
    void showEditForm_Found_RendersEditPage() throws Exception {
        AgentPrompt prompt = AgentPrompt.builder()
                .id(1L)
                .name("Архитектор")
                .stepName(StepName.ARCHITECT)
                .prompt("Промпт")
                .mcpServers(new HashSet<>())
                .build();
        when(agentPromptService.getPromptById(1L)).thenReturn(prompt);
        when(mcpServerService.getAllServers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/prompts/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/prompt-edit"))
                .andExpect(model().attributeExists("prompt"))
                .andExpect(model().attributeExists("updatePromptRequest"))
                .andExpect(model().attributeExists("steps"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/prompts/{id} обновляет промпт и выполняет редирект")
    void updatePrompt_ValidData_Redirects() throws Exception {
        mockMvc.perform(post("/admin/prompts/1")
                        .with(csrf())
                        .param("name", "Updated Architect")
                        .param("stepName", "ARCHITECT")
                        .param("prompt", "Updated prompt text..."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/prompts"));

        verify(agentPromptService).updatePrompt(eq(1L), any(UpdateAgentPromptRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/prompts/{id}/delete удаляет промпт и выполняет редирект")
    void deletePrompt_AdminRole_Redirects() throws Exception {
        mockMvc.perform(post("/admin/prompts/1/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/prompts"));

        verify(agentPromptService).deletePrompt(1L);
    }
}
