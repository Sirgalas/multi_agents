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
@Order(2)
@RequiredArgsConstructor
public class FrontendAnalystService extends BaseAgentService implements AgentsService {

    private final AgentClientFactory clientFactory;
    private final ProjectService projectService;
    private final ProjectContextService contextService;
    private final AgentStepRepository agentStepRepository;
    private final McpClientService mcpClientService;
    private final AgentPromptService agentPromptService;

    @Override
    public StepName getStepName() {
        return StepName.FRONTEND_ANALYST;
    }

    @Override
    public Optional<AgentsService> isNeedAgents(Project project) {
        if (project != null && project.getType() != ProjectType.BACKEND_ONLY) {
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
                .anyMatch(c -> "FRONTEND_SPEC.md".equals(c.getFileName())
                        && c.getFileContent() != null && !c.getFileContent().isBlank());

        boolean hasStep = agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.FRONTEND_ANALYST)
                .map(step -> step.getStatus() == StepStatus.COMPLETED)
                .orElse(false);

        return hasContext && hasStep;
    }

    @Override
    @Transactional
    public void work(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        if (isCompleted(project)) {
            log.info("Pipeline Step: {} already completed for project {}. Reusing specification.", getStepName(), projectId);
            return;
        }
        log.info("Pipeline Step: {} - detailing specification...", getStepName());
        String task = contextService.getLatestContextByType(project, FileType.TASK)
                .map(ProjectContext::getFileContent).orElse("");

        String archSpec = contextService.getContextByProject(project).stream()
                .filter(c -> "ARCHITECTURE_SPEC.md".equals(c.getFileName()))
                .findFirst()
                .map(ProjectContext::getFileContent)
                .orElse("");

        String backendSpec = contextService.getContextByProject(project).stream()
                .filter(c -> "BACKEND_SPEC.md".equals(c.getFileName()))
                .findFirst()
                .map(ProjectContext::getFileContent)
                .orElse("");

        String mcp = mcpClientService.aggregateFrontendMcpContext(project);

        Optional<AgentPrompt> promptOpt = agentPromptService != null
                ? agentPromptService.getEffectivePrompt(project, StepName.FRONTEND_ANALYST, false)
                : Optional.empty();

        String prompt;
        if (promptOpt.isPresent()) {
            Map<String, String> vars = Map.of(
                    "mcpRules", mcp,
                    "taskContent", task,
                    "archSpec", archSpec,
                    "backendSpec", backendSpec
            );
            prompt = agentPromptService.interpolate(promptOpt.get().getPrompt(), vars);
        } else {
            prompt = "Ты — Lead Frontend / UI-UX Systems Analyst (React, Next.js, React Native, TypeScript, Tailwind CSS, Zustand/Redux).\n" +
                "Сформируй исчерпывающую техническую спецификацию клиентского приложения (FRONTEND_SPEC.md) на основе ТЗ, архитектуры и спецификации бэкенда (BACKEND_SPEC.md).\n\n" +
                "СПЕЦИФИКАЦИЯ ДОЛЖНА ВКЛЮЧАТЬ:\n\n" +
                "1. КАРТА ЭКРАНОВ И НАВИГАЦИЯ (Screen Flow & Routing):\n" +
                "   - Полный список экранов и маршрутов (Routing / Stack Navigation / Tab Navigation)\n" +
                "   - Переходы между экранами, параметры навигации (Route Params)\n" +
                "   - Защищенные маршруты (Auth Guard: доступные только после аутентификации)\n\n" +
                "2. ДЕКОМПОЗИЦИЯ КОМПОНЕНТОВ ДЛЯ КАЖДОГО ЭКРАНА:\n" +
                "   - Иерархия компонентов (контейнеры, карточки, списки, кнопки, инпуты, модальные окна)\n" +
                "   - Props интерфейсы и события (onClick, onSubmit, onSelect) для ключевых компонентов\n\n" +
                "3. СОСТОЯНИЯ КАЖДОГО ЭКРАНА (UI States):\n" +
                "   - Initial State: первоначальное состояние при открытии экрана\n" +
                "   - Loading State: скелетоны, спиннеры, блокировка элементов управления\n" +
                "   - Empty State: отображение при отсутствии записей и кнопка призыва к действию\n" +
                "   - Success / Data State: отображение данных, списки с пагинацией\n" +
                "   - Error State: баннеры ошибок, кнопка 'Повторить попытку' (Retry)\n\n" +
                "4. ФОРМЫ, ПОЛЯ И КЛИЕНТСКАЯ ВАЛИДАЦИЯ:\n" +
                "   - Список всех полей ввода на каждой форме\n" +
                "   - Типы инпутов (text, password, email, number, select, datepicker)\n" +
                "   - Валидационные схемы (Zod / Yup): обязательность, минимальная/максимальная длина, regex, тексты сообщений об ошибках\n\n" +
                "5. СТЕЙТ-МЕНЕДЖМЕНТ И КЛИЕНТСКИЙ КЭШ:\n" +
                "   - Глобальное состояние (Auth Store: JWT access/refresh токены, текущий пользователь, тема и др.)\n" +
                "   - Локальное состояние экранов (useState / useReducer)\n" +
                "   - Интеграция с библиотеками кэширования запросов (TanStack Query / SWR)\n\n" +
                "6. ТОЧНЫЙ МАППИНГ НА БЭКЕНД API:\n" +
                "   - Привязка каждого экрана и действия к эндпоинтам из BACKEND_SPEC.md\n" +
                "   - TypeScript DTO интерфейсы (Request / Response) и типизированные API-сервисы (Axios / Fetch с Bearer JWT)\n" +
                "   - Обработка ошибок бэкенда (400, 401, 403, 404, 409, 500) и отображение toast/alert уведомлений\n\n" +
                "7. ПЛАТФОРМЕННЫЕ И МОБИЛЬНЫЕ ОСОБЕННОСТИ:\n" +
                "   - Адаптивность (Mobile-first, Flexbox/Grid, Tailwind CSS)\n" +
                "   - Разрешения (Permissions: геолокация, аудио, камера, push-уведомления) при наличии в ТЗ\n" +
                "   - Offline режим и локальное кэширование (AsyncStorage / LocalStorage)\n\n" +
                "ФОРМАТ ВЫВОДА: Начни ответ со строки '# FRONTEND TECHNICAL SPECIFICATION' и составь структурированный Markdown документ.\n\n" +
                "Архитектурные правила и MCP (Фронтенд):\n" + mcp + "\n\n" +
                "Исходное ТЗ проекта:\n" + task + "\n\n" +
                "Общая архитектура:\n" + archSpec + "\n\n" +
                "Спецификация Бэкенда (API & БД контракты):\n" + backendSpec;
        }

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.FRONTEND_ANALYST);
        String specResponse = executeLlmCall(chatModel, StepName.FRONTEND_ANALYST, prompt);

        AgentStep step = AgentStep.builder()
                .project(project)
                .stepName(StepName.FRONTEND_ANALYST)
                .status(StepStatus.COMPLETED)
                .prompt(prompt)
                .response(specResponse)
                .completedAt(LocalDateTime.now())
                .build();
        agentStepRepository.save(step);

        contextService.saveFile(project, "FRONTEND_SPEC.md", "/FRONTEND_SPEC.md", specResponse, FileType.SPEC, 1);
        log.info("Frontend Analyst generated FRONTEND_SPEC.md for project ID: {}", projectId);
    }
}
