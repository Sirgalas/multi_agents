package ru.sergalas.orchestrator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.request.ArchitectQuestionResponse;
import ru.sergalas.orchestrator.dto.request.ProjectCreateRequest;
import ru.sergalas.orchestrator.dto.response.*;
import ru.sergalas.orchestrator.entity.*;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.agent.ArchitectAgent;
import ru.sergalas.orchestrator.service.agent.HelperAgent;
import ru.sergalas.orchestrator.service.agent.TesterAgent;
import ru.sergalas.orchestrator.service.agent.WorkerAgent;
import ru.sergalas.orchestrator.util.Either;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorServiceImpl implements OrchestratorService {

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final ProjectContextRepository contextRepository;
    private final AgentStepRepository agentStepRepository;
    private final ArchitectAgent architectAgent;
    private final WorkerAgent workerAgent;
    private final TesterAgent testerAgent;
    private final HelperAgent helperAgent;
    private final ArchiveService archiveService;

    @Override
    @Transactional
    public Either<ArchitectQuestionsResponse, Long> startGeneration(ProjectCreateRequest request, String username) {
        Project project = projectService.createProject(request, username);
        Long projectId = project.getId();

        FileStructureTemplate template = project.getTaskTemplate() != null ? project.getTaskTemplate().getFileStructureTemplate() : null;
        List<ProjectMcpServer> mcpServers = project.getMcpServers();
        List<ProjectContext> contextFiles = contextRepository.findByProjectId(projectId);

        recordStep(project, StepName.ARCHITECT, StepStatus.IN_PROGRESS, request.getTaskContent(), null);

        Either<ArchitectQuestionsResponse, ArchitectSpecification> architectResult =
                architectAgent.analyze(projectId, request.getTaskContent(), template, mcpServers, contextFiles);

        if (architectResult.isLeft()) {
            recordStep(project, StepName.ARCHITECT, StepStatus.COMPLETED, "Needs Clarification", "Clarification requested.");
            return Either.left(architectResult.getLeft());
        }

        ArchitectSpecification spec = architectResult.getRight();
        recordStep(project, StepName.ARCHITECT, StepStatus.COMPLETED, "Specification created", spec.getArchitectureSpec());

        saveOrUpdateContext(project, "SPECIFICATION.md", spec.getArchitectureSpec(), FileType.SPEC);
        saveOrUpdateContext(project, "FILE_STRUCTURE.txt", spec.getStructureTree(), FileType.FILE_STRUCTURE);

        executePipeline(project, spec);

        return Either.right(projectId);
    }

    @Override
    @Transactional
    public GenerationResultResponse continueGeneration(ArchitectQuestionResponse response) {
        Project project = projectRepository.findById(response.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + response.getProjectId()));

        ProjectContext taskContext = contextRepository.findByProjectIdAndFileName(project.getId(), "TASK.md")
                .orElseThrow(() -> new IllegalStateException("Task context missing"));

        FileStructureTemplate template = project.getTaskTemplate() != null ? project.getTaskTemplate().getFileStructureTemplate() : null;

        ArchitectSpecification spec = architectAgent.finalizeSpec(
                taskContext.getFileContent(),
                response.getAnswers(),
                template,
                project.getMcpServers()
        );

        saveOrUpdateContext(project, "SPECIFICATION.md", spec.getArchitectureSpec(), FileType.SPEC);
        executePipeline(project, spec);

        List<ProjectContext> allContexts = contextRepository.findByProjectId(project.getId());
        List<GenerationResultResponse.GeneratedFile> generatedFiles = allContexts.stream()
                .map(ctx -> new GenerationResultResponse.GeneratedFile(ctx.getFileName(), ctx.getFileContent().split("\r\n|\r|\n").length))
                .toList();

        return GenerationResultResponse.builder()
                .projectId(project.getId())
                .success(true)
                .message("Generation completed successfully!")
                .files(generatedFiles)
                .zipArchivePath("/projects/" + project.getId() + "/download")
                .build();
    }

    private void executePipeline(Project project, ArchitectSpecification spec) {
        // Step 2: Worker Agent (Code Generation)
        recordStep(project, StepName.WORKER, StepStatus.IN_PROGRESS, spec.getArchitectureSpec(), null);
        Map<String, String> generatedCode = workerAgent.generateCode(spec, project.getMcpServers());
        for (var entry : generatedCode.entrySet()) {
            saveOrUpdateContext(project, entry.getKey(), entry.getValue(), FileType.CONTEXT_CODE);
        }
        recordStep(project, StepName.WORKER, StepStatus.COMPLETED, "Code generation complete", "Generated " + generatedCode.size() + " files.");

        // Step 3: Tester Agent (Unit & Integration Tests)
        recordStep(project, StepName.TESTER, StepStatus.IN_PROGRESS, "Generate Tests", null);
        Map<String, String> generatedTests = testerAgent.generateTests(generatedCode, spec);
        for (var entry : generatedTests.entrySet()) {
            saveOrUpdateContext(project, entry.getKey(), entry.getValue(), FileType.CONTEXT_CODE);
        }
        recordStep(project, StepName.TESTER, StepStatus.COMPLETED, "Tests generated", "Generated " + generatedTests.size() + " test files.");

        // Step 4: Helper Agent (Docker, Build, Configs)
        recordStep(project, StepName.HELPER, StepStatus.IN_PROGRESS, "Generate Configurations", null);
        Map<String, String> helperFiles = helperAgent.generateConfig(spec, generatedCode);
        for (var entry : helperFiles.entrySet()) {
            saveOrUpdateContext(project, entry.getKey(), entry.getValue(), FileType.CONTEXT_CODE);
        }
        recordStep(project, StepName.HELPER, StepStatus.COMPLETED, "Configs generated", "Generated " + helperFiles.size() + " config files.");

        // Combine all files and package to ZIP
        Map<String, String> allFiles = new HashMap<>();
        allFiles.putAll(generatedCode);
        allFiles.putAll(generatedTests);
        allFiles.putAll(helperFiles);

        archiveService.createProjectArchive(project.getId(), allFiles);
    }

    private void saveOrUpdateContext(Project project, String fileName, String content, FileType fileType) {
        ProjectContext context = contextRepository.findByProjectIdAndFileName(project.getId(), fileName)
                .orElseGet(() -> ProjectContext.builder().project(project).fileName(fileName).fileType(fileType).build());
        context.setFileContent(content);
        context.setFileType(fileType);
        contextRepository.save(context);
    }

    private void recordStep(Project project, StepName name, StepStatus status, String prompt, String response) {
        AgentStep step = AgentStep.builder()
                .project(project)
                .stepName(name)
                .stepStatus(status)
                .prompt(prompt != null ? prompt : "")
                .response(response)
                .build();
        agentStepRepository.save(step);
    }

    @Override
    public ProjectResponse getProjectStatus(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + projectId));

        List<AgentStepResponse> stepResponses = agentStepRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(s -> AgentStepResponse.builder()
                        .id(s.getId())
                        .stepName(s.getStepName())
                        .status(s.getStepStatus())
                        .prompt(s.getPrompt())
                        .response(s.getResponse())
                        .createdAt(s.getCreatedAt())
                        .build())
                .toList();

        List<McpServerResponse> mcpResponses = project.getMcpServers().stream()
                .map(m -> McpServerResponse.builder()
                        .id(m.getId())
                        .name(m.getName())
                        .serverUrl(m.getServerUrl())
                        .transportType(m.getTransportType())
                        .isActive(m.getIsActive())
                        .build())
                .toList();

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .taskTemplateName(project.getTaskTemplate() != null ? project.getTaskTemplate().getName() : "Custom")
                .createdAt(project.getCreatedAt())
                .steps(stepResponses)
                .mcpServers(mcpResponses)
                .zipArchiveUrl("/projects/" + project.getId() + "/download")
                .build();
    }
}