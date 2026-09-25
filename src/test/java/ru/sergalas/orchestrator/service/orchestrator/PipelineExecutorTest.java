package ru.sergalas.orchestrator.service.orchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.dto.response.ProjectArchiveResponse;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.ProjectStatus;
import ru.sergalas.orchestrator.entity.enums.ProjectType;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.repository.ProjectRepository;
import ru.sergalas.orchestrator.service.agent.AgentsService;
import ru.sergalas.orchestrator.service.agent.ArchitectService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PipelineExecutorTest {

    @Mock
    private ArchitectService architectService;
    @Mock
    private AgentsService architectAgent;
    @Mock
    private AgentsService backendAnalyst;
    @Mock
    private AgentsService frontendAnalyst;
    @Mock
    private AgentsService designerService;
    @Mock
    private AgentsService backendWorker;
    @Mock
    private AgentsService frontendWorker;
    @Mock
    private AgentsService testerService;
    @Mock
    private AgentsService helperService;
    @Mock
    private AgentsService archiverService;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectContextService contextService;
    @Mock
    private ProjectArchiver projectArchiver;
    @Mock
    private AgentStepRepository agentStepRepository;

    private PipelineExecutor pipelineExecutor;

    private Project project;

    @BeforeEach
    void setUp() {
        project = Project.builder().id(1L).name("Pipeline Project").status(ProjectStatus.DRAFT).build();

        when(backendAnalyst.getStepName()).thenReturn(StepName.BACKEND_ANALYST);
        when(frontendAnalyst.getStepName()).thenReturn(StepName.FRONTEND_ANALYST);
        when(backendAnalyst.isNeedAgents(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            return (p == null || p.getType() != ProjectType.FRONTEND_ONLY) ? Optional.of(backendAnalyst) : Optional.empty();
        });
        when(frontendAnalyst.isNeedAgents(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            return (p == null || p.getType() != ProjectType.BACKEND_ONLY) ? Optional.of(frontendAnalyst) : Optional.empty();
        });
        when(backendAnalyst.isNeedAgents(anyString())).thenAnswer(inv -> {
            String s = inv.getArgument(0);
            return "BACKEND_ANALYST".equalsIgnoreCase(s) ? Optional.of(backendAnalyst) : Optional.empty();
        });
        when(frontendAnalyst.isNeedAgents(anyString())).thenAnswer(inv -> {
            String s = inv.getArgument(0);
            return "FRONTEND_ANALYST".equalsIgnoreCase(s) ? Optional.of(frontendAnalyst) : Optional.empty();
        });
        when(backendAnalyst.isNeedAgents(any(), anyString())).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            String s = inv.getArgument(1);
            return ("BACKEND_ANALYST".equalsIgnoreCase(s) && (p == null || p.getType() != ProjectType.FRONTEND_ONLY))
                    ? Optional.of(backendAnalyst) : Optional.empty();
        });
        when(frontendAnalyst.isNeedAgents(any(), anyString())).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            String s = inv.getArgument(1);
            return ("FRONTEND_ANALYST".equalsIgnoreCase(s) && (p == null || p.getType() != ProjectType.BACKEND_ONLY))
                    ? Optional.of(frontendAnalyst) : Optional.empty();
        });
        when(backendAnalyst.isCompleted(any())).thenReturn(false);
        when(frontendAnalyst.isCompleted(any())).thenReturn(false);
        when(backendAnalyst.isAnalyst()).thenReturn(true);
        when(frontendAnalyst.isAnalyst()).thenReturn(true);

        when(designerService.getStepName()).thenReturn(StepName.DESIGNER);
        when(designerService.isNeedAgents(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            return (p == null || p.getType() != ProjectType.BACKEND_ONLY) ? Optional.of(designerService) : Optional.empty();
        });
        when(designerService.isNeedAgents(anyString())).thenAnswer(inv -> {
            String s = inv.getArgument(0);
            return "DESIGNER".equalsIgnoreCase(s) ? Optional.of(designerService) : Optional.empty();
        });
        when(designerService.isNeedAgents(any(), anyString())).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            String s = inv.getArgument(1);
            return ("DESIGNER".equalsIgnoreCase(s) && (p == null || p.getType() != ProjectType.BACKEND_ONLY))
                    ? Optional.of(designerService) : Optional.empty();
        });
        when(designerService.isCompleted(any())).thenReturn(false);
        when(designerService.isAnalyst()).thenReturn(true);

        when(backendWorker.getStepName()).thenReturn(StepName.BACKEND_DEVELOPER);
        when(frontendWorker.getStepName()).thenReturn(StepName.FRONTEND_DEVELOPER);
        when(backendWorker.isDeveloper()).thenReturn(true);
        when(frontendWorker.isDeveloper()).thenReturn(true);
        when(backendWorker.isNeedAgents(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            return (p == null || p.getType() != ProjectType.FRONTEND_ONLY) ? Optional.of(backendWorker) : Optional.empty();
        });
        when(frontendWorker.isNeedAgents(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            return (p == null || p.getType() != ProjectType.BACKEND_ONLY) ? Optional.of(frontendWorker) : Optional.empty();
        });
        when(backendWorker.isCompleted(any())).thenReturn(false);
        when(frontendWorker.isCompleted(any())).thenReturn(false);

        when(testerService.getStepName()).thenReturn(StepName.TESTER);
        when(helperService.getStepName()).thenReturn(StepName.HELPER);
        when(testerService.isNeedAgents(any(Project.class))).thenReturn(Optional.of(testerService));
        when(helperService.isNeedAgents(any(Project.class))).thenReturn(Optional.of(helperService));
        when(testerService.isCompleted(any())).thenReturn(false);
        when(helperService.isCompleted(any())).thenReturn(false);

        when(archiverService.getStepName()).thenReturn(StepName.ARCHIVER);
        when(archiverService.isNeedAgents(any(Project.class))).thenReturn(Optional.of(archiverService));
        when(archiverService.isCompleted(any())).thenReturn(false);

        when(architectAgent.getStepName()).thenReturn(StepName.ARCHITECT);
        when(architectAgent.isNeedAgents(any(Project.class))).thenReturn(Optional.of(architectAgent));
        when(architectAgent.isCompleted(any())).thenReturn(false);

        pipelineExecutor = new PipelineExecutor(
                architectService,
                List.of(architectAgent, backendAnalyst, frontendAnalyst, designerService, backendWorker, frontendWorker, testerService, helperService, archiverService),
                projectService
        );
    }

    @Test
    @DisplayName("Шаг 1 Архитектор: завершение анализа спецификации переводит проект в статус WAITING_FOR_INPUT и останавливает конвейер до подтверждения")
    void runPipeline_WhenArchitectCompletesSpec_PausesForUserApproval() {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(project);
        when(architectService.getPendingQuestions(1L)).thenReturn(null);
        when(architectAgent.isCompleted(project)).thenReturn(true);

        // Act
        pipelineExecutor.runPipeline(1L);

        // Assert
        verify(architectAgent, never()).work(anyLong());
        verify(backendAnalyst).work(1L);
        verify(frontendAnalyst).work(1L);
        verify(designerService).work(1L);
        verify(projectService).updateStatus(1L, ProjectStatus.WAITING_FOR_INPUT);
        // Downstream development and archiver workers must NEVER be invoked without explicit approval!
        verify(backendWorker, never()).work(anyLong());
        verify(frontendWorker, never()).work(anyLong());
        verify(testerService, never()).work(anyLong());
        verify(helperService, never()).work(anyLong());
        verify(archiverService, never()).work(anyLong());
    }

    @Test
    @DisplayName("Edge Case HITL: при наличии вопросов Архитектора конвейер приостанавливается со статусом WAITING_FOR_INPUT")
    void runPipeline_WhenPendingQuestions_PausesPipeline() {
        // Arrange
        ArchitectQuestionsResponse pendingQuestions = ArchitectQuestionsResponse.builder()
                .questionId(10L)
                .status("PENDING")
                .questions(List.of(Map.of("id", "q1", "text", "Question 1")))
                .build();

        when(projectService.getProjectById(1L)).thenReturn(project);
        when(architectService.getPendingQuestions(1L)).thenReturn(pendingQuestions);

        // Act
        pipelineExecutor.runPipeline(1L);

        // Assert
        verify(architectAgent).work(1L);
        verify(projectService).updateStatus(1L, ProjectStatus.WAITING_FOR_INPUT);

        // Subsequent steps should not execute
        verify(designerService, never()).work(anyLong());
        verify(backendWorker, never()).work(anyLong());
        verify(frontendWorker, never()).work(anyLong());
        verify(testerService, never()).work(anyLong());
        verify(helperService, never()).work(anyLong());
        verify(archiverService, never()).work(anyLong());
    }

    @Test
    @DisplayName("Этап разработки: continueDevelopment запускает Backend, Frontend, Tester, Helper и завершает проект")
    void continueDevelopment_HappyPath_CompletesProject() {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(project);

        // Act
        pipelineExecutor.continueDevelopment(1L);

        // Assert
        verify(projectService).updateStatus(1L, ProjectStatus.IN_PROGRESS);
        verify(backendWorker).work(1L);
        verify(frontendWorker).work(1L);
        verify(testerService).work(1L);
        verify(helperService).work(1L);
        verify(archiverService).work(1L);
    }

    @Test
    @DisplayName("Выборочный запуск: если бэкенд уже сгенерирован, повторный запуск выполняет только Frontend, Tester, Helper")
    void continueDevelopment_WhenBackendAlreadyCompleted_RunsOnlyFrontendAndDownstream() {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(project);
        when(backendWorker.isCompleted(project)).thenReturn(true);

        ProjectContext backendCtx = ProjectContext.builder()
                .fileName("GENERATED_BACKEND_CODE.md")
                .fileType(FileType.CONTEXT_CODE)
                .fileContent("public class BackendApp {}")
                .build();
        when(contextService.getContextByProject(project)).thenReturn(List.of(backendCtx));
        when(agentStepRepository.findFirstByProjectAndStepNameOrderByCreatedAtDesc(project, StepName.BACKEND_DEVELOPER))
                .thenReturn(Optional.of(AgentStep.builder().status(StepStatus.COMPLETED).build()));
        when(projectArchiver.createZipArchive(eq(project), any())).thenReturn(
                ProjectArchiveResponse.builder().archivePath("/output/result.zip").build()
        );

        // Act
        pipelineExecutor.continueDevelopment(1L);

        // Assert
        verify(backendWorker).work(1L);
        verify(frontendWorker).work(1L);
        verify(testerService).work(1L);
        verify(helperService).work(1L);
        verify(archiverService).work(1L);
    }



    @Test
    @DisplayName("Edge Case: при сбое на любом шаге статус проекта переводится в FAILED")
    void runPipeline_WhenExceptionThrown_SetsStatusFailed() {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(project);
        when(architectService.getPendingQuestions(1L)).thenReturn(null);
        when(architectAgent.isCompleted(project)).thenReturn(false);
        doThrow(new RuntimeException("LLM Connection failed")).when(architectAgent).work(1L);

        // Act
        pipelineExecutor.runPipeline(1L);

        // Assert
        verify(projectService).updateStatus(1L, ProjectStatus.FAILED);
    }

    @Test
    @DisplayName("Выборочный запуск: continueFromStep запускает конвейер разработки с указанного шага")
    void continueFromStep_RunsDevelopmentPipeline() {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(project);

        // Act
        pipelineExecutor.continueFromStep(1L, "TESTER");

        // Assert
        verify(projectService).updateStatus(1L, ProjectStatus.IN_PROGRESS);
        verify(backendWorker).work(1L);
        verify(frontendWorker).work(1L);
        verify(testerService).work(1L);
        verify(helperService).work(1L);
        verify(archiverService).work(1L);
    }

    @Test
    @DisplayName("Выборочный запуск: continueFromStep с BACKEND_ANALYST выполняет аналитику бэкенда, затем ожидает подтверждения")
    void continueFromStep_BackendAnalyst_RunsAnalystAndPauses() {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(project);

        // Act
        pipelineExecutor.continueFromStep(1L, "BACKEND_ANALYST");

        // Assert
        verify(backendAnalyst).work(1L);
        verify(frontendAnalyst, never()).work(anyLong());
        verify(designerService, never()).work(anyLong());
        verify(projectService).updateStatus(1L, ProjectStatus.WAITING_FOR_INPUT);
        verify(backendWorker, never()).work(anyLong());
        verify(frontendWorker, never()).work(anyLong());
    }

    @Test
    @DisplayName("Выборочный запуск: continueFromStep с FRONTEND_ANALYST выполняет аналитику фронтенда, затем ожидает подтверждения")
    void continueFromStep_FrontendAnalyst_RunsAnalystAndPauses() {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(project);

        // Act
        pipelineExecutor.continueFromStep(1L, "FRONTEND_ANALYST");

        // Assert
        verify(backendAnalyst, never()).work(anyLong());
        verify(frontendAnalyst).work(1L);
        verify(designerService, never()).work(anyLong());
        verify(projectService).updateStatus(1L, ProjectStatus.WAITING_FOR_INPUT);
        verify(backendWorker, never()).work(anyLong());
        verify(frontendWorker, never()).work(anyLong());
    }

    @Test
    @DisplayName("Тип проекта BACKEND_ONLY: в runPipeline пропускается Frontend Analyst и Designer")
    void runPipeline_WhenBackendOnly_SkipsFrontendAnalyst() {
        // Arrange
        project.setType(ProjectType.BACKEND_ONLY);
        when(projectService.getProjectById(1L)).thenReturn(project);
        when(architectService.getPendingQuestions(1L)).thenReturn(null);
        when(contextService.getLatestContextByType(project, FileType.SPEC))
                .thenReturn(Optional.of(ProjectContext.builder().fileContent("## Architecture Spec").build()));

        // Act
        pipelineExecutor.runPipeline(1L);

        // Assert
        verify(backendAnalyst).work(1L);
        verify(frontendAnalyst, never()).work(anyLong());
        verify(designerService, never()).work(anyLong());
        verify(projectService).updateStatus(1L, ProjectStatus.WAITING_FOR_INPUT);
    }

    @Test
    @DisplayName("Тип проекта FRONTEND_ONLY: в runPipeline пропускается Backend Analyst, но запускаются Frontend Analyst и Designer")
    void runPipeline_WhenFrontendOnly_SkipsBackendAnalyst() {
        // Arrange
        project.setType(ProjectType.FRONTEND_ONLY);
        when(projectService.getProjectById(1L)).thenReturn(project);
        when(architectService.getPendingQuestions(1L)).thenReturn(null);
        when(contextService.getLatestContextByType(project, FileType.SPEC))
                .thenReturn(Optional.of(ProjectContext.builder().fileContent("## Architecture Spec").build()));

        // Act
        pipelineExecutor.runPipeline(1L);

        // Assert
        verify(backendAnalyst, never()).work(anyLong());
        verify(frontendAnalyst).work(1L);
        verify(designerService).work(1L);
        verify(projectService).updateStatus(1L, ProjectStatus.WAITING_FOR_INPUT);
    }

    @Test
    @DisplayName("Тип проекта BACKEND_ONLY: в continueDevelopment пропускается Frontend Developer")
    void continueDevelopment_WhenBackendOnly_SkipsFrontendDeveloper() {
        // Arrange
        project.setType(ProjectType.BACKEND_ONLY);
        when(projectService.getProjectById(1L)).thenReturn(project);
        when(contextService.getContextByProject(project)).thenReturn(Collections.emptyList());
        when(projectArchiver.createZipArchive(any(), any()))
                .thenReturn(ProjectArchiveResponse.builder().archivePath("archive.zip").build());

        // Act
        pipelineExecutor.continueDevelopment(1L);

        // Assert
        verify(backendWorker).work(1L);
        verify(frontendWorker, never()).work(anyLong());
        verify(testerService).work(1L);
        verify(helperService).work(1L);
        verify(archiverService).work(1L);
    }

    @Test
    @DisplayName("Тип проекта FRONTEND_ONLY: в continueDevelopment пропускается Backend Developer")
    void continueDevelopment_WhenFrontendOnly_SkipsBackendDeveloper() {
        // Arrange
        project.setType(ProjectType.FRONTEND_ONLY);
        when(projectService.getProjectById(1L)).thenReturn(project);

        // Act
        pipelineExecutor.continueDevelopment(1L);

        // Assert
        verify(backendWorker, never()).work(anyLong());
        verify(frontendWorker).work(1L);
        verify(testerService).work(1L);
        verify(helperService).work(1L);
        verify(archiverService).work(1L);
    }
}