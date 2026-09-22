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
import ru.sergalas.orchestrator.service.agent.BaseAgentService;
import ru.sergalas.orchestrator.service.mcp.McpClientService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import ru.sergalas.orchestrator.entity.AgentPrompt;
import ru.sergalas.orchestrator.service.prompt.AgentPromptService;

import org.springframework.core.annotation.Order;
import ru.sergalas.orchestrator.entity.enums.ProjectType;
import ru.sergalas.orchestrator.service.agent.AgentsService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Order(3)
@RequiredArgsConstructor
public class BackendWorkerService extends BaseAgentService implements AgentsService {

    private final AgentClientFactory clientFactory;
    private final ProjectService projectService;
    private final ProjectContextService contextService;
    private final AgentStepRepository agentStepRepository;
    private final McpClientService mcpClientService;
    private final AgentPromptService agentPromptService;

    @Override
    public StepName getStepName() {
        return StepName.BACKEND_DEVELOPER;
    }

    @Override
    public Optional<AgentsService> isNeedAgents(Project project) {
        if (project != null && project.getType() != ProjectType.FRONTEND_ONLY) {
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public Optional<AgentsService> isNeedAgents(String stepName) {
        if (stepName == null) {
            return Optional.empty();
        }
        String s = stepName.trim();
        if (StepName.BACKEND_DEVELOPER.name().equalsIgnoreCase(s) || "WORKER".equalsIgnoreCase(s)) {
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public boolean isCompleted(Project project) {
        if (project == null) {
            return false;
        }
        boolean hasContext = contextService.getContextByProject(project).stream()
                .anyMatch(c -> c.getFileType() == FileType.CONTEXT_CODE
                        && ("GENERATED_BACKEND_CODE.md".equals(c.getFileName()) || "GENERATED_CODE.md".equals(c.getFileName()))
                        && c.getFileContent() != null && !c.getFileContent().isBlank());

        boolean hasStep = agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.BACKEND_DEVELOPER)
                .map(step -> step.getStatus() == StepStatus.COMPLETED)
                .orElse(false)
                || agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.WORKER)
                .map(step -> step.getStatus() == StepStatus.COMPLETED)
                .orElse(false);

        return hasContext && hasStep;
    }

    @Override
    @Transactional
    public void work(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        if (isCompleted(project)) {
            log.info("Pipeline Step: {} already completed for project {}. Reusing code.", getStepName(), projectId);
            return;
        }
        log.info("Pipeline Step: {} - generating source code...", getStepName());
        String task = contextService.getLatestContextByType(project, FileType.TASK)
                .map(ProjectContext::getFileContent).orElse("");
        String spec = contextService.getLatestContextByType(project, FileType.SPEC)
                .map(ProjectContext::getFileContent).orElse("");
        String backendSpec = contextService.getContextByProject(project).stream()
                .filter(c -> "BACKEND_SPEC.md".equals(c.getFileName()))
                .findFirst()
                .map(ProjectContext::getFileContent).orElse("");
        String mcp = mcpClientService.aggregateBackendMcpContext(project);

        Optional<AgentPrompt> promptOpt = agentPromptService != null
                ? agentPromptService.getEffectivePrompt(project, StepName.BACKEND_DEVELOPER, false)
                : Optional.empty();

        String prompt;
        if (promptOpt.isPresent()) {
            Map<String, String> vars = Map.of(
                    "mcpRules", mcp,
                    "taskContent", task,
                    "backendSpec", backendSpec,
                    "archSpec", spec
            );
            prompt = agentPromptService.interpolate(promptOpt.get().getPrompt(), vars);
        } else {
            prompt = "Ты — Senior Backend Engineer (Java 21, Spring Boot 3.4, PostgreSQL, Liquibase, Spring Security JWT).\n" +
                "Сгенерируй полный, рабочий и компилируемый серверный код проекта строго на основе ТЗ и Архитектурной спецификации.\n\n" +
                "ТРЕБОВАНИЯ К БЭКЕНДУ (СТРОГО В ПАПКУ `backend/`):\n" +
                "Все файлы бэкенда должны размещаться строго по пути `backend/...`:\n" +
                "1. `backend/pom.xml` или `backend/build.gradle` (Spring Boot 3.4, Java 21, Spring Data JPA, PostgreSQL, Liquibase, Spring Security, JJWT / OAuth2, Lombok, Validation, Actuator).\n" +
                "2. `backend/src/main/resources/application.yml` (конфигурация DataSource, JPA Hibernate, Liquibase, JWT-секреты и сроки жизни токенов, серверные порты).\n" +
                "3. `backend/src/main/resources/db/changelog/db.changelog-master.xml` (или yaml) и changelog-файлы миграций со всеми таблицами, внешними ключами и индексами.\n" +
                "4. Сущности JPA (@Entity, @Table, @Id, связи @ManyToOne, @OneToMany, Enums) в пакете entity.\n" +
                "5. Spring Data JPA Репозитории (@Repository) в пакете repository.\n" +
                "6. DTO (Request/Response) с аннотациями Jakarta Validation (@NotBlank, @NotNull, @Size, @Email) в пакете dto.\n" +
                "7. Сервисы бизнес-логики (@Service, @Transactional) и интерфейсы в пакете service.\n" +
                "8. REST Контроллеры (@RestController, @RequestMapping(\"/api/v1/...\")) с четкими эндпоинтами, HTTP-методами, статус-кодами и валидацией (@Valid) в пакете controller.\n" +
                "9. Безопасность (Spring Security): SecurityConfig, SecurityFilterChain, JwtAuthenticationFilter, JwtTokenProvider, UserDetailsService, PasswordEncoder.\n" +
                "10. Глобальная обработка исключений (@RestControllerAdvice) с единой структурой ErrorResponse.\n\n" +
                "ВАЖНО: Пиши полноценный рабочий код без сокращений и без плейсхолдеров вроде '// TODO: implement later'.\n\n" +
                "ФОРМАТ ВЫВОДА (СТРОГО):\n" +
                "Каждый файл оборачивай строго в формат:\n" +
                "[FILE: backend/относительный_путь_к_файлу]\n" +
                "```{язык}\n" +
                "код_файла\n" +
                "```\n\n" +
                "Архитектурные правила и MCP:\n" + mcp + "\n\n" +
                "ТЗ:\n" + task + "\n\n" +
                "Спецификация Бэкенда (API & БД):\n" + backendSpec + "\n\n" +
                "Общая Спецификация:\n" + spec;
        }

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.BACKEND_DEVELOPER);
        String codeResponse = executeLlmCall(chatModel, StepName.BACKEND_DEVELOPER, prompt);

        AgentStep step = AgentStep.builder()
                .project(project)
                .stepName(StepName.BACKEND_DEVELOPER)
                .status(StepStatus.COMPLETED)
                .prompt(prompt)
                .response(codeResponse)
                .completedAt(LocalDateTime.now())
                .build();
        agentStepRepository.save(step);

        contextService.saveFile(project, "GENERATED_BACKEND_CODE.md", "/GENERATED_BACKEND_CODE.md", codeResponse, FileType.CONTEXT_CODE, 1);
    }
}
