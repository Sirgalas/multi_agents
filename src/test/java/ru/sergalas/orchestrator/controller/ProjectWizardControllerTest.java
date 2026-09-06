package ru.sergalas.orchestrator.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.sergalas.orchestrator.config.properties.McpProperties;
import ru.sergalas.orchestrator.dto.request.CreateProjectRequest;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.service.project.FileStructureService;
import ru.sergalas.orchestrator.service.project.ProjectService;
import ru.sergalas.orchestrator.service.project.TaskTemplateService;
import ru.sergalas.orchestrator.service.user.UserService;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectWizardController.class)
@AutoConfigureMockMvc
class ProjectWizardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private TaskTemplateService taskTemplateService;

    @MockitoBean
    private FileStructureService fileStructureService;

    @MockitoBean
    private McpProperties mcpProperties;

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser
    @DisplayName("GET /projects/new отображает форму мастера создания проекта")
    void showWizard_RendersWizardPage() throws Exception {
        when(taskTemplateService.getAllTemplates()).thenReturn(Collections.emptyList());
        when(fileStructureService.getAllTemplates()).thenReturn(Collections.emptyList());
        when(mcpProperties.getDefaultServers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/projects/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("project/wizard"))
                .andExpect(model().attributeExists("projectRequest"))
                .andExpect(model().attributeExists("taskTemplates"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /projects/new валидирует поля и перенаправляет на созданный проект")
    void submitWizard_ValidData_RedirectsToProject() throws Exception {
        User user = User.builder().id(1L).username("admin").build();
        Project created = Project.builder().id(10L).build();

        when(userService.getCurrentUser()).thenReturn(user);
        when(projectService.createProject(any(CreateProjectRequest.class), any(User.class))).thenReturn(created);

        mockMvc.perform(post("/projects/new")
                        .with(csrf())
                        .param("name", "Billing Service")
                        .param("description", "Payment gateway")
                        .param("taskContent", "# Task Content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects/10"));
    }

    @Test
    @WithMockUser
    @DisplayName("Edge Case: POST /projects/new с пустыми обязательными полями возвращает ту же форму с ошибками")
    void submitWizard_InvalidData_ReturnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/projects/new")
                        .with(csrf())
                        .param("name", "") // blank name
                        .param("taskContent", "")) // blank task content
                .andExpect(status().isOk())
                .andExpect(view().name("project/wizard"))
                .andExpect(model().hasErrors());
    }
}