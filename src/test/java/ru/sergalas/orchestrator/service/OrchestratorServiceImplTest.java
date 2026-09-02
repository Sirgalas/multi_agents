package ru.sergalas.orchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.dto.request.ArchitectQuestionResponse;
import ru.sergalas.orchestrator.dto.request.ProjectCreateRequest;
import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.dto.response.ArchitectSpecification;
import ru.sergalas.orchestrator.dto.response.GenerationResultResponse;
import ru.sergalas.orchestrator.dto.response.ProjectResponse;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: OrchestratorServiceImpl Multi-Agent Pipeline")
class OrchestratorServiceImplTest {

    @Mock private ProjectService projectService;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectContextRepository contextRepository;
    @Mock private AgentStepRepository agentStepRepository;
    @Mock private ArchitectAgent architectAgent;
    @Mock private WorkerAgent workerAgent;
    @Mock private TesterAgent testerAgent;
    @Mock private HelperAgent helperAgent;
    @Mock private ArchiveService archiveService;

    @InjectMocks
    private OrchestratorServiceImpl orchestratorService;

    @Test
    @DisplayName("startGeneration stops early and returns questions when Architect needs clarifications")
    void testStartGenerationWithClarifyingQuestions() {
        Project project = Project.builder().id(100L).name("AI Project").build();
        ProjectCreateRequest request = ProjectCreateRequest.builder().name("AI Project").taskContent("Ambiguous task").build();

        when(projectService.createProject(request, "admin")).thenReturn(project);
        when(contextRepository.findByProjectId(100L)).thenReturn(Collections.emptyList());

        ArchitectQuestionsResponse questions = ArchitectQuestionsResponse.builder().projectId(100L).build();
        when(architectAgent.analyze(eq(100L), any(), any(), any(), any())).thenReturn(Either.left(questions));

        Either<ArchitectQuestionsResponse, Long> result = orchestratorService.startGeneration(request, "admin");

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft().getProjectId()).isEqualTo(100L);

        verifyNoInteractions(workerAgent, testerAgent, helperAgent, archiveService);
    }

    @Test
    @DisplayName("startGeneration executes full agent pipeline (Architect -> Worker -> Tester -> Helper -> ZIP)")
    void testStartGenerationFullPipeline() {
        Project project = Project.builder().id(200L).name("Full Pipeline Project").build();
        ProjectCreateRequest request = ProjectCreateRequest.builder().name("Full Pipeline Project").taskContent("Clear task").build();

        when(projectService.createProject(request, "admin")).thenReturn(project);
        when(contextRepository.findByProjectId(200L)).thenReturn(Collections.emptyList());

        ArchitectSpecification spec = ArchitectSpecification.builder()
                .architectureSpec("Full Spec")
                .structureTree("Tree")
                .build();
        when(architectAgent.analyze(eq(200L), any(), any(), any(), any())).thenReturn(Either.right(spec));

        when(workerAgent.generateCode(eq(spec), any())).thenReturn(Map.of("Service.java", "class Service {}"));
        when(testerAgent.generateTests(any(), eq(spec))).thenReturn(Map.of("ServiceTest.java", "class ServiceTest {}"));
        when(helperAgent.generateConfig(eq(spec), any())).thenReturn(Map.of("Dockerfile", "FROM openjdk"));

        Either<ArchitectQuestionsResponse, Long> result = orchestratorService.startGeneration(request, "admin");

        assertThat(result.isRight()).isTrue();
        assertThat(result.getRight()).isEqualTo(200L);

        verify(workerAgent).generateCode(eq(spec), any());
        verify(testerAgent).generateTests(any(), eq(spec));
        verify(helperAgent).generateConfig(eq(spec), any());
        verify(archiveService).createProjectArchive(eq(200L), anyMap());
    }

    @Test
    @DisplayName("continueGeneration finalizes specification and executes generation pipeline")
    void testContinueGeneration() {
        Project project = Project.builder().id(300L).name("Resumed Project").build();
        when(projectRepository.findById(300L)).thenReturn(Optional.of(project));

        ProjectContext taskContext = ProjectContext.builder().project(project).fileName("TASK.md").fileContent("Task text").build();
        when(contextRepository.findByProjectIdAndFileName(300L, "TASK.md")).thenReturn(Optional.of(taskContext));

        ArchitectSpecification spec = ArchitectSpecification.builder().architectureSpec("Final Spec").build();
        when(architectAgent.finalizeSpec(eq("Task text"), any(), any(), any())).thenReturn(spec);

        when(workerAgent.generateCode(any(), any())).thenReturn(Map.of("App.java", "class App{}"));
        when(testerAgent.generateTests(any(), any())).thenReturn(Map.of("AppTest.java", "class AppTest{}"));
        when(helperAgent.generateConfig(any(), any())).thenReturn(Map.of("build.gradle", "apply plugin: 'java'"));

        when(contextRepository.findByProjectId(300L)).thenReturn(List.of(
                ProjectContext.builder().fileName("App.java").fileContent("line 1\nline 2").build()
        ));

        ArchitectQuestionResponse answers = ArchitectQuestionResponse.builder().projectId(300L).answers(Map.of("q1", "a1")).build();

        GenerationResultResponse response = orchestratorService.continueGeneration(answers);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getProjectId()).isEqualTo(300L);
        assertThat(response.getFiles()).hasSize(1);
    }

    @Test
    @DisplayName("getProjectStatus maps project details and chronological steps")
    void testGetProjectStatus() {
        Project project = Project.builder().id(50L).name("P50").createdAt(LocalDateTime.now()).build();
        when(projectRepository.findById(50L)).thenReturn(Optional.of(project));

        List<AgentStep> steps = List.of(
                AgentStep.builder().id(1L).stepName(StepName.ARCHITECT).stepStatus(StepStatus.COMPLETED).build(),
                AgentStep.builder().id(2L).stepName(StepName.WORKER).stepStatus(StepStatus.IN_PROGRESS).build()
        );
        when(agentStepRepository.findByProjectIdOrderByCreatedAtAsc(50L)).thenReturn(steps);

        ProjectResponse status = orchestratorService.getProjectStatus(50L);

        assertThat(status.getId()).isEqualTo(50L);
        assertThat(status.getName()).isEqualTo("P50");
        assertThat(status.getSteps()).hasSize(2);
        assertThat(status.getSteps().get(0).getStepName()).isEqualTo(StepName.ARCHITECT);
    }
}