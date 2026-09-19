package ru.sergalas.orchestrator.service.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.entity.enums.McpTarget;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpClientService {

    private static final int MAX_CHARS_PER_SERVER = 1500;
    private static final int MAX_TOTAL_MCP_CHARS = 12000;

    private static final Pattern SCRIPT_STYLE_PATTERN = Pattern.compile(
            "<(script|style|svg|noscript|header|footer|nav)[^>]*>[\\s\\S]*?</\\1>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HTML_TAGS_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern MULTI_WHITESPACE_PATTERN = Pattern.compile("[ \\t]+");
    private static final Pattern MULTI_NEWLINE_PATTERN = Pattern.compile("(\\r?\\n){3,}");

    private final ProjectMcpServerRepository mcpServerRepository;
    private final RestClient restClient;

    /**
     * Curated concise best practice summaries for known technologies to guarantee
     * high-quality context and protect against massive HTML dumps.
     */
    private static final Map<String, String> CURATED_RULES = Map.ofEntries(
            Map.entry("Spring Boot Guidelines", "Spring Boot 3.4+, Java 21 LTS records for DTOs, constructor injection, @Valid, RFC 7807 ProblemDetail, Spring Data JPA with explicit transactions."),
            Map.entry("Hibernate ORM Best Practices", "Avoid N+1 queries using EntityGraph/JOIN FETCH, use database indexes on FKs, immutable projections, bidirectional helper methods, validate ddl-auto."),
            Map.entry("Java Modern Conventions", "Idiomatic Java 21, sealed interfaces, record patterns, pattern matching for switch, virtual threads for I/O, immutable collections."),
            Map.entry("PostgreSQL Best Practices", "Use UUID/bigserial PKs, JSONB for flexible attributes, TIMESTAMP WITH TIME ZONE, Flyway schema migrations, proper index coverage."),
            Map.entry("React Guidelines & Hooks", "Functional components, TypeScript strict types, custom hooks for business logic, memoization for expensive renders, clean state management."),
            Map.entry("Next.js Fullstack Architecture", "App Router architecture, React Server Components by default, Client Components with 'use client', Server Actions for mutations."),
            Map.entry("React Native Mobile Standards", "Modular components, platform-specific adaptations (iOS/Android), offline-first caching, background services handling, smooth animations."),
            Map.entry("Flutter Framework & UI Widgets", "State management (Bloc/Riverpod), const widgets for rebuild optimization, responsive layouts, repository pattern for API abstraction."),
            Map.entry("Dart Language Conventions", "Strict null-safety, effective Dart naming, async/await with error handling, immutable data models."),
            Map.entry("TypeScript Strict Typing & Config", "Strict mode enabled, no 'any' types, discriminated unions for state, explicit return types for API contracts."),
            Map.entry("JavaScript (ES6+ / Modern JS)", "ES2024+ syntax, async/await, modular ES modules, immutability patterns, structured error handling."),
            Map.entry("HTML5 & Web Components", "Semantic HTML5 elements (header, main, section, nav, article), accessible ARIA labels, standards-compliant layout."),
            Map.entry("CSS3 & Modern Styling (Tailwind / Flexbox / Grid)", "Mobile-first responsive design, utility-first CSS via Tailwind, flexbox/grid for layouts, accessible contrast ratios.")
    );

    /**
     * Aggregates context and best practices from all active project MCP servers.
     */
    public String aggregateMcpContext(Project project) {
        List<ProjectMcpServer> activeServers = mcpServerRepository.findAllByProjectAndIsActiveTrue(project);
        return aggregateContextForServers(activeServers);
    }

    /**
     * Aggregates MCP context for Backend development (target BACKEND or COMMON).
     */
    public String aggregateBackendMcpContext(Project project) {
        List<ProjectMcpServer> backendServers = mcpServerRepository.findAllByProjectAndIsActiveTrue(project).stream()
                .filter(s -> s.getTarget() == null || s.getTarget() == McpTarget.BACKEND || s.getTarget() == McpTarget.COMMON)
                .toList();
        return aggregateContextForServers(backendServers);
    }

    /**
     * Aggregates MCP context for Frontend development (target FRONTEND or COMMON).
     */
    public String aggregateFrontendMcpContext(Project project) {
        List<ProjectMcpServer> frontendServers = mcpServerRepository.findAllByProjectAndIsActiveTrue(project).stream()
                .filter(s -> s.getTarget() == null || s.getTarget() == McpTarget.FRONTEND || s.getTarget() == McpTarget.COMMON)
                .toList();
        return aggregateContextForServers(frontendServers);
    }

    private String aggregateContextForServers(List<ProjectMcpServer> servers) {
        if (servers.isEmpty()) {
            return "";
        }

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("\n=== MCP ARCHITECTURAL RULES & CONVENTIONS ===\n");

        for (ProjectMcpServer server : servers) {
            if (contextBuilder.length() >= MAX_TOTAL_MCP_CHARS) {
                log.info("Reached maximum total MCP context budget ({} chars). Skipping remaining servers.", MAX_TOTAL_MCP_CHARS);
                break;
            }

            try {
                log.info("Fetching guidelines from MCP server: {} ({}, target={})", server.getName(), server.getServerUrl(), server.getTarget());
                String docResponse = fetchServerDocumentation(server.getServerUrl(), server.getToken());
                String cleanedResponse = sanitizeDocumentation(server.getName(), docResponse);

                contextBuilder.append("\n--- Source: ").append(server.getName()).append(" ---\n");
                contextBuilder.append(cleanedResponse).append("\n");
            } catch (Exception e) {
                log.warn("Could not retrieve MCP context from {}: {}", server.getServerUrl(), e.getMessage());
                contextBuilder.append("\n--- Source: ").append(server.getName()).append(" [Fallback Guidelines] ---\n");
                String curated = CURATED_RULES.getOrDefault(server.getName(),
                        "Follow Clean Architecture, idiomatic patterns, and proper error handling.");
                contextBuilder.append(curated).append("\n");
            }
        }
        contextBuilder.append("============================================\n");
        return contextBuilder.toString();
    }

    private String fetchServerDocumentation(String url, String token) {
        var requestSpec = restClient.get().uri(url);
        if (token != null && !token.isBlank()) {
            requestSpec = requestSpec.header("Authorization", "Bearer " + token.trim());
        }
        return requestSpec.retrieve().body(String.class);
    }

    private String sanitizeDocumentation(String serverName, String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return CURATED_RULES.getOrDefault(serverName, "Standard architectural guidelines.");
        }

        String cleaned = rawContent;
        if (cleaned.contains("<html") || cleaned.contains("<!DOCTYPE") || cleaned.contains("<div") || cleaned.contains("<head")) {
            cleaned = SCRIPT_STYLE_PATTERN.matcher(cleaned).replaceAll(" ");
            cleaned = HTML_TAGS_PATTERN.matcher(cleaned).replaceAll(" ");
            cleaned = MULTI_WHITESPACE_PATTERN.matcher(cleaned).replaceAll(" ");
            cleaned = MULTI_NEWLINE_PATTERN.matcher(cleaned).replaceAll("\n\n").trim();

            if (cleaned.length() < 100 || cleaned.contains("webpack") || cleaned.contains("_next")) {
                String curated = CURATED_RULES.get(serverName);
                if (curated != null) {
                    return curated;
                }
            }
        }

        if (cleaned.length() > MAX_CHARS_PER_SERVER) {
            cleaned = cleaned.substring(0, MAX_CHARS_PER_SERVER) + "... [truncated]";
        }

        return cleaned;
    }
}