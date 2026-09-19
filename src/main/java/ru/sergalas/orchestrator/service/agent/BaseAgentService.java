package ru.sergalas.orchestrator.service.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.exception.AgentException;

@Slf4j
public abstract class BaseAgentService {

    protected String executeLlmCall(OpenAiChatModel chatModel, StepName stepName, String prompt) {
        int maxRetries = 3;
        long baseDelayMs = 2000;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Executing LLM call for agent {} (attempt {}/{})", stepName, attempt, maxRetries);
                ChatResponse response = chatModel.call(new Prompt(prompt));

                if (response != null && response.getResults() != null && !response.getResults().isEmpty() && response.getResult() != null) {
                    Generation generation = response.getResult();
                    if (generation.getOutput() != null && generation.getOutput().getContent() != null) {
                        String content = generation.getOutput().getContent();
                        if (!content.isBlank()) {
                            return content;
                        }
                    }
                }
                log.warn("Agent {} returned empty response on attempt {}/{}", stepName, attempt, maxRetries);
            } catch (Exception e) {
                lastException = e;
                log.warn("Agent {} attempt {}/{} failed with error: {}", stepName, attempt, maxRetries, e.getMessage());
            }

            if (attempt < maxRetries) {
                try {
                    long delay = baseDelayMs * (long) Math.pow(2, attempt - 1);
                    log.info("Retrying agent {} in {} ms...", stepName, delay);
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AgentException("Agent execution interrupted for: " + stepName, ie);
                }
            }
        }

        String errorMsg = "Agent " + stepName + " failed to produce a valid response after " + maxRetries + " attempts.";
        if (lastException != null) {
            errorMsg += " Last error: " + lastException.getMessage();
        }
        throw new AgentException(errorMsg, lastException);
    }
}
