package ru.sergalas.orchestrator.service.agent;

import ru.sergalas.orchestrator.dto.response.ArchitectSpecification;

import java.util.Map;

public interface HelperAgent {
    Map<String, String> generateConfig(
            ArchitectSpecification spec,
            Map<String, String> generatedCode
    );
}