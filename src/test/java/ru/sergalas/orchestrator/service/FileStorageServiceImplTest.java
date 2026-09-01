package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.sergalas.orchestrator.model.Project;
import ru.sergalas.orchestrator.model.ProjectContext;
import ru.sergalas.orchestrator.model.enums.FileType;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.impl.FileStorageServiceImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: FileStorageServiceImpl")
class FileStorageServiceImplTest {

    @Mock
    private ProjectContextRepository contextRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    @Test
    @DisplayName("store: successfully saves uploaded file content as UTF-8")
    void store_ValidFile_ShouldPersistProjectContext() {
        Long projectId = 1L;
        Project project = Project.builder().id(projectId).name("P1").build();
        byte[] contentBytes = "public class App {}".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("files", "App.java", "text/plain", contentBytes);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(contextRepository.save(any(ProjectContext.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectContext saved = fileStorageService.store(projectId, file, FileType.SOURCE_CODE);

        assertNotNull(saved);
        assertEquals("App.java", saved.getFileName());
        assertEquals("public class App {}", saved.getFileContent());
        assertEquals(FileType.SOURCE_CODE, saved.getFileType());
        assertEquals(project, saved.getProject());
    }

    @Test
    @DisplayName("Edge Case: store with null FileType defaults to FileType.TASK")
    void store_NullFileType_ShouldDefaultToTask() {
        Long projectId = 1L;
        Project project = Project.builder().id(projectId).build();
        MockMultipartFile file = new MockMultipartFile("files", "spec.md", "text/markdown", "# Spec".getBytes());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(contextRepository.save(any(ProjectContext.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectContext saved = fileStorageService.store(projectId, file, null);

        assertEquals(FileType.TASK, saved.getFileType());
    }

    @Test
    @DisplayName("Edge Case: store with empty file throws IllegalArgumentException")
    void store_EmptyFile_ShouldThrowIllegalArgumentException() {
        Long projectId = 1L;
        Project project = Project.builder().id(projectId).build();
        MockMultipartFile emptyFile = new MockMultipartFile("files", "empty.txt", "text/plain", new byte[0]);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.store(projectId, emptyFile, FileType.TASK));
        verify(contextRepository, never()).save(any());
    }

    @Test
    @DisplayName("Edge Case: store when project does not exist throws NoSuchElementException")
    void store_ProjectNotFound_ShouldThrowNoSuchElementException() {
        MockMultipartFile file = new MockMultipartFile("files", "file.txt", "text/plain", "data".getBytes());
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> fileStorageService.store(999L, file, FileType.TASK));
    }

    @Test
    @DisplayName("Edge Case: store wraps IOException into RuntimeException")
    void store_IOException_ShouldThrowRuntimeException() throws IOException {
        Long projectId = 1L;
        Project project = Project.builder().id(projectId).build();
        MultipartFile brokenFile = mock(MultipartFile.class);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(brokenFile.isEmpty()).thenReturn(false);
        when(brokenFile.getOriginalFilename()).thenReturn("broken.bin");
        when(brokenFile.getBytes()).thenThrow(new IOException("Disk read failed"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileStorageService.store(projectId, brokenFile, FileType.TASK));
        assertTrue(ex.getMessage().contains("Failed to process uploaded file"));
    }

    @Test
    @DisplayName("listForProject: returns all context files for project")
    void listForProject_ShouldReturnFileList() {
        Long projectId = 1L;
        ProjectContext pc1 = ProjectContext.builder().id(10L).fileName("a.txt").build();
        ProjectContext pc2 = ProjectContext.builder().id(11L).fileName("b.txt").build();

        when(contextRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId)).thenReturn(List.of(pc1, pc2));

        List<ProjectContext> list = fileStorageService.listForProject(projectId);

        assertEquals(2, list.size());
        assertEquals("a.txt", list.getFirst().getFileName());
    }

    @Test
    @DisplayName("delete: deletes context when matching projectId is provided")
    void delete_ValidContextAndProject_ShouldDelete() {
        Long contextId = 10L;
        Long projectId = 1L;
        ProjectContext context = ProjectContext.builder().id(contextId).build();

        when(contextRepository.findByIdAndProjectId(contextId, projectId)).thenReturn(Optional.of(context));

        fileStorageService.delete(contextId, projectId);

        verify(contextRepository).delete(context);
    }

    @Test
    @DisplayName("Edge Case: delete throws NoSuchElementException when context does not belong to project")
    void delete_ContextNotBelongingToProject_ShouldThrowException() {
        Long contextId = 10L;
        Long projectId = 999L;

        when(contextRepository.findByIdAndProjectId(contextId, projectId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> fileStorageService.delete(contextId, projectId));
        verify(contextRepository, never()).delete(any());
    }
}