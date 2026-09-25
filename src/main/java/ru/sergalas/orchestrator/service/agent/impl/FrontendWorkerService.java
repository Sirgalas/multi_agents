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

import ru.sergalas.orchestrator.entity.enums.ProjectType;
import ru.sergalas.orchestrator.service.agent.AgentsService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FrontendWorkerService extends BaseAgentService implements AgentsService {

    private final AgentClientFactory clientFactory;
    private final ProjectService projectService;
    private final ProjectContextService contextService;
    private final AgentStepRepository agentStepRepository;
    private final McpClientService mcpClientService;
    private final AgentPromptService agentPromptService;

    @Override
    public StepName getStepName() {
        return StepName.FRONTEND_DEVELOPER;
    }

    @Override
    public Optional<AgentsService> isNeedAgents(Project project) {
        if (project != null && project.getType() != ProjectType.BACKEND_ONLY) {
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
        if (StepName.FRONTEND_DEVELOPER.name().equalsIgnoreCase(s)) {
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
                        && ("GENERATED_FRONTEND_CODE.md".equals(c.getFileName()) || "GENERATED_CODE.md".equals(c.getFileName()))
                        && c.getFileContent() != null && !c.getFileContent().isBlank());

        boolean hasStep = agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.FRONTEND_DEVELOPER)
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
        String mcp = mcpClientService.aggregateFrontendMcpContext(project);

        String backendCode = contextService.getContextByProject(project).stream()
                .filter(c -> "GENERATED_BACKEND_CODE.md".equals(c.getFileName()) || "GENERATED_CODE.md".equals(c.getFileName()))
                .filter(c -> c.getFileContent() != null && !c.getFileContent().isBlank())
                .map(ProjectContext::getFileContent)
                .findFirst()
                .orElse("");

        String frontendSpec = contextService.getContextByProject(project).stream()
                .filter(c -> "FRONTEND_SPEC.md".equals(c.getFileName()))
                .filter(c -> c.getFileContent() != null && !c.getFileContent().isBlank())
                .map(ProjectContext::getFileContent)
                .findFirst()
                .orElse("");

        String designTokens = contextService.getContextByProject(project).stream()
                .filter(c -> "DESIGN_TOKENS.json".equals(c.getFileName()) || "DESIGN_SPEC.md".equals(c.getFileName()))
                .filter(c -> c.getFileContent() != null && !c.getFileContent().isBlank())
                .map(ProjectContext::getFileContent)
                .findFirst()
                .orElse("");

        Optional<AgentPrompt> promptOpt = agentPromptService != null
                ? agentPromptService.getEffectivePrompt(project, StepName.FRONTEND_DEVELOPER, false)
                : Optional.empty();

        String prompt;
        if (promptOpt.isPresent()) {
            Map<String, String> vars = Map.of(
                    "mcpRules", mcp,
                    "taskContent", task,
                    "frontendSpec", frontendSpec,
                    "archSpec", spec,
                    "backendCode", backendCode,
                    "designTokens", designTokens
            );
            prompt = agentPromptService.interpolate(promptOpt.get().getPrompt(), vars);
        } else {
            prompt = "Ты — Senior Frontend / Mobile Engineer (React / React Native / TypeScript / Flutter).\n" +
                "Сгенерируй полноценный, рабочий клиентский код приложения строго в директорию `frontend/` на основе ТЗ, Архитектурной спецификации, спецификации фронтенда, дизайн-токенов и готового бэкенда.\n\n" +
                "ТРЕБОВАНИЯ К ФРОНТЕНДУ (СТРОГО В ПАПКУ `frontend/`):\n" +
                "Все файлы клиентской части должны размещаться строго по пути `frontend/...`:\n" +
                "1. Конфигурация проекта (`frontend/package.json`, `frontend/tsconfig.json`, `frontend/tailwind.config.js` или `frontend/pubspec.yaml`).\n" +
                "2. Точка входа в приложение (`frontend/App.tsx`, `frontend/index.ts` или `frontend/lib/main.dart`).\n" +
                "3. Навигация (React Navigation / Expo Router / Flutter Router): Auth Stack (логин, регистрация), Main Tab Navigator (дашборд, профиль, трекинг, списки) и детальные экраны.\n" +
                "4. Экраны (Screens): экран входа/регистрации, главный дашборд, специализированные экраны функционала из ТЗ (трекер, таймер, интервалы, настройки, профиль).\n" +
                "5. UI-компоненты (Tailwind CSS + shadcn/ui): переиспользуемые кнопки, карточки, инпуты, модальные окна, индикаторы загрузки и ошибок, строго соответствующие дизайн-токенам (DESIGN_TOKENS.json).\n" +
                "6. API-сервисы и HTTP-клиент (Axios / Fetch / Dio): функции для каждого эндпоинта бэкенда, interceptors для автоматической подстановки JWT Bearer токена и обработки 401 Unauthorized.\n" +
                "7. Управление состоянием (Zustand / Redux / Context / Riverpod / Flutter BLoC) и сохранение JWT токенов в защищенное хранилище (AsyncStorage / SecureStore).\n" +
                "8. Интеграция с нативными API и фоновыми сервисами (если указано в ТЗ: GPS геопозиция, фоновые таймеры, Audio Ducking, разрешения в AndroidManifest.xml / Info.plist).\n" +
                "9. ДИЗАЙН-СИСТЕМА: Строго используй цвета, шрифты, радиусы скруглений и каркасы экранов из переданных дизайн-токенов!\n\n" +
                "СТРОГОЕ СООТВЕТСТВИЕ КОНТРАКТАМ БЭКЕНДА:\n" +
                "Используй точные URL эндпоинтов, параметры запросов, структуру JSON Request/Response DTO и коды статусов, которые реализованы в бэкенде!\n\n" +
                "ВАЖНО: Пиши полноценный рабочий код без сокращений и плейсхолдеров вроде '// TODO: implement'.\n\n" +
                "ФОРМАТ ВЫВОДА (СТРОГО):\n" +
                "Каждый файл оборачивай строго в формат:\n" +
                "[FILE: frontend/относительный_путь_к_файлу]\n" +
                "```{язык}\n" +
                "код_файла\n" +
                "```\n\n" +
                "Архитектурные правила и MCP:\n" + mcp + "\n\n" +
                "ТЗ:\n" + task + "\n\n" +
                "Спецификация Фронтенда (UI & Экраны):\n" + frontendSpec + "\n\n" +
                "UI Дизайн-токены (Tailwind / shadcn/ui):\n" + designTokens + "\n\n" +
                "Общая Спецификация:\n" + spec + "\n\n" +
                "Сгенерированный код бэкенда (API контракты и DTO):\n" + backendCode;
        }

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.FRONTEND_DEVELOPER);
        String codeResponse = executeLlmCall(chatModel, StepName.FRONTEND_DEVELOPER, prompt);

        AgentStep step = AgentStep.builder()
                .project(project)
                .stepName(StepName.FRONTEND_DEVELOPER)
                .status(StepStatus.COMPLETED)
                .prompt(prompt)
                .response(codeResponse)
                .completedAt(LocalDateTime.now())
                .build();
        agentStepRepository.save(step);

        contextService.saveFile(project, "GENERATED_FRONTEND_CODE.md", "/GENERATED_FRONTEND_CODE.md", codeResponse, FileType.CONTEXT_CODE, 1);
    }
}
