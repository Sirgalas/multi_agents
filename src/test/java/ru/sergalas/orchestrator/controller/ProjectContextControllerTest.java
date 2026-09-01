package ru.sergalas.orchestrator.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.sergalas.orchestrator.dto.request.ProjectContextUploadRequest;
import ru.sergalas.orchestrator.dto.response.ProjectContextResponse;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.service.ProjectContextService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectContextController: REST API загрузки и удаления контекстных файлов")
class ProjectContextControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProjectContextService projectContextService;

    @InjectMocks
    private ProjectContextController projectContextController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectContextController).build();
    }

    @Test
    @DisplayName("GET /projects/{id}/context: возврат файлов контекста")
    void getContexts_Success() throws Exception {
        ProjectContextResponse res = new ProjectContextResponse(1L, 10L, "task.md", FileType.TASK, "content", LocalDateTime.now());
        when(projectContextService.getByProject(eq(10L), any())).thenReturn(List.of(res));

        mockMvc.perform(get("/projects/10/context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("task.md"));
    }

    @Test
    @DisplayName("POST /projects/{id}/context: загрузка multipart файла")
    void uploadContext_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "spec.md", "text/plain", "Spec content".getBytes());
        ProjectContextResponse res = new ProjectContextResponse(2L, 10L, "spec.md", FileType.SPEC, "Spec content", LocalDateTime.now());

        when(projectContextService.upload(eq(10L), any(ProjectContextUploadRequest.class), any())).thenReturn(res);

        mockMvc.perform(multipart("/projects/10/context")
                        .file(file)
                        .param("fileType", "SPEC"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.fileName").value("spec.md"));
    }

    @Test
    @DisplayName("DELETE /projects/{id}/context/{contextId}: удаление файла")
    void deleteContext_Success() throws Exception {
        mockMvc.perform(delete("/projects/10/context/5"))
                .andExpect(status().isNoContent());

        verify(projectContextService).delete(eq(5L), any());
    }
}