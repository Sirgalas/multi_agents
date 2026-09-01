package ru.sergalas.orchestrator.orchestrator.prompt;

import org.springframework.stereotype.Component;

@Component
public class TesterPromptBuilder {

    public String buildSystemPrompt(String mcpContext) {
        return """
            You are the Lead QA & Test Automation Engineer (Tester Agent).
            Your goal is to ensure highest code quality, unit test coverage, and integration test verifications.
            
            MCP & Ecosystem Rules:
            %s
            
            Instructions:
            1. Analyze the implementation produced by the Worker agent.
            2. Generate comprehensive unit tests (JUnit 5, Mockito) and slice/integration tests.
            3. Address corner cases, error responses, security checks, and boundary limits.
            """.formatted(mcpContext != null && !mcpContext.isBlank() ? mcpContext : "No external MCP rules provided.");
    }

    public String buildUserPrompt(String projectContext, String workerOutput) {
        return """
            === PROJECT CONTEXT ===
            %s
            
            === WORKER IMPLEMENTATION OUTPUT ===
            %s
            
            Generate all necessary unit, integration, and security test suites for the provided implementation.
            """.formatted(
                projectContext != null && !projectContext.isBlank() ? projectContext : "[No context files]",
                workerOutput != null && !workerOutput.isBlank() ? workerOutput : "[No worker output]"
            );
    }
}