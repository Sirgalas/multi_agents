package ru.sergalas.orchestrator.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agents")
public class AgentsProperties {
    
    private AgentSettings all = new AgentSettings();
    private AgentSettings interviewer = new AgentSettings();
    private AgentSettings architect = new AgentSettings();
    private AgentSettings worker = new AgentSettings();
    private AgentSettings tester = new AgentSettings();
    private AgentSettings helper = new AgentSettings();
    
    @Data
    public static class AgentSettings {
        private String url;
        private String token;
        private String model;
    }
}