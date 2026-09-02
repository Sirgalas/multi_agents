package ru.sergalas.orchestrator.config;

import lombok.experimental.UtilityClass;
import ru.sergalas.orchestrator.dto.request.McpServerRequest;
import ru.sergalas.orchestrator.entity.enums.TransportType;

import java.util.List;

@UtilityClass
public class McpPresets {

    public static final List<McpServerRequest> DEFAULT_MCP_SERVERS = List.of(
        new McpServerRequest(
            "Spring Boot Guidelines",
            "https://context7.com/spring-projects/spring-boot",
            TransportType.HTTP,
            true
        ),
        new McpServerRequest(
            "Hibernate ORM Best Practices",
            "https://context7.com/hibernate/hibernate-orm",
            TransportType.HTTP,
            true
        ),
        new McpServerRequest(
            "Java Modern Conventions",
            "https://context7.com/java/openjdk",
            TransportType.HTTP,
            true
        ),
        new McpServerRequest(
            "PostgreSQL Best Practices",
            "https://context7.com/postgres/postgres",
            TransportType.HTTP,
            true
        ),
        new McpServerRequest(
            "React Guidelines",
            "https://context7.com/facebook/react",
            TransportType.HTTP,
            false
        )
    );
}