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

    private static final String DEFAULT_GENERIC_FALLBACK_RULE =
            "Follow Clean Architecture, idiomatic patterns, and proper error handling.";

    /**
     * Builds a map of architectural rules dynamically from the project's MCP servers,
     * using the server's description configured in the database.
     */
    public Map<String, String> buildRulesMap(List<ProjectMcpServer> servers) {
        Map<String, String> rulesMap = new java.util.HashMap<>();
        if (servers != null) {
            for (ProjectMcpServer server : servers) {
                if (server.getName() != null) {
                    String rule = (server.getDescription() != null && !server.getDescription().isBlank())
                            ? server.getDescription().trim()
                            : DEFAULT_GENERIC_FALLBACK_RULE;
                    rulesMap.put(server.getName(), rule);
                }
            }
        }
        return rulesMap;
    }

    /**
     * Returns the merged rules map for all active servers of a project.
     */
    public Map<String, String> getRulesMap(Project project) {
        List<ProjectMcpServer> activeServers = mcpServerRepository.findAllByProjectAndIsActiveTrue(project);
        return buildRulesMap(activeServers);
    }

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

        Map<String, String> rulesMap = buildRulesMap(servers);

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
                String cleanedResponse = sanitizeDocumentation(server.getName(), docResponse, rulesMap);

                contextBuilder.append("\n--- Source: ").append(server.getName()).append(" ---\n");
                contextBuilder.append(cleanedResponse).append("\n");
            } catch (Exception e) {
                log.warn("Could not retrieve MCP context from {}: {}", server.getServerUrl(), e.getMessage());
                contextBuilder.append("\n--- Source: ").append(server.getName()).append(" [Fallback Guidelines] ---\n");
                String curated = rulesMap.getOrDefault(server.getName(),
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

    private String sanitizeDocumentation(String serverName, String rawContent, Map<String, String> rulesMap) {
        if (rawContent == null || rawContent.isBlank()) {
            return rulesMap.getOrDefault(serverName, DEFAULT_GENERIC_FALLBACK_RULE);
        }

        String cleaned = rawContent;
        if (cleaned.contains("<html") || cleaned.contains("<!DOCTYPE") || cleaned.contains("<div") || cleaned.contains("<head")) {
            cleaned = SCRIPT_STYLE_PATTERN.matcher(cleaned).replaceAll(" ");
            cleaned = HTML_TAGS_PATTERN.matcher(cleaned).replaceAll(" ");
            cleaned = MULTI_WHITESPACE_PATTERN.matcher(cleaned).replaceAll(" ");
            cleaned = MULTI_NEWLINE_PATTERN.matcher(cleaned).replaceAll("\n\n").trim();

            if (cleaned.length() < 100 || cleaned.contains("webpack") || cleaned.contains("_next")) {
                String curated = rulesMap.get(serverName);
                if (curated != null) {
                    return curated;
                }
                return DEFAULT_GENERIC_FALLBACK_RULE;
            }
        }

        if (cleaned.length() > MAX_CHARS_PER_SERVER) {
            cleaned = cleaned.substring(0, MAX_CHARS_PER_SERVER) + "... [truncated]";
        }

        return cleaned;
    }

    private String sanitizeDocumentation(String serverName, String rawContent) {
        return sanitizeDocumentation(serverName, rawContent, Map.of());
    }
}