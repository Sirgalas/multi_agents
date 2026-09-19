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
import ru.sergalas.orchestrator.service.agent.BackendAnalystService;
import ru.sergalas.orchestrator.service.agent.BaseAgentService;
import ru.sergalas.orchestrator.service.mcp.McpClientService;
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
public class BackendAnalystServiceImpl extends BaseAgentService implements BackendAnalystService {

    private final AgentClientFactory clientFactory;
    private final ProjectService projectService;
    private final ProjectContextService contextService;
    private final AgentStepRepository agentStepRepository;
    private final McpClientService mcpClientService;
    private final AgentPromptService agentPromptService;

    @Override
    @Transactional
    public void analyzeBackend(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        String task = contextService.getLatestContextByType(project, FileType.TASK)
                .map(ProjectContext::getFileContent).orElse("");

        String archSpec = contextService.getContextByProject(project).stream()
                .filter(c -> "ARCHITECTURE_SPEC.md".equals(c.getFileName()) || c.getFileType() == FileType.SPEC)
                .findFirst()
                .map(ProjectContext::getFileContent)
                .orElse("");

        String mcp = mcpClientService.aggregateBackendMcpContext(project);

        Optional<AgentPrompt> promptOpt = agentPromptService != null
                ? agentPromptService.getEffectivePrompt(project, StepName.BACKEND_ANALYST, false)
                : Optional.empty();

        String prompt;
        if (promptOpt.isPresent()) {
            Map<String, String> vars = Map.of(
                    "mcpRules", mcp,
                    "taskContent", task,
                    "archSpec", archSpec
            );
            prompt = agentPromptService.interpolate(promptOpt.get().getPrompt(), vars);
        } else {
            prompt = "Ты — Lead Backend Systems Analyst & API Architect (Spring Boot, Java 21, PostgreSQL, Liquibase, Security).\n" +
                "Сформируй исчерпывающую техническую спецификацию серверной части проекта (BACKEND_SPEC.md) на основе ТЗ и общей архитектуры.\n\n" +
                "СПЕЦИФИКАЦИЯ ДОЛЖНА ВКЛЮЧАТЬ:\n\n" +
                "1. СХЕМА РЕЛЯЦИОННОЙ БАЗЫ ДАННЫХ (PostgreSQL):\n" +
                "   - Точные имена таблиц (в snake_case, например: users, orders, order_items)\n" +
                "   - Полный перечень колонок с типами данных PostgreSQL (BIGSERIAL, VARCHAR(255), TIMESTAMP, NUMERIC(12,2), BOOLEAN, JSONB и т.д.)\n" +
                "   - Ограничения: PRIMARY KEY, NOT NULL, UNIQUE, DEFAULT, CHECK\n" +
                "   - Внешние ключи (FOREIGN KEY) и правила каскадирования (ON DELETE CASCADE/SET NULL)\n" +
                "   - Индексы для оптимизации запросов и фильтрации\n" +
                "   - Перечисления (Enums): точные строковые константы статусов и типов\n\n" +
                "2. КОНТРАКТ REST API & OPENAPI СПЕЦИФИКАЦИЯ:\n" +
                "   - Полный список эндпоинтов с версионированием (/api/v1/...) и HTTP-методами (GET, POST, PUT, PATCH, DELETE)\n" +
                "   - Path-переменные и Query-параметры (фильтры, сортировка sort, пагинация page, size) с указанием типов\n" +
                "   - Request DTO: названия классов, детальный список полей, типы, правила Jakarta Validation (@NotNull, @NotBlank, @Size, @Email, @Pattern)\n" +
                "   - Response DTO: структура возвращаемых объектов, пагинация PagedResponse\n" +
                "   - Примеры JSON Request / Response для каждого эндпоинта\n" +
                "   - HTTP статус-коды (200, 201, 204, 400, 401, 403, 404, 409, 500) и формат ошибок ErrorResponse\n\n" +
                "3. СЕРВИСНЫЙ СЛОЙ И БИЗНЕС-ЛОГИКА:\n" +
                "   - Декомпозиция сервисов и сигнатуры методов бизнес-логики\n" +
                "   - Транзакционные границы (@Transactional readOnly = true / rollbackFor = Exception.class)\n" +
                "   - Правила бизнес-валидаций и условия выброса кастомных исключений\n\n" +
                "4. БЕЗОПАСНОСТЬ И АВТОРИЗАЦИЯ:\n" +
                "   - Ролевая модель (ROLE_USER, ROLE_ADMIN и др.)\n" +
                "   - Матрица доступа (публичные эндпоинты vs защищенные JWT Bearer токеном)\n\n" +
                "ФОРМАТ ВЫВОДА: Начни ответ со строки '# BACKEND TECHNICAL SPECIFICATION' и составь четкий, структурированный Markdown документ без сокращений.\n\n" +
                "Архитектурные правила и MCP (Бэкенд):\n" + mcp + "\n\n" +
                "Исходное ТЗ проекта:\n" + task + "\n\n" +
                "Общая архитектурная спецификация:\n" + archSpec;
        }

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.BACKEND_ANALYST);
        String specResponse = executeLlmCall(chatModel, StepName.BACKEND_ANALYST, prompt);

        AgentStep step = AgentStep.builder()
                .project(project)
                .stepName(StepName.BACKEND_ANALYST)
                .status(StepStatus.COMPLETED)
                .prompt(prompt)
                .response(specResponse)
                .completedAt(LocalDateTime.now())
                .build();
        agentStepRepository.save(step);

        contextService.saveFile(project, "BACKEND_SPEC.md", "/BACKEND_SPEC.md", specResponse, FileType.SPEC, 1);
        log.info("Backend Analyst generated BACKEND_SPEC.md for project ID: {}", projectId);
    }
}
