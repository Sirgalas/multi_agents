package ru.sergalas.orchestrator.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import ru.sergalas.orchestrator.config.properties.AgentsProperties;
import ru.sergalas.orchestrator.config.properties.AnymodelProperties;
import ru.sergalas.orchestrator.dto.internal.AgentConfig;
import ru.sergalas.orchestrator.entity.enums.StepName;

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
        
        OpenAiApi openAiApi = new OpenAiApi(config.getUrl(), config.getToken());
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(config.getModel())
                .withTemperature(0.7)
                .build();
        
        return new OpenAiChatModel(openAiApi, options);
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
            case WORKER -> agentsProperties.getWorker();
            case TESTER -> agentsProperties.getTester();
            case HELPER -> agentsProperties.getHelper();
        };
    }
}