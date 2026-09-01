package ru.sergalas.orchestrator.orchestrator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentExecutor Unit Tests")
class AgentExecutorTest {

    @Mock
    private OpenAiChatModel chatModel;

    @InjectMocks
    private AgentExecutor agentExecutor;

    @Test
    @DisplayName("Should successfully execute prompt and extract textual response")
    void shouldExecutePromptSuccessfully() {
        String expectedOutput = "Architecture specification generated.";
        AssistantMessage assistantMessage = new AssistantMessage(expectedOutput);
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        String result = agentExecutor.execute(AgentRole.ARCHITECT, "System instructions", "User project context");

        assertThat(result).isEqualTo(expectedOutput);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when LLM response is null")
    void shouldThrowExceptionWhenChatResponseIsNull() {
        when(chatModel.call(any(Prompt.class))).thenReturn(null);

        assertThatThrownBy(() -> agentExecutor.execute(AgentRole.WORKER, "sys", "usr"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Empty response received from LLM for role: WORKER");
    }
}