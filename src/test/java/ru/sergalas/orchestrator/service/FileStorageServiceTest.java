package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.dto.request.FileUploadRequest;
import ru.sergalas.orchestrator.enums.FileType;
import ru.sergalas.orchestrator.model.Project;
import ru.sergalas.orchestrator.model.ProjectContext;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;
import ru.sergalas.orchestrator.service.impl.sympathyFileStorageServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-тесты файлового хранилища контекста (FileStorageService)")
class FileStorageServiceTest {

    @Mock
    private ProjectContextRepository contextRepository;

    @InjectMocks
    private sympathyFileStorageServiceImpl fileStorageService;

    @Test
    @DisplayName("Успешное сохранение контекстного файла с триммингом имени")
    void storeFileContext_TrimsFileNameAndSaves() {
        Project project = Project.builder().id(5L).name("Test Project").build();
        FileUploadRequest request = new FileUploadRequest(
                "   spec-v1.json   ",
                "{\"key\": \"value\"}",
                FileType.SPEC
        );

        when(contextRepository.save(any(ProjectContext.class))).thenAnswer(invocation -> {
            ProjectContext ctx = invocation.getArgument(0);
            ctx.setId(101L);
            return ctx;
        });

        ProjectContext savedContext = fileStorageService.storeFileContext(project, request);

        assertThat(savedContext).isNotNull();
        assertThat(savedContext.getId()).isEqualTo(101L);
        assertThat(savedContext.getFileName()).isEqualTo("spec-v1.json");
        assertThat(savedContext.getFileContent()).isEqualTo("{\"key\": \"value\"}");
        assertThat(savedContext.getFileType()).isEqualTo(FileType.SPEC);
        assertThat(savedContext.getProject()).isEqualTo(project);

        ArgumentCaptor<ProjectContext> captor = ArgumentCaptor.forClass(ProjectContext.class);
        verify(contextRepository).save(captor.capture());
        ProjectContext captured = captor.getValue();
        assertThat(captured.getFileName()).isEqualTo("spec-v1.json");
    }
}