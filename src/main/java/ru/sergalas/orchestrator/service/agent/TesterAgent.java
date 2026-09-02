package ru.sergalas.orchestrator.service.agent;

import ru.sergalas.orchestrator.dto.response.ArchitectSpecification;

import java.util.Map;

public interface TesterAgent {
    Map<String, String> generateTests(
            Map<String, String> generatedCode,
            ArchitectSpecification spec
    );
}