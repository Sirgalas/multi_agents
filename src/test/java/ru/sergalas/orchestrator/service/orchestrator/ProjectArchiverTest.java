package ru.sergalas.orchestrator.service.orchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.sergalas.orchestrator.dto.internal.GeneratedFile;
import ru.sergalas.orchestrator.dto.response.ProjectArchiveResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectArchiverTest {

    private ProjectArchiver projectArchiver;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        projectArchiver = new ProjectArchiver(tempDir.toString());
    }

    @Test
    @DisplayName("Edge Case: корректное извлечение нескольких файлов из одного контекста по маркерам [FILE: path]")
    void extractFiles_MultipleFilesWithCodeBlocks() {
        // Arrange
        String markdown = """
                [FILE: src/main/java/ru/sergalas/Main.java]
                ```java
                package ru.sergalas;
                public class Main { }
                ```
                
                Some description in between.
                
                [FILE: src/main/resources/application.yaml]
                ```yaml
                server:
                  port: 8080
                ```
                """;

        ProjectContext context = ProjectContext.builder()
                .fileName("CODE.md")
                .fileContent(markdown)
                .build();

        // Act
        List<GeneratedFile> files = projectArchiver.extractFiles(List.of(context));

        // Assert
        assertThat(files).hasSize(2);
        assertThat(files.get(0).getPath()).isEqualTo("src/main/java/ru/sergalas/Main.java");
        assertThat(files.get(0).getContent()).contains("package ru.sergalas;");
        assertThat(files.get(1).getPath()).isEqualTo("src/main/resources/application.yaml");
        assertThat(files.get(1).getContent()).contains("port: 8080");
    }

    @Test
    @DisplayName("Edge Case: контекст без маркера [FILE: ...] использует fileName как путь к файлу")
    void extractFiles_FallbackToFileName_WhenNoMarker() {
        // Arrange
        ProjectContext context = ProjectContext.builder()
                .fileName("README.md")
                .fileContent("# Project Documentation")
                .build();

        // Act
        List<GeneratedFile> files = projectArchiver.extractFiles(List.of(context));

        // Assert
        assertThat(files).hasSize(1);
        assertThat(files.get(0).getPath()).isEqualTo("README.md");
        assertThat(files.get(0).getContent()).isEqualTo("# Project Documentation");
    }

    @Test
    @DisplayName("Создание валидного ZIP архива и проверка содержимого архива")
    void createZipArchive_Success() throws Exception {
        // Arrange
        Project project = Project.builder().id(42L).name("My Test App").build();
        ProjectContext context = ProjectContext.builder()
                .fileName("TASK.md")
                .fileContent("[FILE: /docs/TASK.md]\n# Task content")
                .build();

        // Act
        ProjectArchiveResponse response = projectArchiver.createZipArchive(project, List.of(context));

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getArchivePath()).endsWith(".zip");
        assertThat(response.getSizeBytes()).isGreaterThan(0);

        File zipFile = new File(response.getArchivePath());
        assertThat(zipFile).exists();

        // Verify ZIP Entries
        List<String> entryNames = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                zis.closeEntry();
            }
        }

        assertThat(entryNames).contains("docs/TASK.md");
    }

    @Test
    @DisplayName("Edge Case: дублирующиеся имена файлов в контексте дедуплицируются без выброса ZipException")
    void createZipArchive_DeduplicatesFiles_AvoidsZipException() throws Exception {
        // Arrange: Multiple contexts with the exact same file path (e.g. repeated iterations of ARCHITECTURE_SPEC.md)
        Project project = Project.builder().id(99L).name("Duplication Test").build();
        ProjectContext spec1 = ProjectContext.builder()
                .fileName("ARCHITECTURE_SPEC.md")
                .fileContent("Old spec version")
                .build();
        ProjectContext spec2 = ProjectContext.builder()
                .fileName("ARCHITECTURE_SPEC.md")
                .fileContent("New updated spec version")
                .build();

        // Act: Should succeed without throwing ZipException: duplicate entry
        ProjectArchiveResponse response = projectArchiver.createZipArchive(project, List.of(spec1, spec2));

        // Assert
        assertThat(response).isNotNull();
        File zipFile = new File(response.getArchivePath());
        assertThat(zipFile).exists();

        List<String> entryNames = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                zis.closeEntry();
            }
        }

        // ARCHITECTURE_SPEC.md should be present exactly once in the zip
        assertThat(entryNames).containsExactly("ARCHITECTURE_SPEC.md");
    }
}