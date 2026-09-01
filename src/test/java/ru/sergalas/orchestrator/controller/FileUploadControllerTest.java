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
import ru.sergalas.orchestrator.dto.response.ProjectResponse;
import ru.sergalas.orchestrator.model.Project;
import ru.sergalas.orchestrator.model.ProjectContext;
import ru.sergalas.orchestrator.model.User;
import ru.sergalas.orchestrator.model.enums.FileType;
import ru.sergalas.orchestrator.model.enums.Role;
import ru.sergalas.orchestrator.service.FileStorageService;
import ru.sergalas.orchestrator.service.ProjectService;
import ru.sergalas.orchestrator.service.UserService;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: FileUploadController (Multipart & REST)")
class FileUploadControllerTest {

    private MockMvc mockMvc;

    @Mock private FileStorageService fileStorageService;
    @Mock private ProjectService projectService;
    @Mock private UserService userService;

    @InjectMocks
    private FileUploadController fileUploadController;

    private User appUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileUploadController).build();
        appUser = User.builder().id(1L).username("dev").role(Role.ROLE_USER).build();
    }

    @Test
    @DisplayName("POST /projects/{id}/files: uploads multiple non-empty files and ignores empty ones")
    void uploadFiles_MultipleFiles_ShouldStoreOnlyNonEmptyFiles() throws Exception {
        Long projectId = 10L;
        MockMultipartFile file1 = new MockMultipartFile("files", "Spec.md", "text/markdown", "# Spec".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "empty.txt", "text/plain", new byte[0]);

        Project project = Project.builder().id(projectId).build();
        ProjectContext savedContext = ProjectContext.builder()
                .id(100L)
                .project(project)
                .fileName("Spec.md")
                .fileType(FileType.SPEC)
                .build();

        when(userService.findByUsername("dev")).thenReturn(appUser);
        when(projectService.getById(projectId, 1L)).thenReturn(new ProjectResponse(projectId, "P", "D", Instant.now(), 0, 0));
        when(fileStorageService.store(eq(projectId), eq(file1), eq(FileType.SPEC))).thenReturn(savedContext);

        mockMvc.perform(multipart("/projects/{projectId}/files", projectId)
                        .file(file1)
                        .file(file2)
                        .param("fileType", "SPEC")
                        .principal(() -> "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.uploadedCount").value(1))
                .andExpect(jsonPath("$.files[0].fileName").value("Spec.md"));

        verify(fileStorageService, times(1)).store(eq(projectId), any(), eq(FileType.SPEC));
    }

    @Test
    @DisplayName("DELETE /projects/{id}/files/{fileId}: deletes context file and validates project ownership")
    void deleteFile_ShouldDeleteFileAndReturnOk() throws Exception {
        Long projectId = 10L;
        Long fileId = 555L;

        when(userService.findByUsername("dev")).thenReturn(appUser);
        when(projectService.getById(projectId, 1L)).thenReturn(new ProjectResponse(projectId, "P", "D", Instant.now(), 0, 0));

        mockMvc.perform(delete("/projects/{projectId}/files/{fileId}", projectId, fileId)
                        .principal(() -> "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedId").value(555));

        verify(fileStorageService).delete(fileId, projectId);
    }
}