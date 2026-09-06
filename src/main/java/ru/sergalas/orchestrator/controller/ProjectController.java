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

        model.addAttribute("project", project);
        model.addAttribute("contexts", contexts);
        model.addAttribute("steps", steps);
        return "project/view";
    }

    @PostMapping("/{id}/generate")
    public String triggerGeneration(@PathVariable Long id) {
        orchestratorService.executePipeline(id);
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