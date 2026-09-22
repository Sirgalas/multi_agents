package ru.sergalas.orchestrator.service.agent.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.service.agent.AgentClientFactory;
import ru.sergalas.orchestrator.service.agent.AgentsService;
import ru.sergalas.orchestrator.service.agent.BaseAgentService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import ru.sergalas.orchestrator.entity.AgentPrompt;
import ru.sergalas.orchestrator.service.prompt.AgentPromptService;

import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Order(5)
@RequiredArgsConstructor
public class TesterService extends BaseAgentService implements AgentsService {

    private final AgentClientFactory clientFactory;
    private final ProjectService projectService;
    private final ProjectContextService contextService;
    private final AgentStepRepository agentStepRepository;
    private final AgentPromptService agentPromptService;

    @Override
    public StepName getStepName() {
        return StepName.TESTER;
    }

    @Override
    public Optional<AgentsService> isNeedAgents(Project project) {
        return Optional.of(this);
    }

    @Override
    public boolean isCompleted(Project project) {
        if (project == null) {
            return false;
        }
        boolean hasContextTests = contextService.getContextByProject(project).stream()
                .anyMatch(c -> c.getFileType() == FileType.CONTEXT_CODE && "GENERATED_TESTS.md".equals(c.getFileName())
                        && c.getFileContent() != null && !c.getFileContent().isBlank());

        return hasContextTests && agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.TESTER)
                .map(step -> step.getStatus() == StepStatus.COMPLETED)
                .orElse(false);
    }

    @Override
    @Transactional
    public void work(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        if (isCompleted(project)) {
            log.info("Pipeline Step: TESTER already completed for project {}. Reusing generated tests.", projectId);
            return;
        }
        log.info("Pipeline Step: TESTER - generating unit/integration tests...");
        List<ProjectContext> contexts = contextService.getContextByProject(project);
        StringBuilder codeBuilder = new StringBuilder();
        for (ProjectContext ctx : contexts) {
            if (ctx.getFileType() == FileType.CONTEXT_CODE &&
                ("GENERATED_BACKEND_CODE.md".equals(ctx.getFileName()) || 
                 "GENERATED_FRONTEND_CODE.md".equals(ctx.getFileName()) ||
                 "GENERATED_CODE.md".equals(ctx.getFileName()))) {
                codeBuilder.append("### ").append(ctx.getFileName()).append(":\n")
                           .append(ctx.getFileContent()).append("\n\n");
            }
        }
        String code = codeBuilder.toString().trim();
        if (code.isBlank()) {
            code = contextService.getLatestContextByType(project, FileType.CONTEXT_CODE)
                    .map(ProjectContext::getFileContent).orElse("");
        }

        Optional<AgentPrompt> promptOpt = agentPromptService != null
                ? agentPromptService.getEffectivePrompt(project, StepName.TESTER, false)
                : Optional.empty();

        String prompt;
        if (promptOpt.isPresent()) {
            Map<String, String> vars = Map.of(
                    "code", code
            );
            prompt = agentPromptService.interpolate(promptOpt.get().getPrompt(), vars);
        } else {
            prompt = "Ты — Senior QA Automation Engineer.\n" +
                    "Напиши модульные и интеграционные тесты для сгенерированного исходного кода проекта:\n" +
                    "- Для backend (`backend/`): JUnit 5, Mockito, Spring Boot Test (контроллеры MockMvc, сервисы бизнес-логики, валидация).\n" +
                    "- Для frontend (`frontend/`): тесты компонентов, хуков или API-клиентов (Jest / React Testing Library / Flutter test при наличии frontend в коде).\n\n" +
                    "Форматируй вывод строго в формате:\n" +
                    "[FILE: относительный_путь_к_тесту]\n" +
                    "```{язык}\n" +
                    "код_теста\n" +
                    "```\n\n" +
                    "Исходный код проекта:\n" + code;
        }

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.TESTER);
        String testResponse = executeLlmCall(chatModel, StepName.TESTER, prompt);

        AgentStep step = AgentStep.builder()
                .project(project)
                .stepName(StepName.TESTER)
                .status(StepStatus.COMPLETED)
                .prompt(prompt)
                .response(testResponse)
                .completedAt(LocalDateTime.now())
                .build();
        agentStepRepository.save(step);

        contextService.saveFile(project, "GENERATED_TESTS.md", "/GENERATED_TESTS.md", testResponse, FileType.CONTEXT_CODE, 1);
    }
}