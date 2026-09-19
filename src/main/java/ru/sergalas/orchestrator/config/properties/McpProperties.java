package ru.sergalas.orchestrator.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import ru.sergalas.orchestrator.entity.enums.McpTarget;
import ru.sergalas.orchestrator.entity.enums.TransportType;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {
    private List<DefaultMcpServerConfig> defaultServers = new ArrayList<>();

    @Data
    public static class DefaultMcpServerConfig {
        private String name;
        private String url;
        private TransportType transport = TransportType.SSE;
        private boolean defaultSelected = false;
        private McpTarget target = McpTarget.COMMON;
    }
}