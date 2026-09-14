package ru.sergalas.orchestrator.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
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
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TesterServiceImpl implements TesterService {

    private final AgentClientFactory clientFactory;
    private final ProjectService projectService;
    private final ProjectContextService contextService;
    private final AgentStepRepository agentStepRepository;

    @Override
    @Transactional
    public void generateTests(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        String code = contextService.getLatestContextByType(project, FileType.CONTEXT_CODE)
                .map(ProjectContext::getFileContent).orElse("");

        String prompt = "Ты — Senior QA Automation Engineer (JUnit 5, Mockito, Spring Boot Test). " +
                "Напиши модульные и интеграционные тесты для следующего кода. " +
                "Форматируй вывод строго в формате:\n[FILE: src/test/java/...]\n```java\nтесты\n```\n\nКод:\n" + code;

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.TESTER);
        String testResponse = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();

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