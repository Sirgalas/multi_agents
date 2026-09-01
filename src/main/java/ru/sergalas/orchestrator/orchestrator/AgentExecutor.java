package ru.sergalas.orchestrator.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecutor {

    private final OpenAiChatModel chatModel;

    public String execute(AgentRole role, String systemPrompt, String userPrompt) {
        log.info("Executing Agent [{}] using model [{}] with temperature [{}]",
                role.getStepName(), role.getModelId(), role.getTemperature());

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(role.getModelId())
                .temperature(role.getTemperature())
                .maxTokens(role.getMaxTokens())
                .build();

        Prompt prompt = new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                options
        );

        ChatResponse response = chatModel.call(prompt);
        if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
            return response.getResult().getOutput().getText();
        }

        throw new IllegalStateException("Empty response received from LLM for role: " + role.getStepName());
    }
}