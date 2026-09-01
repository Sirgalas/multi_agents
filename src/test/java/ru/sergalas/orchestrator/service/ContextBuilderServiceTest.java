package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.enums.FileType;
import ru.sergalas.orchestrator.model.Project;
import ru.sergalas.orchestrator.model.ProjectContext;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.impl.ContextBuilderServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-тесты сборщика контекста проекта (ContextBuilderService)")
class ContextBuilderServiceTest {

    @Mock
    private ProjectContextRepository contextRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ContextBuilderServiceImpl contextBuilderService;

    @Test
    @DisplayName("Выброс исключения, если проект не существует")
    void buildContextString_ProjectNotFound_ThrowsException() {
        Long projectId = 100L;
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contextBuilderService.buildContextString(projectId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Проект не найден: ID " + projectId);

        verify(projectRepository).findById(projectId);
    }

    @Test
    @DisplayName("Формирование контекста при отсутствии контекстных файлов")
    void buildContextString_NoContextFiles_ReturnsDefaultFallbackNotice() {
        Long projectId = 1L;
        Project project = Project.builder()
                .id(projectId)
                .name("Payment Gateway")
                .description("Handles online transactions")
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(contextRepository.findAllByProjectIdOrderByIdAsc(projectId)).thenReturn(List.of());

        String context = contextBuilderService.buildContextString(projectId);

        assertThat(context)
                .contains("Project Name: Payment Gateway")
                .contains("Project Description: Handles online transactions")
                .contains("Контекстные файлы отсутствуют. Используется только базовое описание проекта.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("Формирование контекста при отсутствии или пустом описании проекта")
    void buildContextString_BlankDescription_OmitsDescriptionField(String blankDesc) {
        Long projectId = 1L;
        Project project = Project.builder()
                .id(projectId)
                .name("Core API")
                .description(blankDesc)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(contextRepository.findAllByProjectIdOrderByIdAsc(projectId)).thenReturn(List.of());

        String context = contextBuilderService.buildContextString(projectId);

        assertThat(context)
                .contains("Project Name: Core API")
                .doesNotContain("Project Description:");
    }

    @Test
    @DisplayName("Корректная сборка контекста с несколькими разнотипными файлами")
    void buildContextString_MultipleFiles_FormatsProperly() {
        Long projectId = 1L;
        Project project = Project.builder()
                .id(projectId)
                .name("Order Service")
                .description("Processes user carts")
                .build();

        ProjectContext taskFile = ProjectContext.builder()
                .id(10L)
                .fileName("requirements.md")
                .fileType(FileType.TASK)
                .fileContent("Need to support discounts")
                .build();

        ProjectContext schemaFile = ProjectContext.builder()
                .id(11L)
                .fileName("schema.sql")
                .fileType(FileType.SPEC)
                .fileContent("CREATE TABLE orders (id BIGINT);")
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(contextRepository.findAllByProjectIdOrderByIdAsc(projectId))
                .thenReturn(List.of(taskFile, schemaFile));

        String context = contextBuilderService.buildContextString(projectId);

        assertThat(context)
                .contains("=== PROJECT METADATA ===")
                .contains("Project Name: Order Service")
                .contains("Project Description: Processes user carts")
                .contains("=== PROJECT CONTEXT FILES ===")
                .contains("--- FILE: requirements.md [TYPE: TASK] ---")
                .contains("Need to support discounts")
                .contains("--- FILE: schema.sql [TYPE: SPEC] ---")
                .contains("CREATE TABLE orders (id BIGINT);")
                .contains("=============================");
    }
}