package ru.sergalas.orchestrator.service.agent.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.service.agent.AgentClientFactory;
import ru.sergalas.orchestrator.service.agent.BaseAgentService;
import ru.sergalas.orchestrator.service.agent.HelperService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import ru.sergalas.orchestrator.entity.AgentPrompt;
import ru.sergalas.orchestrator.service.prompt.AgentPromptService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HelperServiceImpl extends BaseAgentService implements HelperService {

    private final AgentClientFactory clientFactory;
    private final ProjectService projectService;
    private final ProjectContextService contextService;
    private final AgentStepRepository agentStepRepository;
    private final AgentPromptService agentPromptService;

    @Override
    @Transactional
    public void generateInfrastructure(Long projectId) {
        Project project = projectService.getProjectById(projectId);

        Optional<AgentPrompt> promptOpt = agentPromptService != null
                ? agentPromptService.getEffectivePrompt(project, StepName.HELPER, false)
                : Optional.empty();

        String prompt;
        if (promptOpt.isPresent()) {
            Map<String, String> vars = Map.of(
                    "projectName", project.getName()
            );
            prompt = agentPromptService.interpolate(promptOpt.get().getPrompt(), vars);
        } else {
            prompt = "Ты — DevOps Engineer. Сгенерируй файлы окружения и документацию для проекта '" + project.getName() + "':\n" +
                    "1. Корневой docker-compose.yml (сервисы: postgres, backend, frontend при необходимости, pgadmin)\n" +
                    "2. backend/Dockerfile (многоэтапная сборка Java 21 / Spring Boot 3.4)\n" +
                    "3. frontend/Dockerfile (или скрипты сборки/запуска frontend/mobile)\n" +
                    "4. Корневой .gitignore\n" +
                    "5. Корневой README.md с подробным руководством по локальному запуску и backend, и frontend.\n" +
                    "Каждый файл оформляй строго в формате:\n[FILE: относительный_путь]\n```формат\nконтент\n```";
        }

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.HELPER);
        String infraResponse = executeLlmCall(chatModel, StepName.HELPER, prompt);

        AgentStep step = AgentStep.builder()
                .project(project)
                .stepName(StepName.HELPER)
                .status(StepStatus.COMPLETED)
                .prompt(prompt)
                .response(infraResponse)
                .completedAt(LocalDateTime.now())
                .build();
        agentStepRepository.save(step);

        contextService.saveFile(project, "GENERATED_INFRA.md", "/GENERATED_INFRA.md", infraResponse, FileType.CONTEXT_CODE, 1);
    }
}