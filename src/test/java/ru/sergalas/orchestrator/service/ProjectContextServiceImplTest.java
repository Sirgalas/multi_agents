package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import ru.sergalas.orchestrator.dto.request.UploadContextRequest;
import ru.sergalas.orchestrator.enums.FileType;
import ru.sergalas.orchestrator.exception.ProjectNotFoundException;
import ru.sergalas.orchestrator.exception.ResourceNotFoundException;
import ru.sergalas.orchestrator.model.Project;
import ru.sergalas.orchestrator.model.ProjectContext;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.impl.ProjectContextServiceImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectContextServiceImpl Unit Tests")
class ProjectContextServiceImplTest {

    @Mock
    private ProjectContextRepository projectContextRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private FileParserService fileParserService;

    @InjectMocks
    private ProjectContextServiceImpl projectContextService;

    @Test
    @DisplayName("Should successfully add context file to project")
    void shouldAddContextSuccessfully() throws IOException {
        Long projectId = 1L;
        Long userId = 10L;
        Project project = Project.builder().id(projectId).build();

        MockMultipartFile file = new MockMultipartFile(
                "file", "architecture.md", "text/markdown", "# Architecture".getBytes(StandardCharsets.UTF_8)
        );
        UploadContextRequest req = new UploadContextRequest(file, FileType.SPEC);

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(fileParserService.parseFileContent(file)).thenReturn("# Architecture");
        when(projectContextRepository.save(any(ProjectContext.class))).thenAnswer(invocation -> {
            ProjectContext ctx = invocation.getArgument(0);
            ctx.setId(101L);
            return ctx;
        });

        ProjectContext saved = projectContextService.addContext(projectId, userId, req);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(101L);
        assertThat(saved.getFileName()).isEqualTo("architecture.md");
        assertThat(saved.getFileContent()).isEqualTo("# Architecture");
        assertThat(saved.getFileType()).isEqualTo(FileType.SPEC);
    }

    @Test
    @DisplayName("Should throw ProjectNotFoundException when adding context to non-existent project")
    void shouldThrowExceptionWhenProjectNotFoundOnAddContext() {
        Long projectId = 99L;
        Long userId = 10L;
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", new byte[]{1});
        UploadContextRequest req = new UploadContextRequest(file, FileType.TASK);

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectContextService.addContext(projectId, userId, req))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    @DisplayName("Should aggregate multiple contexts into single structured string")
    void shouldAggregateContextsAsText() {
        Long projectId = 1L;
        List<ProjectContext> contexts = List.of(
                ProjectContext.builder()
                        .fileName("task.md")
                        .fileType(FileType.TASK)
                        .fileContent("Build Auth Module")
                        .build(),
                ProjectContext.builder()
                        .fileName(".env")
                        .fileType(FileType.ENV_FILE)
                        .fileContent("PORT=8080")
                        .build()
        );

        when(projectContextRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId)).thenReturn(contexts);

        String aggregated = projectContextService.aggregateContextAsText(projectId);

        assertThat(aggregated)
                .contains("--- FILE: task.md [TYPE: TASK] ---")
                .contains("Build Auth Module")
                .contains("--- FILE: .env [TYPE: ENV_FILE] ---")
                .contains("PORT=8080");
    }

    @Test
    @DisplayName("Should return empty string when aggregating contexts for project with no files")
    void shouldReturnEmptyStringWhenNoContextFiles() {
        Long projectId = 1L;
        when(projectContextRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId))
                .thenReturn(Collections.emptyList());

        String result = projectContextService.aggregateContextAsText(projectId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should delete context file when owner requests it")
    void shouldDeleteContextSuccessfully() {
        Long projectId = 1L;
        Long contextId = 5L;
        Long userId = 10L;
        Project project = Project.builder().id(projectId).build();
        ProjectContext context = ProjectContext.builder().id(contextId).project(project).build();

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(projectContextRepository.findByIdAndProjectId(contextId, projectId)).thenReturn(Optional.of(context));

        projectContextService.deleteContext(contextId, projectId, userId);

        verify(projectContextRepository).delete(context);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when context does not belong to project")
    void shouldThrowExceptionWhenContextNotFound() {
        Long projectId = 1L;
        Long contextId = 999L;
        Long userId = 10L;
        Project project = Project.builder().id(projectId).build();

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(projectContextRepository.findByIdAndProjectId(contextId, projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectContextService.deleteContext(contextId, projectId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Context file not found: 999");
    }
}