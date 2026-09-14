package ru.sergalas.orchestrator.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "anymodel.api")
public class AnymodelProperties {
    private String key;
    private Base base = new Base();
    
    @Data
    public static class Base {
        private String url;
    }
}