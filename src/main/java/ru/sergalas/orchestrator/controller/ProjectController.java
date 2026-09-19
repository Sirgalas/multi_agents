package ru.sergalas.orchestrator.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.sergalas.orchestrator.entity.AgentStep;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.FileType;
import ru.sergalas.orchestrator.entity.enums.ProjectStatus;
import ru.sergalas.orchestrator.repository.AgentStepRepository;
import ru.sergalas.orchestrator.service.orchestrator.OrchestratorService;
import ru.sergalas.orchestrator.service.project.ProjectContextService;
import ru.sergalas.orchestrator.service.project.ProjectService;
import ru.sergalas.orchestrator.service.user.UserService;

import java.io.File;
import java.util.List;

@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;
    private final ProjectContextService projectContextService;
    private final AgentStepRepository agentStepRepository;
    private final OrchestratorService orchestratorService;

    @GetMapping
    public String listProjects(Model model) {
        User user = userService.getCurrentUser();
        List<Project> projects = projectService.getProjectsForUser(user);
        model.addAttribute("projects", projects);
        return "project/list";
    }

    @GetMapping("/{id}")
    public String viewProject(@PathVariable Long id, Model model) {
        Project project = projectService.getProjectById(id);
        List<ProjectContext> contexts = projectContextService.getContextByProject(project);
        List<AgentStep> steps = agentStepRepository.findAllByProjectOrderByCreatedAtAsc(project);

        boolean hasSpec = contexts.stream().anyMatch(c -> 
                ("ARCHITECTURE_SPEC.md".equals(c.getFileName()) || c.getFileType() == FileType.SPEC)
                && c.getFileContent() != null && !c.getFileContent().isBlank());
        boolean hasBackendSpec = contexts.stream().anyMatch(c -> 
                "BACKEND_SPEC.md".equals(c.getFileName())
                && c.getFileContent() != null && !c.getFileContent().isBlank());
        boolean hasFrontendSpec = contexts.stream().anyMatch(c -> 
                "FRONTEND_SPEC.md".equals(c.getFileName())
                && c.getFileContent() != null && !c.getFileContent().isBlank());
        boolean hasBackendCode = contexts.stream().anyMatch(c -> 
                ("GENERATED_BACKEND_CODE.md".equals(c.getFileName()) || "GENERATED_CODE.md".equals(c.getFileName()))
                && c.getFileContent() != null && !c.getFileContent().isBlank());
        boolean hasWorkerCode = contexts.stream().anyMatch(c -> 
                ("GENERATED_CODE.md".equals(c.getFileName()) || "GENERATED_BACKEND_CODE.md".equals(c.getFileName()) || "GENERATED_FRONTEND_CODE.md".equals(c.getFileName()))
                && c.getFileContent() != null && !c.getFileContent().isBlank());
        boolean canApproveSpec = hasSpec && !hasWorkerCode && project.getStatus() != ProjectStatus.IN_PROGRESS;

        model.addAttribute("project", project);
        model.addAttribute("contexts", contexts);
        model.addAttribute("steps", steps);
        model.addAttribute("hasSpec", hasSpec);
        model.addAttribute("hasBackendSpec", hasBackendSpec);
        model.addAttribute("hasFrontendSpec", hasFrontendSpec);
        model.addAttribute("hasBackendCode", hasBackendCode);
        model.addAttribute("hasWorkerCode", hasWorkerCode);
        model.addAttribute("canApproveSpec", canApproveSpec);
        return "project/view";
    }

    @PostMapping("/{id}/generate")
    public String triggerGeneration(
            @PathVariable Long id,
            @RequestParam(required = false) String fromStep
    ) {
        orchestratorService.executePipeline(id, fromStep);
        return "redirect:/projects/" + id;
    }

    @PostMapping("/{id}/approve-spec")
    public String approveSpecification(@PathVariable Long id) {
        orchestratorService.approveSpecAndContinue(id);
        return "redirect:/projects/" + id;
    }

    @PostMapping("/{id}/restart-development")
    public String restartDevelopment(@PathVariable Long id) {
        orchestratorService.restartDevelopment(id);
        return "redirect:/projects/" + id;
    }

    @PostMapping("/{id}/reset")
    public String resetProject(@PathVariable Long id) {
        projectService.resetProject(id);
        return "redirect:/projects/" + id;
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadArchive(@PathVariable Long id) {
        Project project = projectService.getProjectById(id);
        if (project.getArchivePath() == null) {
            orchestratorService.archiveProject(id);
            project = projectService.getProjectById(id);
        }

        File file = new File(project.getArchivePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}