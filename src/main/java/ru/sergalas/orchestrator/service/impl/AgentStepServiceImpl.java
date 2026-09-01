package ru.sergalas.orchestrator.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.response.AgentStepResponse;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.service.AgentStepService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgentStepServiceImpl implements AgentStepService {

    private final AgentStepRepository agentStepRepository;

    @Override
    @Transactional
    public AgentStep createStep(Project project, StepName stepName, String prompt, String modelUsed) {
        AgentStep step = AgentStep.builder()
                .project(project)
                .stepName(stepName)
                .stepStatus(StepStatus.IN_PROGRESS)
                .prompt(prompt)
                .modelUsed(modelUsed)
                .build();
        return agentStepRepository.save(step);
    }

    @Override
    @Transactional
    public AgentStep updateStepStatus(Long stepId, StepStatus status, String response) {
        AgentStep step = getById(stepId);
        step.setStepStatus(status);
        step.setResponse(response);
        if (status == StepStatus.COMPLETED || status == StepStatus.FAILED) {
            step.setCompletedAt(LocalDateTime.now());
        }
        return agentStepRepository.save(step);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentStepResponse> getStepsByProject(Long projectId) {
        return agentStepRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AgentStep> getLatestStep(Long projectId, StepName stepName) {
        return agentStepRepository.findTopByProjectIdAndStepNameOrderByCreatedAtDesc(projectId, stepName);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentStep getById(Long stepId) {
        return agentStepRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Agent step not found: " + stepId));
    }

    private AgentStepResponse mapToResponse(AgentStep step) {
        return new AgentStepResponse(
                step.getId(),
                step.getStepName(),
                step.getStepStatus(),
                step.getPrompt(),
                step.getResponse(),
                step.getModelUsed(),
                step.getCreatedAt(),
                step.getCompletedAt()
        );
    }
}