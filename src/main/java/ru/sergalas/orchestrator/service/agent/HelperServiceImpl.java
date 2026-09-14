package ru.sergalas.orchestrator.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
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
public class HelperServiceImpl implements HelperService {

    private final AgentClientFactory clientFactory;
    private final ProjectService projectService;
    private final ProjectContextService contextService;
    private final AgentStepRepository agentStepRepository;

    @Override
    @Transactional
    public void generateInfrastructure(Long projectId) {
        Project project = projectService.getProjectById(projectId);

        String prompt = "Ты — DevOps Engineer. Сгенерируй файлы окружения для проекта '" + project.getName() + "':\n" +
                "1. build.gradle (Java 21, Spring Boot 3.4)\n" +
                "2. settings.gradle\n" +
                "3. Dockerfile\n" +
                "4. docker-compose.yml\n" +
                "5. .gitignore\n" +
                "6. README.md\n" +
                "Каждый файл оформляй строго в формате:\n[FILE: относительный_путь]\n```формат\nконтент\n```";

        OpenAiChatModel chatModel = clientFactory.createClient(StepName.HELPER);
        String infraResponse = chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();

        AgentStep step = AgentStep.builder()
                .project(project)
                .stepName(StepName.HELPER)
                .status(StepStatus.COMPLETED)
                .prompt(prompt)
                .response(infraResponse)
                .completedAt(LocalDateTime.now())
                .build();
        agentStepRepository.save(step);

        contextService.saveFile(project, "GENERATED_INFRA.md", "/GENERATED_INFRA.md", infraResponse, FileType.CONTEXT_CODE, 1);
    }
}