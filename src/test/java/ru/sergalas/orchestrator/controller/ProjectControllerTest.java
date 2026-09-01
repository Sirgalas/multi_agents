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
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.sergalas.orchestrator.dto.request.ProjectCreateRequest;
import ru.sergalas.orchestrator.dto.response.ProjectResponse;
import ru.sergalas.orchestrator.service.ProjectService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectController: Web & REST эндпоинты проектов")
class ProjectControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectController projectController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /projects: рендер HTML списка")
    void listProjects_HtmlView() throws Exception {
        when(projectService.getProjectsForUser(any())).thenReturn(List.of());

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/list"))
                .andExpect(model().attributeExists("projects"));
    }

    @Test
    @DisplayName("GET /projects/api: возврат JSON списка проектов")
    void listProjectsApi_Json() throws Exception {
        ProjectResponse p = new ProjectResponse(1L, "Proj 1", "Desc", LocalDateTime.now(), List.of(), List.of(), List.of());
        when(projectService.getProjectsForUser(any())).thenReturn(List.of(p));

        mockMvc.perform(get("/projects/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Proj 1"));
    }

    @Test
    @DisplayName("POST /projects: создание проекта через HTML форму и редирект")
    void createProjectForm_Redirects() throws Exception {
        mockMvc.perform(post("/projects")
                        .param("name", "Form Project")
                        .param("description", "Form Desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/projects"));

        verify(projectService).createProject(any(ProjectCreateRequest.class), any());
    }

    @Test
    @DisplayName("POST /projects/api: создание проекта через JSON API")
    void createProjectApi_Created() throws Exception {
        ProjectCreateRequest req = new ProjectCreateRequest("API Proj", "Desc");
        ProjectResponse res = new ProjectResponse(10L, "API Proj", "Desc", LocalDateTime.now(), List.of(), List.of(), List.of());

        when(projectService.createProject(any(ProjectCreateRequest.class), any())).thenReturn(res);

        mockMvc.perform(post("/projects/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("API Proj"));
    }

    @Test
    @DisplayName("DELETE /projects/{id}: успешное удаление проекта")
    void deleteProject_NoContent() throws Exception {
        mockMvc.perform(delete("/projects/100"))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(eq(100L), any(Authentication.class));
    }
}