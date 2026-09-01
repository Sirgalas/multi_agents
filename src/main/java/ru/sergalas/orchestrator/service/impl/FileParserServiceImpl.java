package ru.sergalas.orchestrator.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.sergalas.orchestrator.service.FileParserService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
public class FileParserServiceImpl implements FileParserService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "txt", "md", "json", "yaml", "yml", "java", "kt", "ts", "js", "xml", "gradle", "properties", "env", "sql"
    );

    @Override
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Uploaded file name is invalid");
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(dotIndex + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension) && !originalFilename.startsWith(".env")) {
            throw new IllegalArgumentException("File extension ." + extension + " is not supported. Allowed: " + ALLOWED_EXTENSIONS);
        }
    }

    @Override
    public String parseFileContent(MultipartFile file) throws IOException {
        validateFile(file);
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }
}