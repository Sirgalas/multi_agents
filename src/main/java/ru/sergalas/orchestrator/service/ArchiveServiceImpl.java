package ru.sergalas.orchestrator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    private final ProjectContextRepository contextRepository;

    @Value("${app.output.directory:./output}")
    private String outputDirectory;

    @Override
    public String createProjectArchive(Long projectId, Map<String, String> files) {
        try {
            Path outDirPath = Paths.get(outputDirectory);
            if (!Files.exists(outDirPath)) {
                Files.createDirectories(outDirPath);
            }

            String zipFileName = "project_" + projectId + ".zip";
            Path zipPath = outDirPath.resolve(zipFileName);

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                for (Map.Entry<String, String> entry : files.entrySet()) {
                    String rawPath = entry.getKey().replace("\\", "/");
                    if (rawPath.startsWith("/")) {
                        rawPath = rawPath.substring(1);
                    }
                    ZipEntry zipEntry = new ZipEntry(rawPath);
                    zos.putNextEntry(zipEntry);
                    zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }

            log.info("Successfully created project ZIP: {}", zipPath.toAbsolutePath());
            return zipPath.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("Failed to create ZIP archive for project {}: {}", projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to create ZIP archive", e);
        }
    }

    @Override
    public Resource getArchive(Long projectId) throws IOException {
        Path zipPath = Paths.get(outputDirectory, "project_" + projectId + ".zip");

        if (!Files.exists(zipPath)) {
            List<ProjectContext> contexts = contextRepository.findByProjectId(projectId);
            if (contexts.isEmpty()) {
                throw new IOException("No files found for project " + projectId);
            }

            Map<String, String> files = contexts.stream()
                    .collect(Collectors.toMap(
                            ProjectContext::getFileName,
                            ProjectContext::getFileContent,
                            (existing, replacement) -> replacement
                    ));
            createProjectArchive(projectId, files);
        }

        return new UrlResource(zipPath.toUri());
    }
}