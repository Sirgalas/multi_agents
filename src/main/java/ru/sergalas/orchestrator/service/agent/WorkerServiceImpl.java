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
import ru.sergalas.orchestrator.service.mcp.McpClientService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerServiceImpl implements WorkerService {

    private final AgentClientFactory clientFactory;
    private final ProjectService projectService;
    private final ProjectContextService contextService;
    private final AgentStepRepository agentStepRepository;
    private final McpClientService mcpClientService;

    @Override
    @Transactional
    public void generateSourceCode(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        String task = contextService.getLatestContextByType(project, FileType.TASK)
                .map(ProjectContext::getFileContent).orElse("");
        String spec = contextService.getLatestContextByType(project, FileType.SPEC)
                .map(ProjectContext::getFileContent).orElse("");
        String mcp = mcpClientService.aggregateMcpContext(project);

        String prompt = "Ты — Senior Full-Stack / Backend Engineer (Java 21, Spring Boot 3.4). " +
                "Сгенерируй полный, компилируемый исходный код проекта по ТЗ и Спецификации.\n" +
                "ВАЖНО: Каждый файл оборачивай строго в формат:\n" +
                "[FILE: относительный_путь_к_файлу]\n```java\nкод\n```\n\n" +
                "Правила:\n" + mcp + "\n" +
                "ТЗ:\n" + task + "\n" +
                "Спецификация:\n" + spec;

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.WORKER);
        String codeResponse = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();

        AgentStep step = AgentStep.builder()
                .project(project)
                .stepName(StepName.WORKER)
                .status(StepStatus.COMPLETED)
                .prompt(prompt)
                .response(codeResponse)
                .completedAt(LocalDateTime.now())
                .build();
        agentStepRepository.save(step);

        contextService.saveFile(project, "GENERATED_CODE.md", "/GENERATED_CODE.md", codeResponse, FileType.CONTEXT_CODE, 1);
    }
}