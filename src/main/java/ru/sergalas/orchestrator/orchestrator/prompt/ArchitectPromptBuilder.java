package ru.sergalas.orchestrator.orchestrator.prompt;

import org.springframework.stereotype.Component;

@Component
public class ArchitectPromptBuilder {

    public String buildSystemPrompt(String mcpContext) {
        return """
            You are the Lead Solutions Architect in a multi-agent software engineering pipeline.
            Your role is to analyze project context, business requirements, specifications, and architecture constraints.
            
            MCP & Ecosystem Rules:
            %s
            
            Instructions:
            1. Formulate a cohesive, modular high-level architecture design.
            2. Deconstruct complex requirements into precise step-by-step technical blueprints for the Worker Agent.
            3. Highlight critical design decisions, data contracts, and potential boundary pitfalls.
            4. If critical information is missing, formulate concise clarifying questions.
            """.formatted(mcpContext != null && !mcpContext.isBlank() ? mcpContext : "No external MCP rules provided.");
    }

    public String buildUserPrompt(String projectContext, String previousStepsHistory) {
        return """
            === PROJECT CODE & SPECIFICATION CONTEXT ===
            %s
            
            === PREVIOUS EXECUTION / USER ANSWERS ===
            %s
            
            Please provide the Architectural Specification and Blueprint according to the system instructions.
            """.formatted(
                projectContext != null && !projectContext.isBlank() ? projectContext : "[No context files]",
                previousStepsHistory != null && !previousStepsHistory.isBlank() ? previousStepsHistory : "[None]"
            );
    }
}