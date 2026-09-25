package ru.sergalas.orchestrator.service.agent.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.ArchitectQuestion;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.ProjectStatus;
import ru.sergalas.orchestrator.entity.enums.ProjectType;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.exception.AgentException;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.repository.ArchitectQuestionRepository;
import ru.sergalas.orchestrator.service.agent.AgentClientFactory;
import ru.sergalas.orchestrator.service.agent.AgentsService;
import ru.sergalas.orchestrator.service.agent.ArchitectService;
import ru.sergalas.orchestrator.service.agent.BaseAgentService;
import ru.sergalas.orchestrator.service.mcp.McpClientService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import ru.sergalas.orchestrator.entity.AgentPrompt;
import ru.sergalas.orchestrator.service.prompt.AgentPromptService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchitectServiceImpl extends BaseAgentService implements AgentsService, ArchitectService {

    private final AgentClientFactory clientFactory;
    private final ProjectService projectService;
    private final ProjectContextService contextService;
    private final AgentStepRepository agentStepRepository;
    private final ArchitectQuestionRepository questionRepository;
    private final McpClientService mcpClientService;
    private final ObjectMapper objectMapper;
    private final AgentPromptService agentPromptService;

    public static final int MAX_QUESTION_ROUNDS = 4;

    @Override
    public StepName getStepName() {
        return StepName.ARCHITECT;
    }

    @Override
    public Optional<AgentsService> isNeedAgents(Project project) {
        return Optional.of(this);
    }

    @Override
    @Transactional
    public void work(Long projectId) {
        analyzeTask(projectId);
    }

    @Transactional
    public void analyzeTask(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        String taskContent = contextService.getLatestContextByType(project, FileType.TASK)
                .map(ProjectContext::getFileContent)
                .orElse("No task content provided.");

        String mcpRules = mcpClientService.aggregateMcpContext(project);

        long questionRounds = questionRepository.countByProject(project);
        boolean finalRound = questionRounds >= MAX_QUESTION_ROUNDS;

        ProjectType projectType = project.getType() != null ? project.getType() : ProjectType.FULLSTACK;
        String projectTypeHint = switch (projectType) {
            case BACKEND_ONLY -> "ТИП ПРОЕКТА: ТОЛЬКО БЭКЕНД (REST API / БД). Не проектируй клиентский интерфейс, экраны и фронтенд. Сосредоточься на архитектуре бэкенда, API, сущностях и персистентности.";
            case FRONTEND_ONLY -> "ТИП ПРОЕКТА: ТОЛЬКО ФРОНТЕНД (UI / Mobile). Не проектируй серверную БД и бэкенд. Сосредоточься на структуре экранов, UI-компонентах, управлении состоянием и контрактах внешних API.";
            case FULLSTACK -> "ТИП ПРОЕКТА: FULLSTACK (Бэкенд + Фронтенд). Спроектируй как серверную архитектуру (API, сущности, сервисы), так и клиентскую архитектуру (экраны, компоненты).";
        };

        Optional<AgentPrompt> promptOpt = agentPromptService != null 
                ? agentPromptService.getEffectivePrompt(project, StepName.ARCHITECT, finalRound) 
                : Optional.empty();

        String prompt;
        if (promptOpt.isPresent()) {
            Map<String, String> vars = Map.of(
                    "mcpRules", mcpRules,
                    "taskContent", taskContent,
                    "questionRounds", String.valueOf(questionRounds + (finalRound ? 0 : 1)),
                    "maxRounds", String.valueOf(MAX_QUESTION_ROUNDS),
                    "projectType", projectType.name(),
                    "projectTypeHint", projectTypeHint
            );
            prompt = agentPromptService.interpolate(promptOpt.get().getPrompt(), vars);
        } else if (finalRound) {
            prompt = "Ты — Principal Software Architect. Проанализируй следующее ТЗ и правила разработки:\n" +
                    "Направление проекта:\n" + projectTypeHint + "\n\n" +
                    "Правила/Контекст:\n" + mcpRules + "\n" +
                    "ТЗ проекта:\n" + taskContent + "\n\n" +
                    "ИНСТРУКЦИЯ:\n" +
                    "Лимит уточняющих вопросов исчерпан (пройдено " + questionRounds + " раунда из " + MAX_QUESTION_ROUNDS + "). " +
                    "НЕ ЗАДАВАЙ никаких новых вопросов. На основе всей имеющейся информации составь исчерпывающую архитектурную спецификацию: " +
                    "структуру пакетов, перечень сущностей, сервисов, DTO и REST контроллеров, начав свой ответ строго со слова SPECIFICATION:";
        } else {
            prompt = "Ты — Principal Software Architect. Проанализируй следующее ТЗ и правила разработки (Раунд вопросов " + (questionRounds + 1) + " из " + MAX_QUESTION_ROUNDS + "):\n" +
                    "Направление проекта:\n" + projectTypeHint + "\n\n" +
                    "Правила/Контекст:\n" + mcpRules + "\n" +
                    "ТЗ проекта:\n" + taskContent + "\n\n" +
                    "ИНСТРУКЦИЯ:\n" +
                    "1. Если в ТЗ есть критические неясности, противоречия или не хватает важных технических деталей, верни ТОЛЬКО JSON массив вопросов без лишнего текста вокруг в формате:\n" +
                    "[{\"id\": \"q1\", \"question\": \"Текст конкретного вопроса\"}]\n" +
                    "2. Если ТЗ достаточно полно и понятно для старта проектирования, составь подробную архитектурную спецификацию: структуру пакетов, перечень сущностей, сервисов, DTO и REST контроллеров, начав свой ответ строго со слова SPECIFICATION:";
        }

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.ARCHITECT);
        String response = executeLlmCall(chatModel, StepName.ARCHITECT, prompt);

        List<Map<String, String>> questions = finalRound ? Collections.emptyList() : parseQuestions(response);
        if (!questions.isEmpty()) {
            AgentStep step = AgentStep.builder()
                    .project(project)
                    .stepName(StepName.ARCHITECT)
                    .status(StepStatus.WAITING_FOR_INPUT)
                    .prompt(prompt)
                    .response("Вопросы архитектора для уточнения ТЗ (HITL)")
                    .build();
            agentStepRepository.save(step);

            ArchitectQuestion architectQuestion = ArchitectQuestion.builder()
                    .project(project)
                    .agentStep(step)
                    .questions(questions)
                    .status("PENDING")
                    .build();
            questionRepository.save(architectQuestion);
            projectService.updateStatus(projectId, ProjectStatus.WAITING_FOR_INPUT);
            log.info("Architect generated {} clarifying questions for project ID: {}. Pipeline paused.", questions.size(), projectId);
            return;
        }

        // Generate full spec and file structure
        AgentStep step = AgentStep.builder()
                .project(project)
                .stepName(StepName.ARCHITECT)
                .status(StepStatus.COMPLETED)
                .prompt(prompt)
                .response(response)
                .completedAt(LocalDateTime.now())
                .build();
        agentStepRepository.save(step);

        contextService.saveFile(project, "ARCHITECTURE_SPEC.md", "/ARCHITECTURE_SPEC.md", response, FileType.SPEC, 1);
        projectService.updateStatus(projectId, ProjectStatus.WAITING_FOR_INPUT);
        log.info("Architect generated specification for project ID: {}. Pausing in WAITING_FOR_INPUT for user approval.", projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public ArchitectQuestionsResponse getPendingQuestions(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        return questionRepository.findFirstByProjectAndStatus(project, "PENDING")
                .map(q -> ArchitectQuestionsResponse.builder()
                        .questionId(q.getId())
                        .projectId(projectId)
                        .questions(q.getQuestions())
                        .status(q.getStatus())
                        .build())
                .orElse(null);
    }

    @Override
    @Transactional
    public void processAnswers(Long questionId, List<Map<String, String>> answers) {
        ArchitectQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AgentException("Question not found: " + questionId));
        
        question.setAnswers(answers);
        question.setStatus("ANSWERED");
        question.setAnsweredAt(LocalDateTime.now());
        questionRepository.save(question);

        Project project = question.getProject();
        String prompt = "Архитектор получил ответы на уточняющие вопросы:\n" + answers +
                "\nТеперь составь финальную архитектурную спецификацию и файловую структуру для проекта: " + project.getName() +
                ". Начни со слова SPECIFICATION:";

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.ARCHITECT);
        String specResponse = executeLlmCall(chatModel, StepName.ARCHITECT, prompt);

        contextService.saveFile(project, "ARCHITECTURE_SPEC.md", "/ARCHITECTURE_SPEC.md", specResponse, FileType.SPEC, 1);

        AgentStep step = question.getAgentStep();
        step.setStatus(StepStatus.COMPLETED);
        step.setCompletedAt(LocalDateTime.now());
        step.setResponse(specResponse);
        agentStepRepository.save(step);

        projectService.updateStatus(project.getId(), ProjectStatus.WAITING_FOR_INPUT);
        log.info("Answers processed and specification saved for project ID: {}. Pausing in WAITING_FOR_INPUT for user approval.", project.getId());
    }

    private List<Map<String, String>> parseQuestions(String response) {
        if (response == null || response.isBlank()) {
            return Collections.emptyList();
        }
        String jsonCandidate = extractJsonArray(response);
        if (jsonCandidate != null) {
            try {
                List<Map<String, String>> questions = objectMapper.readValue(jsonCandidate, new TypeReference<>() {});
                if (questions != null && !questions.isEmpty() && questions.get(0).containsKey("question")) {
                    return questions;
                }
            } catch (Exception e) {
                log.warn("Failed to parse JSON questions: {}", e.getMessage());
            }
        }

        // Fallback: If model did not return pure JSON, but wrote questions ending with ?
        if (!response.contains("SPECIFICATION:") && (response.contains("?") && (response.toLowerCase().contains("вопрос") || response.toLowerCase().contains("question")))) {
            List<Map<String, String>> extracted = new ArrayList<>();
            int count = 1;
            for (String line : response.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.endsWith("?") && (trimmed.matches("^\\d+[.)].*") || trimmed.startsWith("-") || trimmed.startsWith("*"))) {
                    String qText = trimmed.replaceFirst("^[\\d+.)\\-*\\s]+", "").trim();
                    if (!qText.isBlank()) {
                        extracted.add(Map.of("id", "q" + (count++), "question", qText));
                    }
                }
            }
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }

        return Collections.emptyList();
    }

    private String extractJsonArray(String raw) {
        if (raw == null) return null;
        String text = raw.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.trim();
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    @Override
    public boolean isCompleted(Project project) {
        if (project == null) {
            return false;
        }
        ArchitectQuestionsResponse pending = getPendingQuestions(project.getId());
        if (pending != null && "PENDING".equalsIgnoreCase(pending.getStatus())) {
            return false;
        }

        Optional<ProjectContext> specContext = contextService.getContextByProject(project).stream()
                .filter(c -> "ARCHITECTURE_SPEC.md".equals(c.getFileName()) || (c.getFileType() == FileType.SPEC && !"BACKEND_SPEC.md".equals(c.getFileName()) && !"FRONTEND_SPEC.md".equals(c.getFileName())))
                .findFirst();

        if (specContext.isEmpty()) {
            specContext = contextService.getLatestContextByType(project, FileType.SPEC)
                    .filter(c -> !"BACKEND_SPEC.md".equals(c.getFileName()) && !"FRONTEND_SPEC.md".equals(c.getFileName()));
        }

        if (specContext.isEmpty() || specContext.get().getFileContent() == null || specContext.get().getFileContent().isBlank()) {
            return false;
        }

        String content = specContext.get().getFileContent().trim();
        if (content.startsWith("[") && content.contains("\"question\":")) {
            return false;
        }

        return true;
    }
}