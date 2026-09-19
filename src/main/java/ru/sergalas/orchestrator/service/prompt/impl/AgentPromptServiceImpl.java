package ru.sergalas.orchestrator.service.prompt.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.request.CreateAgentPromptRequest;
import ru.sergalas.orchestrator.dto.request.UpdateAgentPromptRequest;
import ru.sergalas.orchestrator.dto.response.AgentPromptResponse;
import ru.sergalas.orchestrator.entity.AgentPrompt;
import ru.sergalas.orchestrator.entity.McpServer;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectAgentPrompt;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.exception.ResourceNotFoundException;
import ru.sergalas.orchestrator.repository.AgentPromptRepository;
import ru.sergalas.orchestrator.repository.McpServerRepository;
import ru.sergalas.orchestrator.repository.ProjectAgentPromptRepository;
import ru.sergalas.orchestrator.service.prompt.AgentPromptService;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentPromptServiceImpl implements AgentPromptService {

    private final AgentPromptRepository promptRepository;
    private final McpServerRepository mcpServerRepository;
    private final ProjectAgentPromptRepository projectAgentPromptRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AgentPrompt> getAllPrompts() {
        return promptRepository.findAllByOrderByStepNameAscNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentPrompt> getPromptsByStep(StepName stepName) {
        return promptRepository.findAllByStepNameOrderByIsDefaultDescNameAsc(stepName);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentPrompt getPromptById(Long id) {
        return promptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent prompt not found with id: " + id));
    }

    @Override
    @Transactional
    public AgentPrompt createPrompt(CreateAgentPromptRequest request) {
        Set<McpServer> servers = resolveMcpServers(request.getMcpServerIds());
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            resetExistingDefault(request.getStepName(), request.getIsFinal());
        }

        AgentPrompt prompt = AgentPrompt.builder()
                .name(request.getName().trim())
                .stepName(request.getStepName())
                .prompt(request.getPrompt().trim())
                .isFinal(Boolean.TRUE.equals(request.getIsFinal()))
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .description(request.getDescription())
                .mcpServers(servers)
                .build();

        return promptRepository.save(prompt);
    }

    @Override
    @Transactional
    public AgentPrompt updatePrompt(Long id, UpdateAgentPromptRequest request) {
        AgentPrompt prompt = getPromptById(id);
        Set<McpServer> servers = resolveMcpServers(request.getMcpServerIds());

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(prompt.getIsDefault())) {
            resetExistingDefault(request.getStepName(), request.getIsFinal());
        }

        prompt.setName(request.getName().trim());
        prompt.setStepName(request.getStepName());
        prompt.setPrompt(request.getPrompt().trim());
        prompt.setIsFinal(Boolean.TRUE.equals(request.getIsFinal()));
        prompt.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        prompt.setDescription(request.getDescription());
        prompt.setMcpServers(servers);

        return promptRepository.save(prompt);
    }

    @Override
    @Transactional
    public void deletePrompt(Long id) {
        AgentPrompt prompt = getPromptById(id);
        promptRepository.delete(prompt);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AgentPrompt> getEffectivePrompt(Project project, StepName stepName, boolean isFinal) {
        if (project != null && project.getId() != null) {
            Optional<ProjectAgentPrompt> papOpt = projectAgentPromptRepository.findByProjectAndStepName(project, stepName);
            if (papOpt.isPresent()) {
                AgentPrompt assignedPrompt = papOpt.get().getAgentPrompt();
                if (stepName == StepName.ARCHITECT) {
                    if (isFinal && Boolean.TRUE.equals(assignedPrompt.getIsFinal())) {
                        return Optional.of(assignedPrompt);
                    }
                    if (!isFinal && !Boolean.TRUE.equals(assignedPrompt.getIsFinal())) {
                        return Optional.of(assignedPrompt);
                    }
                    // For architect, if isFinal round is requested and assigned is non-final, find default final
                    return promptRepository.findFirstByStepNameAndIsFinalAndIsDefaultTrue(stepName, isFinal);
                }
                return Optional.of(assignedPrompt);
            }
        }

        if (stepName == StepName.ARCHITECT) {
            return promptRepository.findFirstByStepNameAndIsFinalAndIsDefaultTrue(stepName, isFinal)
                    .or(() -> promptRepository.findAllByStepNameOrderByIsDefaultDescNameAsc(stepName).stream()
                            .filter(p -> Boolean.TRUE.equals(p.getIsFinal()) == isFinal)
                            .findFirst());
        }

        return promptRepository.findFirstByStepNameAndIsDefaultTrue(stepName)
                .or(() -> promptRepository.findAllByStepNameOrderByIsDefaultDescNameAsc(stepName).stream().findFirst());
    }

    @Override
    public String interpolate(String template, Map<String, String> variables) {
        if (template == null) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String val = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, val);
        }
        return result;
    }

    @Override
    public AgentPromptResponse toResponse(AgentPrompt prompt) {
        return AgentPromptResponse.builder()
                .id(prompt.getId())
                .name(prompt.getName())
                .stepName(prompt.getStepName())
                .prompt(prompt.getPrompt())
                .isFinal(prompt.getIsFinal())
                .isDefault(prompt.getIsDefault())
                .description(prompt.getDescription())
                .mcpServers(new ArrayList<>(prompt.getMcpServers()))
                .createdAt(prompt.getCreatedAt())
                .updatedAt(prompt.getUpdatedAt())
                .build();
    }

    private Set<McpServer> resolveMcpServers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(mcpServerRepository.findAllById(ids));
    }

    private void resetExistingDefault(StepName stepName, Boolean isFinal) {
        if (stepName == StepName.ARCHITECT) {
            promptRepository.findFirstByStepNameAndIsFinalAndIsDefaultTrue(stepName, Boolean.TRUE.equals(isFinal))
                    .ifPresent(p -> {
                        p.setIsDefault(false);
                        promptRepository.save(p);
                    });
        } else {
            promptRepository.findFirstByStepNameAndIsDefaultTrue(stepName)
                    .ifPresent(p -> {
                        p.setIsDefault(false);
                        promptRepository.save(p);
                    });
        }
    }
}
