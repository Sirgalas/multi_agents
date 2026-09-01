package ru.sergalas.orchestrator.orchestrator.prompt;

import org.springframework.stereotype.Component;

@Component
public class WorkerPromptBuilder {

    public String buildSystemPrompt(String mcpContext) {
        return """
            You are the Senior Implementation Engineer (Worker Agent).
            Your duty is to produce complete, idiomatic, fully functional source code strictly complying with the Architectural Blueprint.
            
            MCP & Ecosystem Rules:
            %s
            
            Guidelines:
            - Write clean, robust, production-ready code with complete package statements and annotations.
            - Adhere strictly to the requested frameworks and versions.
            - Ensure code matches all entity definitions, schemas, and contract rules.
            """.formatted(mcpContext != null && !mcpContext.isBlank() ? mcpContext : "No external MCP rules provided.");
    }

    public String buildUserPrompt(String projectContext, String architectOutput) {
        return """
            === PROJECT CONTEXT ===
            %s
            
            === ARCHITECTURAL BLUEPRINT ===
            %s
            
            Implement all components based strictly on the Architect's instructions. Output full code listings.
            """.formatted(
                projectContext != null && !projectContext.isBlank() ? projectContext : "[No context files]",
                architectOutput != null && !architectOutput.isBlank() ? architectOutput : "[No architect output]"
            );
    }
}