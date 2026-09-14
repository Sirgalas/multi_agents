package ru.sergalas.orchestrator.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.admin.user")
public class AdminProperties {
    private String name = "admin";
    private String password = "admin";
}