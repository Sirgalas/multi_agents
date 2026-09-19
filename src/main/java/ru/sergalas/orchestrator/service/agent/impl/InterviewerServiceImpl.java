package ru.sergalas.orchestrator.service.agent.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.response.InterviewResponse;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.exception.AgentException;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.service.agent.AgentClientFactory;
import ru.sergalas.orchestrator.service.agent.BaseAgentService;
import ru.sergalas.orchestrator.service.agent.InterviewerService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewerServiceImpl extends BaseAgentService implements InterviewerService {

    private final AgentClientFactory clientFactory;
    private final ProjectService projectService;
    private final ProjectContextService contextService;
    private final AgentStepRepository agentStepRepository;

    @Override
    @Transactional
    public InterviewResponse startInterview(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        
        String systemPrompt = "Ты — Senior IT Interviewer. Твоя цель — расспросить пользователя о проекте '" +
                project.getName() + "' и собрать исчерпывающие требования для ТЗ. Задавай вопросы последовательно и лаконично. Начни с приветствия и первого ключевого вопроса.";

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.INTERVIEWER);
        String initialQuestion = executeLlmCall(chatModel, StepName.INTERVIEWER, systemPrompt);

        AgentStep step = AgentStep.builder()
                .project(project)
                .stepName(StepName.INTERVIEWER)
                .status(StepStatus.IN_PROGRESS)
                .prompt(systemPrompt)
                .response(initialQuestion)
                .build();
        
        AgentStep savedStep = agentStepRepository.save(step);

        return InterviewResponse.builder()
                .stepId(savedStep.getId())
                .question(initialQuestion)
                .isFinalized(false)
                .build();
    }

    @Override
    @Transactional
    public InterviewResponse processAnswer(Long projectId, String userMessage) {
        Project project = projectService.getProjectById(projectId);
        List<AgentStep> previousSteps = agentStepRepository.findAllByProjectOrderByCreatedAtAsc(project);

        StringBuilder history = new StringBuilder();
        for (AgentStep s : previousSteps) {
            if (s.getStepName() == StepName.INTERVIEWER) {
                history.append("Interviewer: ").append(s.getResponse()).append("\n");
                history.append("User: ").append(s.getPrompt()).append("\n");
            }
        }
        history.append("User: ").append(userMessage).append("\n");

        String promptText = "История диалога:\n" + history +
                "\nТы — Senior IT Interviewer. Ответь пользователю и задай следующий уточняющий вопрос. Если информации достаточно, напиши 'FINAL_READY'.";

        try {
            OpenAiChatModel chatModel = clientFactory.createClient(StepName.INTERVIEWER);
            String aiAnswer = executeLlmCall(chatModel, StepName.INTERVIEWER, promptText);

            boolean isFinal = aiAnswer.contains("FINAL_READY");

            AgentStep step = AgentStep.builder()
                    .project(project)
                    .stepName(StepName.INTERVIEWER)
                    .status(isFinal ? StepStatus.COMPLETED : StepStatus.IN_PROGRESS)
                    .prompt(userMessage)
                    .response(aiAnswer)
                    .completedAt(isFinal ? LocalDateTime.now() : null)
                    .build();
            agentStepRepository.save(step);

            String finalTaskMarkdown = null;
            if (isFinal) {
                finalTaskMarkdown = finalizeInterview(projectId);
            }

            return InterviewResponse.builder()
                    .stepId(step.getId())
                    .question(aiAnswer.replace("FINAL_READY", "").trim())
                    .isFinalized(isFinal)
                    .generatedTaskMarkdown(finalTaskMarkdown)
                    .build();
        } catch (Exception e) {
            log.error("Interviewer agent error: {}", e.getMessage(), e);
            throw new AgentException("Failed to process interview step: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public String finalizeInterview(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        List<AgentStep> steps = agentStepRepository.findAllByProjectOrderByCreatedAtAsc(project);

        StringBuilder dialog = new StringBuilder();
        for (AgentStep s : steps) {
            if (s.getStepName() == StepName.INTERVIEWER) {
                dialog.append("Q/Context: ").append(s.getResponse()).append("\n");
                dialog.append("A/User: ").append(s.getPrompt()).append("\n");
            }
        }

        String compilePrompt = "На основе следующих ответов и интервью:\n" + dialog +
                "\nСоставь профессиональное, исчерпывающее Техническое Задание (ТЗ) в формате Markdown для проекта '" + project.getName() + "'. Включи стек: Java 21, Spring Boot 3, архитектуру, REST эндпоинты, сущности БД и критерии приемки.";

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.INTERVIEWER);
        String generatedMarkdown = executeLlmCall(chatModel, StepName.INTERVIEWER, compilePrompt);

        contextService.saveFile(project, "TASK_DRAFT.md", "/TASK_DRAFT.md", generatedMarkdown, FileType.TASK_DRAFT, 1);
        contextService.saveFile(project, "TASK.md", "/TASK.md", generatedMarkdown, FileType.TASK, 1);

        return generatedMarkdown;
    }
}