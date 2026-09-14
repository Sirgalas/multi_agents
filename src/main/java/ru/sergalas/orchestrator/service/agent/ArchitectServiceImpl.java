package ru.sergalas.orchestrator.service.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.ArchitectQuestion;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.exception.AgentException;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.repository.ArchitectQuestionRepository;
import ru.sergalas.orchestrator.service.mcp.McpClientService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchitectServiceImpl implements ArchitectService {

    private final AgentClientFactory clientFactory;
    private final ProjectService projectService;
    private final ProjectContextService contextService;
    private final AgentStepRepository agentStepRepository;
    private final ArchitectQuestionRepository questionRepository;
    private final McpClientService mcpClientService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void analyzeTask(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        String taskContent = contextService.getLatestContextByType(project, FileType.TASK)
                .map(ProjectContext::getFileContent)
                .orElse("No task content provided.");

        String mcpRules = mcpClientService.aggregateMcpContext(project);

        String prompt = "Ты — Principal Software Architect. Проанализируй следующее ТЗ и правила:\n" +
                "Правила/Контекст:\n" + mcpRules + "\n" +
                "ТЗ проекта:\n" + taskContent + "\n" +
                "Если в ТЗ есть критические неясности, верни ТОЛЬКО JSON список вопросов в формате: [{\"id\": \"q1\", \"question\": \"Текст вопроса\"}]. " +
                "Если всё понятно и готово к проектированию, сформируй полную спецификацию архитектуры, перечень файлов, моделей и сервисов, начав со слова SPECIFICATION:";

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.ARCHITECT);
        String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();

        if (response.trim().startsWith("[") && response.trim().endsWith("]")) {
            try {
                List<Map<String, String>> questions = objectMapper.readValue(response, new TypeReference<>() {});
                
                AgentStep step = AgentStep.builder()
                        .project(project)
                        .stepName(StepName.ARCHITECT)
                        .status(StepStatus.WAITING_FOR_INPUT)
                        .prompt(prompt)
                        .response("Questions generated for HITL")
                        .build();
                agentStepRepository.save(step);

                ArchitectQuestion architectQuestion = ArchitectQuestion.builder()
                        .project(project)
                        .agentStep(step)
                        .questions(questions)
                        .status("PENDING")
                        .build();
                questionRepository.save(architectQuestion);
                return;
            } catch (Exception e) {
                log.warn("Failed to parse JSON questions, fallback to specification mode: {}", e.getMessage());
            }
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
                "\nТеперь составь финальную архитектурную спецификацию и файловую структуру для проекта: " + project.getName();

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.ARCHITECT);
        String specResponse = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();

        contextService.saveFile(project, "ARCHITECTURE_SPEC.md", "/ARCHITECTURE_SPEC.md", specResponse, FileType.SPEC, 1);

        AgentStep step = question.getAgentStep();
        step.setStatus(StepStatus.COMPLETED);
        step.setCompletedAt(LocalDateTime.now());
        step.setResponse(specResponse);
        agentStepRepository.save(step);
    }
}