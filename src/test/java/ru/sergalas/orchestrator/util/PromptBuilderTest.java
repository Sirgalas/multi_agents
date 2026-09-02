package ru.sergalas.orchestrator.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.TransportType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Tests: PromptBuilder Utility")
class PromptBuilderTest {

    @Test
    @DisplayName("Empty or null MCP servers produce empty section")
    void testEmptyMcpSection() {
        assertThat(PromptBuilder.buildMcpContextSection(null, null)).isEmpty();
        assertThat(PromptBuilder.buildMcpContextSection(Collections.emptyList(), Map.of())).isEmpty();
        assertThat(PromptBuilder.buildMcpContextSection(List.of(
                ProjectMcpServer.builder().name("Test").isActive(true).serverUrl("http://url").build()
        ), Collections.emptyMap())).isEmpty();
    }

    @Test
    @DisplayName("Active MCP servers with valid contexts are appended; inactive are skipped")
    void testActiveMcpSectionBuilding() {
        ProjectMcpServer activeServer = ProjectMcpServer.builder()
                .name("Spring Guide")
                .serverUrl("https://context7.com/spring")
                .transportType(TransportType.HTTP)
                .isActive(true)
                .build();

        ProjectMcpServer inactiveServer = ProjectMcpServer.builder()
                .name("React Guide")
                .serverUrl("https://context7.com/react")
                .transportType(TransportType.HTTP)
                .isActive(false)
                .build();

        Map<String, String> contexts = Map.of(
                "https://context7.com/spring", "Use Spring Boot 3.4 best practices.",
                "https://context7.com/react", "Use React hooks."
        );

        String result = PromptBuilder.buildMcpContextSection(List.of(activeServer, inactiveServer), contexts);

        assertThat(result).contains("### MCP CONTEXT & GUIDELINES");
        assertThat(result).contains("Spring Guide");
        assertThat(result).contains("Use Spring Boot 3.4 best practices.");
        assertThat(result).doesNotContain("React Guide");
    }

    @Test
    @DisplayName("Context files section builds correct [FILE: path] structure")
    void testContextFilesSection() {
        ProjectContext ctx1 = ProjectContext.builder()
                .fileName("pom.xml")
                .fileContent("<project/>")
                .fileType(FileType.CONTEXT_CODE)
                .build();

        ProjectContext ctx2 = ProjectContext.builder()
                .fileName("SPEC.md")
                .fileContent("# Architecture")
                .fileType(FileType.SPEC)
                .build();

        String result = PromptBuilder.buildContextFilesSection(List.of(ctx1, ctx2));

        assertThat(result).contains("### PROJECT EXISTING FILES");
        assertThat(result).contains("[FILE: pom.xml]");
        assertThat(result).contains("<project/>");
        assertThat(result).contains("[FILE: SPEC.md]");
        assertThat(result).contains("# Architecture");
    }

    @Test
    @DisplayName("Null or empty context files return empty string")
    void testEmptyContextFilesSection() {
        assertThat(PromptBuilder.buildContextFilesSection(null)).isEmpty();
        assertThat(PromptBuilder.buildContextFilesSection(Collections.emptyList())).isEmpty();
    }
}