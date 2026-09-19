package ru.sergalas.orchestrator.service.prompt;

import ru.sergalas.orchestrator.dto.request.CreateAgentPromptRequest;
import ru.sergalas.orchestrator.dto.request.UpdateAgentPromptRequest;
import ru.sergalas.orchestrator.dto.response.AgentPromptResponse;
import ru.sergalas.orchestrator.entity.AgentPrompt;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.enums.StepName;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AgentPromptService {
    List<AgentPrompt> getAllPrompts();
    List<AgentPrompt> getPromptsByStep(StepName stepName);
    AgentPrompt getPromptById(Long id);
    AgentPrompt createPrompt(CreateAgentPromptRequest request);
    AgentPrompt updatePrompt(Long id, UpdateAgentPromptRequest request);
    void deletePrompt(Long id);
    
    Optional<AgentPrompt> getEffectivePrompt(Project project, StepName stepName, boolean isFinal);
    String interpolate(String template, Map<String, String> variables);
    AgentPromptResponse toResponse(AgentPrompt prompt);
}
