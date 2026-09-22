package ru.sergalas.orchestrator.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import ru.sergalas.orchestrator.config.properties.AgentsProperties;
import ru.sergalas.orchestrator.config.properties.AnymodelProperties;
import ru.sergalas.orchestrator.dto.internal.AgentConfig;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.exception.AgentException;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentClientFactory {
    
    private final AgentsProperties agentsProperties;
    private final AnymodelProperties anymodelProperties;
    
    public OpenAiChatModel createClient(StepName stepName) {
        AgentConfig config = resolveConfig(stepName);
        
        log.info("Creating agent client for {} with URL: {}, Model: {}", 
                 stepName, config.getUrl(), config.getModel());
        
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(60));
        requestFactory.setReadTimeout(Duration.ofSeconds(300));

        RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);
        WebClient.Builder webClientBuilder = WebClient.builder();
        OpenAiApi openAiApi = new OpenAiApi(config.getUrl(), config.getToken(), restClientBuilder, webClientBuilder);
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(config.getModel())
                .temperature(0.7)
                .build();
        
        return new OpenAiChatModel(openAiApi, options);
    }

    public String callChatModel(OpenAiChatModel chatModel, StepName stepName, String prompt) {
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
    
    private AgentConfig resolveConfig(StepName stepName) {
        AgentsProperties.AgentSettings agentSettings = getAgentSettings(stepName);
        AgentsProperties.AgentSettings allSettings = agentsProperties.getAll();
        
        String url = resolveParameter(
                agentSettings.getUrl(),
                allSettings.getUrl(),
                anymodelProperties.getBase().getUrl()
        );
        
        String token = resolveParameter(
                agentSettings.getToken(),
                allSettings.getToken(),
                anymodelProperties.getKey()
        );
        
        String model = agentSettings.getModel();
        if (!isNotEmpty(model)) {
            model = "ag/gemini-3.7-flash-high";
        }
        
        return AgentConfig.builder()
                .url(url)
                .token(token)
                .model(model)
                .build();
    }
    
    private String resolveParameter(String personal, String common, String fallback) {
        if (isNotEmpty(personal)) {
            return personal;
        }
        if (isNotEmpty(common)) {
            return common;
        }
        return fallback;
    }
    
    private boolean isNotEmpty(String value) {
        return value != null && !value.isBlank();
    }
    
    private AgentsProperties.AgentSettings getAgentSettings(StepName stepName) {
        return switch (stepName) {
            case INTERVIEWER -> agentsProperties.getInterviewer();
            case ARCHITECT -> agentsProperties.getArchitect();
            case BACKEND_ANALYST -> (agentsProperties.getBackendAnalyst() != null && isNotEmpty(agentsProperties.getBackendAnalyst().getModel()))
                    ? agentsProperties.getBackendAnalyst()
                    : agentsProperties.getArchitect();
            case FRONTEND_ANALYST -> (agentsProperties.getFrontendAnalyst() != null && isNotEmpty(agentsProperties.getFrontendAnalyst().getModel()))
                    ? agentsProperties.getFrontendAnalyst()
                    : agentsProperties.getArchitect();
            case BACKEND_DEVELOPER -> (agentsProperties.getBackendDeveloper() != null && isNotEmpty(agentsProperties.getBackendDeveloper().getModel()))
                    ? agentsProperties.getBackendDeveloper()
                    : agentsProperties.getAll();
            case FRONTEND_DEVELOPER -> (agentsProperties.getFrontendDeveloper() != null && isNotEmpty(agentsProperties.getFrontendDeveloper().getModel()))
                    ? agentsProperties.getFrontendDeveloper()
                    : agentsProperties.getAll();
            case TESTER -> agentsProperties.getTester();
            case HELPER -> agentsProperties.getHelper();
            case ARCHIVER -> agentsProperties.getAll();
        };
    }
}