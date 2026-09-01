package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import ru.sergalas.orchestrator.service.impl.FileParserServiceImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileParserServiceImpl Unit Tests")
class FileParserServiceImplTest {

    private FileParserService fileParserService;

    @BeforeEach
    void setUp() {
        fileParserService = new FileParserServiceImpl();
    }

    @Test
    @DisplayName("Should successfully parse valid text file content")
    void shouldParseValidFileContent() throws IOException {
        String content = "public class Sample { int x = 42; }";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Sample.java",
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );

        String result = fileParserService.parseFileContent(file);

        assertThat(result).isEqualTo(content);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "application.yaml",
            "schema.sql",
            "spec.md",
            "data.json",
            "build.gradle",
            "config.properties",
            "script.ts",
            "index.js",
            "App.kt",
            "pom.xml",
            ".env",
            ".env.local"
    })
    @DisplayName("Should accept all allowed extensions and dot-env files")
    void shouldAcceptAllowedExtensions(String fileName) {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                "sample-content".getBytes(StandardCharsets.UTF_8)
        );

        assertThatCode(() -> fileParserService.validateFile(file))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept uppercase allowed extensions")
    void shouldAcceptUppercaseExtensions() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Application.YAML",
                "text/plain",
                "key: value".getBytes(StandardCharsets.UTF_8)
        );

        assertThatCode(() -> fileParserService.validateFile(file))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"archive.zip", "binary.exe", "image.png", "script.sh", "payload.bin"})
    @DisplayName("Should throw IllegalArgumentException for unsupported extensions")
    void shouldRejectUnsupportedExtensions(String fileName) {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "application/octet-stream",
                "binary-data".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> fileParserService.validateFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not supported");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when file is null")
    void shouldThrowExceptionWhenFileIsNull() {
        assertThatThrownBy(() -> fileParserService.validateFile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uploaded file is empty");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when file is empty (0 bytes)")
    void shouldThrowExceptionWhenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                new byte[0]
        );

        assertThatThrownBy(() -> fileParserService.validateFile(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uploaded file is empty");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when filename is blank")
    void shouldThrowExceptionWhenFilenameIsBlank() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "   ",
                "text/plain",
                "content".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> fileParserService.validateFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uploaded file name is invalid");
    }
}