package ru.sergalas.orchestrator.service.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sergalas.orchestrator.config.properties.McpProperties;
import ru.sergalas.orchestrator.dto.request.CreateProjectRequest;
import ru.sergalas.orchestrator.dto.response.ProjectResponse;
import ru.sergalas.orchestrator.entity.*;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.ProjectStatus;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.TransportType;
import ru.sergalas.orchestrator.exception.ProjectNotFoundException;
import ru.sergalas.orchestrator.repository.*;
import ru.sergalas.orchestrator.service.project.impl.ProjectServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskTemplateRepository taskTemplateRepository;

    @Mock
    private FileStructureTemplateRepository fileStructureTemplateRepository;

    @Mock
    private ProjectContextService projectContextService;

    @Mock
    private ProjectMcpServerRepository projectMcpServerRepository;

    @Mock
    private McpServerRepository mcpServerRepository;

    @Mock
    private AgentStepRepository agentStepRepository;

    @Mock
    private ArchitectQuestionRepository architectQuestionRepository;

    @Mock
    private ProjectContextRepository projectContextRepository;

    @Mock
    private McpProperties mcpProperties;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(10L).username("architect").build();
    }

    @Test
    @DisplayName("Edge Case: создание проекта с привязкой шаблона ТЗ, шаблона структуры и дефолтных MCP серверов")
    void createProject_FullSetup_Success() throws Exception {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Payment Gateway");
        request.setDescription("Microservice");
        request.setTaskContent("# Task Details");
        request.setTaskTemplateId(1L);
        request.setFileStructureTemplateId(2L);
        request.setDefaultMcpServerNames(List.of("Spring Boot Guidelines"));

        TaskTemplate taskTemplate = TaskTemplate.builder().id(1L).name("Template 1").build();
        FileStructureTemplate fst = FileStructureTemplate.builder()
                .id(2L)
                .structureTree(Map.of("type", "directory", "name", "root"))
                .build();

        McpProperties.DefaultMcpServerConfig mcpConfig = new McpProperties.DefaultMcpServerConfig();
        mcpConfig.setName("Spring Boot Guidelines");
        mcpConfig.setUrl("https://context7.com/spring-boot");
        mcpConfig.setTransport(TransportType.SSE);

        when(taskTemplateRepository.findById(1L)).thenReturn(Optional.of(taskTemplate));
        when(fileStructureTemplateRepository.findById(2L)).thenReturn(Optional.of(fst));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"directory\"}");
        when(mcpProperties.getDefaultServers()).thenReturn(List.of(mcpConfig));

        when(projectRepository.save(any(Project.class))).thenAnswer(i -> {
            Project p = i.getArgument(0);
            p.setId(100L);
            return p;
        });

        // Act
        Project result = projectService.createProject(request, user);

        // Assert
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.DRAFT);
        assertThat(result.getTaskTemplate()).isEqualTo(taskTemplate);

        verify(projectContextService).saveFile(
                eq(result),
                eq("TASK.md"),
                eq("/TASK.md"),
                eq("# Task Details"),
                eq(FileType.TASK),
                eq(1)
        );

        verify(projectContextService).saveFile(
                eq(result),
                eq("file_structure.json"),
                eq("/file_structure.json"),
                eq("{\"type\":\"directory\"}"),
                eq(FileType.FILE_STRUCTURE),
                eq(1)
        );

        verify(projectMcpServerRepository).save(any(ProjectMcpServer.class));
    }

    @Test
    @DisplayName("Создание проекта с MCP сервером из БД копирует target и token")
    void createProject_WhenMcpServerInDatabase_UsesDbServerAndSetsTargetAndToken() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Database MCP Project");
        request.setDefaultMcpServerNames(List.of("React Guidelines & Hooks"));

        McpServer dbServer = McpServer.builder()
                .id(1L)
                .name("React Guidelines & Hooks")
                .url("https://context7.com/facebook/react")
                .target(ru.sergalas.orchestrator.entity.enums.McpTarget.FRONTEND)
                .token("test-secret-token")
                .build();

        when(mcpServerRepository.findByNameIgnoreCase("React Guidelines & Hooks")).thenReturn(Optional.of(dbServer));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> {
            Project p = i.getArgument(0);
            p.setId(200L);
            return p;
        });

        // Act
        Project result = projectService.createProject(request, user);

        // Assert
        assertThat(result.getId()).isEqualTo(200L);
        verify(projectMcpServerRepository).save(argThat(mcp ->
                mcp.getName().equals("React Guidelines & Hooks") &&
                mcp.getTarget() == ru.sergalas.orchestrator.entity.enums.McpTarget.FRONTEND &&
                "test-secret-token".equals(mcp.getToken())
        ));
    }

    @Test
    @DisplayName("Edge Case: поиск несуществующего проекта выбрасывает ProjectNotFoundException")
    void getProjectById_NotFound_ThrowsException() {
        // Arrange
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projectService.getProjectById(999L))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("Project not found with id: 999");
    }

    @Test
    @DisplayName("Преобразование Project в DTO ProjectResponse")
    void toResponse_Success() {
        // Arrange
        Project project = Project.builder()
                .id(5L)
                .name("Test Service")
                .description("Desc")
                .status(ProjectStatus.COMPLETED)
                .archivePath("/output/test.zip")
                .build();

        // Act
        ProjectResponse response = projectService.toResponse(project);

        // Assert
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("Test Service");
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(response.getTaskTemplateName()).isEqualTo("Custom");
        assertThat(response.getArchivePath()).isEqualTo("/output/test.zip");
    }

    @Test
    @DisplayName("Сброс проекта: очищает шаги, вопросы, сгенерированные файлы кроме TASK.md и переводит в DRAFT")
    void resetProject_ClearsStepsQuestionsNonTaskContextsAndSetsDraft() {
        // Arrange
        Project project = Project.builder()
                .id(7L)
                .name("Failed Project")
                .status(ProjectStatus.FAILED)
                .archivePath("/output/fake.zip")
                .build();

        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));

        // Act
        projectService.resetProject(7L);

        // Assert
        verify(agentStepRepository).deleteAllByProject(project);
        verify(architectQuestionRepository).deleteAllByProject(project);
        verify(projectContextRepository).deleteAllByProjectAndFileTypeNot(project, FileType.TASK);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.DRAFT);
        assertThat(project.getArchivePath()).isNull();
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("Перезапуск разработки: очищает только шаги Worker/Tester/Helper и сгенерированный код, сохраняя ТЗ и Спецификацию")
    void resetDevelopment_ClearsOnlyDevelopmentStepsAndCodeContexts() {
        // Arrange
        Project project = Project.builder()
                .id(8L)
                .name("Development Project")
                .status(ProjectStatus.COMPLETED)
                .archivePath("/output/fake.zip")
                .build();

        when(projectRepository.findById(8L)).thenReturn(Optional.of(project));

        // Act
        projectService.resetDevelopment(8L);

        // Assert
        verify(agentStepRepository).deleteAllByProjectAndStepNameIn(project, 
                List.of(StepName.BACKEND_DEVELOPER, StepName.FRONTEND_DEVELOPER, StepName.TESTER, StepName.HELPER));
        verify(projectContextRepository).deleteAllByProjectAndFileType(project, FileType.CONTEXT_CODE);
        verify(architectQuestionRepository, never()).deleteAllByProject(any());
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(project.getArchivePath()).isNull();
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("Сброс с шага ARCHITECT: очищает спецификацию, вопросы, шаги и контексты разработки")
    void resetFromStep_WhenArchitect_ResetsAllDownstream() {
        // Arrange
        Project project = Project.builder()
                .id(9L)
                .name("Architect Reset Project")
                .status(ProjectStatus.COMPLETED)
                .archivePath("/output/fake.zip")
                .build();

        when(projectRepository.findById(9L)).thenReturn(Optional.of(project));

        // Act
        projectService.resetFromStep(9L, "ARCHITECT");

        // Assert
        verify(agentStepRepository).deleteAllByProjectAndStepNameIn(project, 
                List.of(StepName.ARCHITECT, StepName.BACKEND_ANALYST, StepName.FRONTEND_ANALYST, StepName.DESIGNER,
                        StepName.BACKEND_DEVELOPER, StepName.FRONTEND_DEVELOPER, StepName.TESTER, StepName.HELPER));
        verify(architectQuestionRepository).deleteAllByProject(project);
        verify(projectContextRepository).deleteAllByProjectAndFileType(project, FileType.SPEC);
        verify(projectContextRepository).deleteAllByProjectAndFileType(project, FileType.CONTEXT_CODE);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(project.getArchivePath()).isNull();
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("Сброс с шага BACKEND_ANALYST: удаляет шаги Backend/Frontend Analyst, Designer и разработки, удаляет BACKEND_SPEC.md, FRONTEND_SPEC.md, DESIGN_TOKENS.json и код")
    void resetFromStep_WhenBackendAnalyst_ResetsBackendAnalystDownstream() {
        // Arrange
        Project project = Project.builder()
                .id(13L)
                .name("Backend Analyst Reset Project")
                .status(ProjectStatus.COMPLETED)
                .archivePath("/output/fake.zip")
                .build();

        when(projectRepository.findById(13L)).thenReturn(Optional.of(project));

        // Act
        projectService.resetFromStep(13L, "BACKEND_ANALYST");

        // Assert
        verify(agentStepRepository).deleteAllByProjectAndStepNameIn(project, 
                List.of(StepName.BACKEND_ANALYST, StepName.FRONTEND_ANALYST, StepName.DESIGNER,
                        StepName.BACKEND_DEVELOPER, StepName.FRONTEND_DEVELOPER, StepName.TESTER, StepName.HELPER));
        verify(projectContextRepository).deleteAllByProjectAndFileNameIn(project, 
                List.of("BACKEND_SPEC.md", "FRONTEND_SPEC.md", "DESIGN_TOKENS.json"));
        verify(projectContextRepository).deleteAllByProjectAndFileType(project, FileType.CONTEXT_CODE);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(project.getArchivePath()).isNull();
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("Сброс с шага FRONTEND_ANALYST: удаляет шаг Frontend Analyst, Designer и разработки, удаляет FRONTEND_SPEC.md, DESIGN_TOKENS.json и код")
    void resetFromStep_WhenFrontendAnalyst_ResetsFrontendAnalystDownstream() {
        // Arrange
        Project project = Project.builder()
                .id(14L)
                .name("Frontend Analyst Reset Project")
                .status(ProjectStatus.COMPLETED)
                .archivePath("/output/fake.zip")
                .build();

        when(projectRepository.findById(14L)).thenReturn(Optional.of(project));

        // Act
        projectService.resetFromStep(14L, "FRONTEND_ANALYST");

        // Assert
        verify(agentStepRepository).deleteAllByProjectAndStepNameIn(project, 
                List.of(StepName.FRONTEND_ANALYST, StepName.DESIGNER,
                        StepName.BACKEND_DEVELOPER, StepName.FRONTEND_DEVELOPER, StepName.TESTER, StepName.HELPER));
        verify(projectContextRepository).deleteAllByProjectAndFileNameIn(project, 
                List.of("FRONTEND_SPEC.md", "DESIGN_TOKENS.json"));
        verify(projectContextRepository).deleteAllByProjectAndFileType(project, FileType.CONTEXT_CODE);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(project.getArchivePath()).isNull();
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("Сброс с шага DESIGNER: удаляет шаг Designer и разработки, удаляет DESIGN_TOKENS.json и код")
    void resetFromStep_WhenDesigner_ResetsDesignerDownstream() {
        // Arrange
        Project project = Project.builder()
                .id(15L)
                .name("Designer Reset Project")
                .status(ProjectStatus.COMPLETED)
                .archivePath("/output/fake.zip")
                .build();

        when(projectRepository.findById(15L)).thenReturn(Optional.of(project));

        // Act
        projectService.resetFromStep(15L, "DESIGNER");

        // Assert
        verify(agentStepRepository).deleteAllByProjectAndStepNameIn(project, 
                List.of(StepName.DESIGNER, 
                        StepName.BACKEND_DEVELOPER, StepName.FRONTEND_DEVELOPER, StepName.TESTER, StepName.HELPER));
        verify(projectContextRepository).deleteAllByProjectAndFileNameIn(project, 
                List.of("DESIGN_TOKENS.json"));
        verify(projectContextRepository).deleteAllByProjectAndFileType(project, FileType.CONTEXT_CODE);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(project.getArchivePath()).isNull();
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("Сброс с шага FRONTEND_DEVELOPER: удаляет только шаги Frontend, Tester, Helper и их контексты, сохраняя бэкенд")
    void resetFromStep_WhenFrontendDeveloper_ResetsOnlyFrontendDownstream() {
        // Arrange
        Project project = Project.builder()
                .id(12L)
                .name("Frontend Reset Project")
                .status(ProjectStatus.COMPLETED)
                .archivePath("/output/fake.zip")
                .build();

        when(projectRepository.findById(12L)).thenReturn(Optional.of(project));

        // Act
        projectService.resetFromStep(12L, "FRONTEND_DEVELOPER");

        // Assert
        verify(agentStepRepository).deleteAllByProjectAndStepNameIn(project, 
                List.of(StepName.FRONTEND_DEVELOPER, StepName.TESTER, StepName.HELPER));
        verify(projectContextRepository).deleteAllByProjectAndFileNameIn(project, 
                List.of("GENERATED_FRONTEND_CODE.md", "GENERATED_TESTS.md", "GENERATED_INFRA.md"));
        verify(architectQuestionRepository, never()).deleteAllByProject(any());
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(project.getArchivePath()).isNull();
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("Сброс с шага TESTER: удаляет тесты и инфраструктуру, сохраняя код и спецификацию")
    void resetFromStep_WhenTester_ResetsOnlyTestsAndInfra() {
        // Arrange
        Project project = Project.builder()
                .id(10L)
                .name("Tester Reset Project")
                .status(ProjectStatus.COMPLETED)
                .archivePath("/output/fake.zip")
                .build();

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        // Act
        projectService.resetFromStep(10L, "TESTER");

        // Assert
        verify(agentStepRepository).deleteAllByProjectAndStepNameIn(project, 
                List.of(StepName.TESTER, StepName.HELPER));
        verify(projectContextRepository).deleteAllByProjectAndFileNameIn(project, 
                List.of("GENERATED_TESTS.md", "GENERATED_INFRA.md"));
        verify(architectQuestionRepository, never()).deleteAllByProject(any());
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(project.getArchivePath()).isNull();
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("Сброс с шага HELPER: удаляет только шаг HELPER и GENERATED_INFRA.md")
    void resetFromStep_WhenHelper_ResetsOnlyInfra() {
        // Arrange
        Project project = Project.builder()
                .id(11L)
                .name("Helper Reset Project")
                .status(ProjectStatus.COMPLETED)
                .archivePath("/output/fake.zip")
                .build();

        when(projectRepository.findById(11L)).thenReturn(Optional.of(project));

        // Act
        projectService.resetFromStep(11L, "HELPER");

        // Assert
        verify(agentStepRepository).deleteAllByProjectAndStepNameIn(project, 
                List.of(StepName.HELPER));
        verify(projectContextRepository).deleteAllByProjectAndFileNameIn(project, 
                List.of("GENERATED_INFRA.md"));
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(project.getArchivePath()).isNull();
        verify(projectRepository).save(project);
    }
}