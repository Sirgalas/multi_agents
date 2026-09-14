package ru.sergalas.orchestrator.service.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.sergalas.orchestrator.dto.internal.GeneratedFile;
import ru.sergalas.orchestrator.dto.response.ProjectArchiveResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class ProjectArchiver {

    private final String outputDirectory;
    private static final Pattern FILE_PATTERN = Pattern.compile(
            "\\[FILE:\\s*([^\\]]+)\\]\\s*(?:```[a-zA-Z0-9_-]*\\n)?([\\s\\S]*?)(?:```|$)",
            Pattern.MULTILINE
    );

    public ProjectArchiver(@Value("${app.output.directory:./output}") String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public ProjectArchiveResponse createZipArchive(Project project, List<ProjectContext> contexts) {
        try {
            Path outDirPath = Paths.get(outputDirectory);
            if (!Files.exists(outDirPath)) {
                Files.createDirectories(outDirPath);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String sanitizedName = project.getName().replaceAll("[^a-zA-Z0-9_-]", "_");
            String zipFileName = String.format("project_%d_%s_%s.zip", project.getId(), sanitizedName, timestamp);
            Path zipFilePath = outDirPath.resolve(zipFileName);

            List<GeneratedFile> files = extractFiles(contexts);

            try (FileOutputStream fos = new FileOutputStream(zipFilePath.toFile());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                for (GeneratedFile file : files) {
                    String cleanPath = file.getPath().replace("\\", "/").replaceAll("^/+", "");
                    ZipEntry zipEntry = new ZipEntry(cleanPath);
                    zos.putNextEntry(zipEntry);
                    zos.write(file.getContent().getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }

            File createdFile = zipFilePath.toFile();
            log.info("Project archive created successfully at: {}", createdFile.getAbsolutePath());

            return ProjectArchiveResponse.builder()
                    .archivePath(createdFile.getAbsolutePath())
                    .downloadUrl("/projects/" + project.getId() + "/download")
                    .sizeBytes(createdFile.length())
                    .build();

        } catch (Exception e) {
            log.error("Failed to create ZIP archive for project: {}", project.getId(), e);
            throw new RuntimeException("Error during project archive generation: " + e.getMessage(), e);
        }
    }

    public List<GeneratedFile> extractFiles(List<ProjectContext> contexts) {
        List<GeneratedFile> files = new ArrayList<>();

        for (ProjectContext context : contexts) {
            String content = context.getFileContent();
            if (content == null || content.isBlank()) {
                continue;
            }

            Matcher matcher = FILE_PATTERN.matcher(content);
            boolean matchedAny = false;
            while (matcher.find()) {
                matchedAny = true;
                String relativePath = matcher.group(1).trim();
                String fileContent = matcher.group(2).trim();
                files.add(GeneratedFile.builder()
                        .path(relativePath)
                        .content(fileContent)
                        .build());
            }

            if (!matchedAny && context.getFileName() != null) {
                files.add(GeneratedFile.builder()
                        .path(context.getFileName())
                        .content(content)
                        .build());
            }
        }
        return files;
    }
}