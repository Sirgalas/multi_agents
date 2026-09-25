package ru.sergalas.orchestrator.service.agent.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.entity.AgentPrompt;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.ProjectType;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.service.agent.AgentClientFactory;
import ru.sergalas.orchestrator.service.agent.AgentsService;
import ru.sergalas.orchestrator.service.agent.BaseAgentService;
import ru.sergalas.orchestrator.service.mcp.McpClientService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;
import ru.sergalas.orchestrator.service.prompt.AgentPromptService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesignerService extends BaseAgentService implements AgentsService {

    private final AgentClientFactory clientFactory;
    private final ProjectService projectService;
    private final ProjectContextService contextService;
    private final AgentStepRepository agentStepRepository;
    private final McpClientService mcpClientService;
    private final AgentPromptService agentPromptService;

    @Override
    public StepName getStepName() {
        return StepName.DESIGNER;
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
                .anyMatch(c -> "DESIGN_TOKENS.json".equals(c.getFileName())
                        && c.getFileContent() != null && !c.getFileContent().isBlank());

        boolean hasStep = agentStepRepository
                .findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.DESIGNER)
                .map(step -> step.getStatus() == StepStatus.COMPLETED)
                .orElse(false);

        return hasContext && hasStep;
    }

    @Override
    @Transactional
    public void work(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        if (isCompleted(project)) {
            log.info("Pipeline Step: {} already completed for project {}. Reusing design tokens.", getStepName(), projectId);
            return;
        }
        log.info("Pipeline Step: {} - generating design system and tokens...", getStepName());

        String task = contextService.getLatestContextByType(project, FileType.TASK)
                .map(ProjectContext::getFileContent).orElse("");

        String archSpec = contextService.getContextByProject(project).stream()
                .filter(c -> "ARCHITECTURE_SPEC.md".equals(c.getFileName()))
                .findFirst()
                .map(ProjectContext::getFileContent)
                .orElse("");

        String frontendSpec = contextService.getContextByProject(project).stream()
                .filter(c -> "FRONTEND_SPEC.md".equals(c.getFileName()))
                .findFirst()
                .map(ProjectContext::getFileContent)
                .orElse("");

        String mcp = mcpClientService.aggregateFrontendMcpContext(project);

        Optional<AgentPrompt> promptOpt = agentPromptService != null
                ? agentPromptService.getEffectivePrompt(project, StepName.DESIGNER, false)
                : Optional.empty();

        String prompt;
        if (promptOpt.isPresent()) {
            Map<String, String> vars = Map.of(
                    "mcpRules", mcp,
                    "taskContent", task,
                    "archSpec", archSpec,
                    "frontendSpec", frontendSpec
            );
            prompt = agentPromptService.interpolate(promptOpt.get().getPrompt(), vars);
        } else {
            prompt = "Ты — Lead UI/UX Systems Designer & Design Architect (Tailwind CSS, shadcn/ui, Radix UI, Figma Tokens).\n" +
                "Спроектируй исчерпывающую дизайн-систему приложения (DESIGN_TOKENS.json) на основе ТЗ, архитектуры и спецификации клиентских экранов (FRONTEND_SPEC.md).\n\n" +
                "ДИЗАЙН-СИСТЕМА ДОЛЖНА ВКЛЮЧАТЬ:\n\n" +
                "1. СЕМАНТИЧЕСКАЯ ЦВЕТОВАЯ ПАЛИТРА (Light & Dark Theme):\n" +
                "   - Primary (основной бренд-акцент), Secondary (вторичный), Accent (яркие маркеры)\n" +
                "   - Background, Foreground, Surface/Card, Surface-Foreground\n" +
                "   - Muted, Muted-Foreground (подписи, неактивный текст)\n" +
                "   - Destructive (ошибки, удаление, алерты)\n" +
                "   - Border, Ring (границы и фокусные кольца shadcn/ui)\n" +
                "   Укажи Hex-значения и классы Tailwind CSS с гарантией высокой контрастности (WCAG AA/AAA).\n\n" +
                "2. ТИПОГРАФИКА И ШРИФТЫ:\n" +
                "   - Гармоничные шрифтовые пары (Inter / Geist / Roboto / system-ui)\n" +
                "   - Шкала размеров (h1, h2, h3, h4, body, small, tiny) с соответствующими классами Tailwind CSS (например, text-3xl font-bold tracking-tight)\n\n" +
                "3. ТОКЕНЫ СКРУГЛЕНИЙ (Radii) И ТЕНЕЙ (Shadows):\n" +
                "   - Скругления: sm, md, lg, full\n" +
                "   - Тени: card, modal, dropdown, header\n\n" +
                "4. СТАНДАРТЫ КОМПОНЕНТОВ SHADCN/UI:\n" +
                "   - Стили и варианты (variants) для Button, Card, Input, Dialog/Modal, Table, Badge, Tabs, Alert/Toast\n\n" +
                "5. КАРКАСЫ ЭКРАНОВ (Wireframes & Screen Layouts):\n" +
                "   - Сетка и структура (Sidebar + Topbar + Content Grid / Flex layout) для каждого экрана из FRONTEND_SPEC.md\n" +
                "   - Спецификация ключевых компонентов для каждого экрана\n\n" +
                "ФОРМАТ ВЫВОДА: Верни валидный, чистый JSON со структурой токенов и макетов экранов без лишнего вступительного текста:\n" +
                "{\n" +
                "  \"theme\": {\n" +
                "    \"colors\": { \"light\": { ... }, \"dark\": { ... } },\n" +
                "    \"typography\": { \"fontFamily\": \"...\", \"scale\": { ... } },\n" +
                "    \"borderRadius\": { ... },\n" +
                "    \"shadows\": { ... }\n" +
                "  },\n" +
                "  \"components\": { \"button\": { ... }, \"card\": { ... }, \"input\": { ... } },\n" +
                "  \"screens\": [ { \"name\": \"...\", \"layout\": \"...\", \"components\": [ ... ] } ]\n" +
                "}\n\n" +
                "Архитектурные правила и MCP (Стили и UI):\n" + mcp + "\n\n" +
                "Исходное ТЗ проекта:\n" + task + "\n\n" +
                "Общая архитектура:\n" + archSpec + "\n\n" +
                "Спецификация Фронтенда (Экраны и Потоки):\n" + frontendSpec;
        }

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.DESIGNER);
        String designResponse = executeLlmCall(chatModel, StepName.DESIGNER, prompt);

        AgentStep step = AgentStep.builder()
                .project(project)
                .stepName(StepName.DESIGNER)
                .status(StepStatus.COMPLETED)
                .prompt(prompt)
                .response(designResponse)
                .completedAt(LocalDateTime.now())
                .build();
        agentStepRepository.save(step);

        contextService.saveFile(project, "DESIGN_TOKENS.json", "/DESIGN_TOKENS.json", designResponse, FileType.SPEC, 1);
        log.info("Designer generated DESIGN_TOKENS.json for project ID: {}", projectId);
    }
}
