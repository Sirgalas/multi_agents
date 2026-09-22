package ru.sergalas.orchestrator.service.project;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;
import ru.sergalas.orchestrator.service.project.impl.ProjectContextServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectContextServiceImplTest {

    @Mock
    private ProjectContextRepository projectContextRepository;

    @InjectMocks
    private ProjectContextServiceImpl projectContextService;

    @Test
    @DisplayName("Сохранение файла в контекст проекта с корректными атрибутами")
    void saveFile_Success() {
        // Arrange
        Project project = Project.builder().id(1L).name("Test Project").build();
        when(projectContextRepository.save(any(ProjectContext.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        ProjectContext saved = projectContextService.saveFile(
                project,
                "UserService.java",
                "/src/main/java/UserService.java",
                "public class UserService {}",
                FileType.CONTEXT_CODE,
                2
        );

        // Assert
        assertThat(saved.getProject()).isEqualTo(project);
        assertThat(saved.getFileName()).isEqualTo("UserService.java");
        assertThat(saved.getFilePath()).isEqualTo("/src/main/java/UserService.java");
        assertThat(saved.getFileContent()).isEqualTo("public class UserService {}");
        assertThat(saved.getFileType()).isEqualTo(FileType.CONTEXT_CODE);
        assertThat(saved.getIteration()).isEqualTo(2);
    }

    @Test
    @DisplayName("Edge Case: поиск последнего контекста по типу с наивысшей итерацией")
    void getLatestContextByType_Success() {
        // Arrange
        Project project = Project.builder().id(1L).build();
        ProjectContext context = ProjectContext.builder()
                .fileType(FileType.TASK)
                .iteration(3)
                .fileContent("Final Spec")
                .build();

        when(projectContextRepository.findFirstByProjectAndFileTypeOrderByIterationDesc(project, FileType.TASK))
                .thenReturn(Optional.of(context));

        // Act
        Optional<ProjectContext> result = projectContextService.getLatestContextByType(project, FileType.TASK);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getIteration()).isEqualTo(3);
        assertThat(result.get().getFileContent()).isEqualTo("Final Spec");
    }
}