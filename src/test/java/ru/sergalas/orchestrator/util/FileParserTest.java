package ru.sergalas.orchestrator.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Tests: FileParser Marker Extraction")
class FileParserTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t  \n"})
    @DisplayName("Empty or blank input returns empty map")
    void testEmptyOrBlankInput(String input) {
        Map<String, String> files = FileParser.parseFiles(input);
        assertThat(files).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Standard multi-file markdown response extraction")
    void testStandardFileExtraction() {
        String input = """
                Here is your project structure:
                
                [FILE: src/main/java/ru/sergalas/Test.java]
                ```java
                package ru.sergalas;
                public class Test {}
                ```
                
                [FILE: build.gradle]
                ```gradle
                plugins {
                    id 'java'
                }
                ```
                """;

        Map<String, String> files = FileParser.parseFiles(input);

        assertThat(files).hasSize(2);
        assertThat(files).containsEntry("src/main/java/ru/sergalas/Test.java", "package ru.sergalas;\npublic class Test {}");
        assertThat(files).containsEntry("build.gradle", "plugins {\n    id 'java'\n}");
    }

    @Test
    @DisplayName("Fallback parsing when standard code fence is missing or formatted irregularly")
    void testFallbackParsing() {
        String input = """
                [FILE: application.yaml]
                server:
                  port: 8080
                [FILE: README.md]
                # AI Orchestrator
                Instructions here.
                """;

        Map<String, String> files = FileParser.parseFiles(input);

        assertThat(files).hasSize(2);
        assertThat(files).containsKey("application.yaml");
        assertThat(files.get("application.yaml")).contains("server:\n  port: 8080");
        assertThat(files).containsKey("README.md");
        assertThat(files.get("README.md")).contains("# AI Orchestrator");
    }

    @Test
    @DisplayName("File paths with Windows separators and spaces are trimmed properly")
    void testWindowsPathAndSpacedMarkers() {
        String input = """
                [FILE:   docker\\harness\\Dockerfile   ]
                ```dockerfile
                FROM node:22
                CMD ["pnpm", "start"]
                ```
                """;

        Map<String, String> files = FileParser.parseFiles(input);

        assertThat(files).hasSize(1);
        assertThat(files).containsKey("docker\\harness\\Dockerfile");
        assertThat(files.get("docker\\harness\\Dockerfile")).contains("FROM node:22");
    }
}