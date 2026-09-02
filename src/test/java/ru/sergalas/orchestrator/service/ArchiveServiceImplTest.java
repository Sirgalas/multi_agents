package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: ArchiveServiceImpl ZIP generation and retrieval")
class ArchiveServiceImplTest {

    @Mock
    private ProjectContextRepository contextRepository;

    @InjectMocks
    private ArchiveServiceImpl archiveService;

    @Test
    @DisplayName("createProjectArchive creates a valid ZIP archive with normalized entry names")
    void testCreateProjectArchive(@TempDir Path tempDir) throws IOException {
        ReflectionTestUtils.setField(archiveService, "outputDirectory", tempDir.toString());

        Map<String, String> files = Map.of(
                "src\\main\\java\\App.java", "public class App {}",
                "/README.md", "# Project Documentation",
                "config/app.yaml", "key: value"
        );

        String archivePath = archiveService.createProjectArchive(1L, files);

        File zipFile = new File(archivePath);
        assertThat(zipFile).exists();
        assertThat(zipFile.length()).isGreaterThan(0);

        try (ZipFile zf = new ZipFile(zipFile)) {
            assertThat(zf.getEntry("src/main/java/App.java")).isNotNull();
            assertThat(zf.getEntry("README.md")).isNotNull();
            assertThat(zf.getEntry("config/app.yaml")).isNotNull();
        }
    }

    @Test
    @DisplayName("getArchive re-creates archive from ProjectContext if file is not on disk")
    void testGetArchiveRecreateFromDb(@TempDir Path tempDir) throws IOException {
        ReflectionTestUtils.setField(archiveService, "outputDirectory", tempDir.toString());

        List<ProjectContext> contexts = List.of(
                ProjectContext.builder().fileName("build.gradle").fileContent("plugins {}").fileType(FileType.CONTEXT_CODE).build()
        );
        when(contextRepository.findByProjectId(2L)).thenReturn(contexts);

        Resource resource = archiveService.getArchive(2L);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.getFilename()).isEqualTo("project_2.zip");
    }

    @Test
    @DisplayName("getArchive throws IOException when no files exist on disk or DB")
    void testGetArchiveEmptyThrows(@TempDir Path tempDir) {
        ReflectionTestUtils.setField(archiveService, "outputDirectory", tempDir.toString());
        when(contextRepository.findByProjectId(999L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> archiveService.getArchive(999L))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("No files found for project 999");
    }
}