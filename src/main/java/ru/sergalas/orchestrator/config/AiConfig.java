package ru.sergalas.orchestrator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ru.sergalas.orchestrator.entity.enums.StepName;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "orchestrator")
@Getter
@Setter
public class AiConfig {

    private Map<String, String> models = new HashMap<>();

    public String getModelForStep(StepName stepName) {
        if (models == null || models.isEmpty()) {
            return switch (stepName) {
                case ARCHITECT -> "cc/claude-sonnet-4-6";
                case WORKER -> "ag/gemini-3.7-flash-high";
                case TESTER -> "ag/gemini-3.7-flash-high";
                case HELPER -> "ag/gemini-3.6-flash-high";
            };
        }
        return models.getOrDefault(stepName.name().toLowerCase(), "cc/claude-sonnet-4-6");
    }
}