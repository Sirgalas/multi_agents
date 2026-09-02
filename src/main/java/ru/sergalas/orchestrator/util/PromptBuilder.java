package ru.sergalas.orchestrator.util;

import lombok.experimental.UtilityClass;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;

import java.util.List;
import java.util.Map;

@UtilityClass
public class PromptBuilder {

    public static String buildMcpContextSection(List<ProjectMcpServer> mcpServers, Map<String, String> serverContexts) {
        if (mcpServers == null || mcpServers.isEmpty() || serverContexts == null || serverContexts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n\n### MCP CONTEXT & GUIDELINES\n");
        for (ProjectMcpServer server : mcpServers) {
            if (Boolean.TRUE.equals(server.getIsActive())) {
                String ctx = serverContexts.get(server.getServerUrl());
                if (ctx != null && !ctx.isBlank()) {
                    sb.append("#### Context from ").append(server.getName()).append(" (").append(server.getServerUrl()).append("):\n");
                    sb.append(ctx).append("\n\n");
                }
            }
        }
        return sb.toString();
    }

    public static String buildContextFilesSection(List<ProjectContext> contextFiles) {
        if (contextFiles == null || contextFiles.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n\n### PROJECT EXISTING FILES\n");
        for (ProjectContext ctx : contextFiles) {
            sb.append("[FILE: ").append(ctx.getFileName()).append("]\n```\n");
            sb.append(ctx.getFileContent()).append("\n```\n\n");
        }
        return sb.toString();
    }
}