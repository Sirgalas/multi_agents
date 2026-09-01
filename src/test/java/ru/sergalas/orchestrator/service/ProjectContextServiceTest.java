package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import ru.sergalas.orchestrator.dto.request.ProjectContextUploadRequest;
import ru.sergalas.orchestrator.dto.response.ProjectContextResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.Role;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;
import ru.sergalas.orchestrator.security.UserPrincipal;
import ru.sergalas.orchestrator.service.impl.ProjectContextServiceImpl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectContextService: Загрузка, валидация и генерация контекста файлов")
class ProjectContextServiceTest {

    @Mock
    private ProjectContextRepository projectContextRepository;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectContextServiceImpl projectContextService;

    private User ownerUser;
    private User otherUser;
    private Authentication ownerAuth;
    private Authentication otherAuth;
    private Project project;

    @BeforeEach
    void setUp() {
        ownerUser = User.builder().id(1L).username("owner").role(Role.ROLE_USER).build();
        otherUser = User.builder().id(2L).username("other").role(Role.ROLE_USER).build();
        ownerAuth = new UsernamePasswordAuthenticationToken(new UserPrincipal(ownerUser), null);
        otherAuth = new UsernamePasswordAuthenticationToken(new UserPrincipal(otherUser), null);

        project = Project.builder().id(10L).name("AI Context").user(ownerUser).build();
    }

    @Nested
    @DisplayName("Загрузка файлов контекста и валидация расширений")
    class UploadValidation {

        @ParameterizedTest
        @ValueSource(strings = {"task.md", "Service.java", "data.txt", ".env", "app.yaml", "config.yml", "DOC.MD", "SCHEMA.YAML"})
        @DisplayName("Разрешенные типы файлов успешно загружаются (включая проверку регистра)")
        void upload_AllowedExtensions_Success(String filename) {
            MockMultipartFile file = new MockMultipartFile("file", filename, "text/plain", "Sample Content".getBytes(StandardCharsets.UTF_8));
            ProjectContextUploadRequest request = new ProjectContextUploadRequest(file, FileType.TASK);

            when(projectService.getProjectEntity(10L, ownerAuth)).thenReturn(project);
            when(projectContextRepository.save(any(ProjectContext.class))).thenAnswer(inv -> {
                ProjectContext ctx = inv.getArgument(0);
                ctx.setId(50L);
                ctx.setCreatedAt(LocalDateTime.now());
                return ctx;
            });

            ProjectContextResponse response = projectContextService.upload(10L, request, ownerAuth);

            assertThat(response).isNotNull();
            assertThat(response.fileName()).isEqualTo(filename);
            assertThat(response.fileContent()).isEqualTo("Sample Content");
            verify(projectContextRepository).save(any(ProjectContext.class));
        }

        @Test
        @DisplayName("Пустой файл вызывает исключение")
        void upload_EmptyFile_ThrowsIllegalArgumentException() {
            MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.md", "text/plain", new byte[0]);
            ProjectContextUploadRequest request = new ProjectContextUploadRequest(emptyFile, FileType.TASK);

            when(projectService.getProjectEntity(10L, ownerAuth)).thenReturn(project);

            assertThatThrownBy(() -> projectContextService.upload(10L, request, ownerAuth))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("file is empty");
        }

        @ParameterizedTest
        @ValueSource(strings = {"script.sh", "virus.exe", "image.png", "archive.zip", "test.py"})
        @DisplayName("Запрещенные расширения файлов вызывают исключение")
        void upload_ForbiddenExtensions_ThrowsIllegalArgumentException(String filename) {
            MockMultipartFile file = new MockMultipartFile("file", filename, "application/octet-stream", "content".getBytes());
            ProjectContextUploadRequest request = new ProjectContextUploadRequest(file, FileType.TASK);

            when(projectService.getProjectEntity(10L, ownerAuth)).thenReturn(project);

            assertThatThrownBy(() -> projectContextService.upload(10L, request, ownerAuth))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid file type");
        }
    }

    @Nested
    @DisplayName("Удаление контекста")
    class DeleteContext {

        @Test
        @DisplayName("Владелец удаляет контекстный файл")
        void delete_Owner_Success() {
            ProjectContext context = ProjectContext.builder().id(70L).project(project).build();
            when(projectContextRepository.findById(70L)).thenReturn(Optional.of(context));

            projectContextService.delete(70L, ownerAuth);

            verify(projectContextRepository).delete(context);
        }

        @Test
        @DisplayName("Чужой пользователь получает отказ в удалении")
        void delete_NonOwner_ThrowsAccessDenied() {
            ProjectContext context = ProjectContext.builder().id(70L).project(project).build();
            when(projectContextRepository.findById(70L)).thenReturn(Optional.of(context));

            assertThatThrownBy(() -> projectContextService.delete(70L, otherAuth))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("Формирование текстового контекста для промптов")
    class BuildPromptContext {

        @Test
        @DisplayName("Пустой список файлов возвращает пустую строку")
        void buildPromptContext_Empty() {
            when(projectContextRepository.findAllByProjectIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());

            String promptContext = projectContextService.buildPromptContext(10L);

            assertThat(promptContext).isEmpty();
        }

        @Test
        @DisplayName("Множественные файлы форматируются в структурированный блок")
        void buildPromptContext_MultipleFiles() {
            ProjectContext file1 = ProjectContext.builder()
                    .fileName("spec.md")
                    .fileType(FileType.SPEC)
                    .fileContent("# Arch Spec")
                    .build();

            ProjectContext file2 = ProjectContext.builder()
                    .fileName("Main.java")
                    .fileType(FileType.CONTEXT_CODE)
                    .fileContent("public class Main {}")
                    .build();

            when(projectContextRepository.findAllByProjectIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(file1, file2));

            String promptContext = projectContextService.buildPromptContext(10L);

            assertThat(promptContext)
                    .contains("=== PROJECT FILES & SPECIFICATIONS ===")
                    .contains("--- File: spec.md (Type: SPEC) ---")
                    .contains("# Arch Spec")
                    .contains("--- File: Main.java (Type: CONTEXT_CODE) ---")
                    .contains("public class Main {}")
                    .contains("=== END PROJECT FILES ===");
        }
    }
}