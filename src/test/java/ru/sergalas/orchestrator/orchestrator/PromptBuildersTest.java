package ru.sergalas.orchestrator.orchestrator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sergalas.orchestrator.orchestrator.prompt.ArchitectPromptBuilder;
import ru.sergalas.orchestrator.orchestrator.prompt.HelperPromptBuilder;
import ru.sergalas.orchestrator.orchestrator.prompt.TesterPromptBuilder;
import ru.sergalas.orchestrator.orchestrator.prompt.WorkerPromptBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Prompt Builders Edge Cases & Formatting Tests")
class PromptBuildersTest {

    private final ArchitectPromptBuilder architectPromptBuilder = new ArchitectPromptBuilder();
    private final WorkerPromptBuilder workerPromptBuilder = new WorkerPromptBuilder();
    private final TesterPromptBuilder testerPromptBuilder = new TesterPromptBuilder();
    private final HelperPromptBuilder helperPromptBuilder = new HelperPromptBuilder();

    @Test
    @DisplayName("Prompt Builders should provide fallback defaults when input contexts are null or blank")
    void shouldFallbackWhenInputsAreNullOrBlank() {
        assertAll("Fallback prompt verifications",
                () -> {
                    String sys = architectPromptBuilder.buildSystemPrompt(null);
                    String usr = architectPromptBuilder.buildUserPrompt("", null);
                    assertThat(sys).contains("No external MCP rules provided.");
                    assertThat(usr).contains("[No context files]").contains("[None]");
                },
                () -> {
                    String sys = workerPromptBuilder.buildSystemPrompt("");
                    String usr = workerPromptBuilder.buildUserPrompt(null, "   ");
                    assertThat(sys).contains("No external MCP rules provided.");
                    assertThat(usr).contains("[No context files]").contains("[No architect output]");
                },
                () -> {
                    String sys = testerPromptBuilder.buildSystemPrompt(null);
                    String usr = testerPromptBuilder.buildUserPrompt("", "");
                    assertThat(sys).contains("No external MCP rules provided.");
                    assertThat(usr).contains("[No context files]").contains("[No worker output]");
                },
                () -> {
                    String sys = helperPromptBuilder.buildSystemPrompt(null);
                    String usr = helperPromptBuilder.buildUserPrompt("", null, "");
                    assertThat(sys).contains("No external MCP rules provided.");
                    assertThat(usr).contains("[No context files]").contains("[No worker output]").contains("[No tester output]");
                }
        );
    }

    @Test
    @DisplayName("Prompt Builders should inject custom MCP and user context correctly")
    void shouldInjectProvidedContext() {
        String mcp = "RULE 1: Use Java 21 LTS";
        String codeCtx = "class Orchestrator {}";

        String architectSys = architectPromptBuilder.buildSystemPrompt(mcp);
        String architectUsr = architectPromptBuilder.buildUserPrompt(codeCtx, "User answer: yes");

        assertThat(architectSys).contains(mcp);
        assertThat(architectUsr).contains(codeCtx).contains("User answer: yes");
    }
}