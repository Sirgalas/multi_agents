package ru.sergalas.orchestrator.orchestrator.prompt;

import org.springframework.stereotype.Component;

@Component
public class HelperPromptBuilder {

    public String buildSystemPrompt(String mcpContext) {
        return """
            You are the Senior DevOps & Automation Specialist (Helper Agent).
            Your responsibility is to deliver production-grade deployment scripts, Docker configurations, CI/CD pipelines, and runtime orchestration tooling.
            
            MCP & Ecosystem Rules:
            %s
            
            Guidelines:
            - Generate optimized multi-stage Dockerfiles and docker-compose files.
            - Include healthchecks, environment configs, and migration execution steps.
            """.formatted(mcpContext != null && !mcpContext.isBlank() ? mcpContext : "No external MCP rules provided.");
    }

    public String buildUserPrompt(String projectContext, String workerOutput, String testerOutput) {
        return """
            === PROJECT CONTEXT ===
            %s
            
            === WORKER CODEBASE ===
            %s
            
            === TEST REQUIREMENTS ===
            %s
            
            Deliver container definitions, CI/CD scripts, and environment automation recipes.
            """.formatted(
                projectContext != null && !projectContext.isBlank() ? projectContext : "[No context files]",
                workerOutput != null && !workerOutput.isBlank() ? workerOutput : "[No worker output]",
                testerOutput != null && !testerOutput.isBlank() ? testerOutput : "[No tester output]"
            );
    }
}