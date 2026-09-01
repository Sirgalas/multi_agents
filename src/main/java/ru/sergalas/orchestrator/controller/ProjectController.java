package ru.sergalas.orchestrator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.sergalas.orchestrator.dto.request.ProjectCreateRequest;
import ru.sergalas.orchestrator.dto.response.ProjectResponse;
import ru.sergalas.orchestrator.service.ProjectService;

import java.util.List;

@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public String listProjects(Model model, Authentication auth) {
        List<ProjectResponse> projects = projectService.getProjectsForUser(auth);
        model.addAttribute("projects", projects);
        return "projects/list";
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<ProjectResponse>> listProjectsApi(Authentication auth) {
        return ResponseEntity.ok(projectService.getProjectsForUser(auth));
    }

    @PostMapping
    public String createProjectForm(@Valid @ModelAttribute ProjectCreateRequest request, Authentication auth) {
        projectService.createProject(request, auth);
        return "redirect:/projects";
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<ProjectResponse> createProjectApi(@Valid @RequestBody ProjectCreateRequest request, Authentication auth) {
        ProjectResponse response = projectService.createProject(request, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public String viewProject(@PathVariable("id") Long id, Model model, Authentication auth) {
        ProjectResponse project = projectService.getProjectById(id, auth);
        model.addAttribute("project", project);
        return "projects/view";
    }

    @GetMapping("/{id}/api")
    @ResponseBody
    public ResponseEntity<ProjectResponse> viewProjectApi(@PathVariable("id") Long id, Authentication auth) {
        return ResponseEntity.ok(projectService.getProjectById(id, auth));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteProject(@PathVariable("id") Long id, Authentication auth) {
        projectService.deleteProject(id, auth);
        return ResponseEntity.noContent().build();
    }
}