package ru.sergalas.orchestrator.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.sergalas.orchestrator.dto.request.ArchitectQuestionResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.repository.ProjectContextRepository;
import ru.sergalas.orchestrator.service.ArchiveService;
import ru.sergalas.orchestrator.service.OrchestratorService;
import ru.sergalas.orchestrator.service.ProjectService;
import ru.sergalas.orchestrator.service.UserService;

import java.io.IOException;

@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;
    private final OrchestratorService orchestratorService;
    private final ArchiveService archiveService;
    private final ProjectContextRepository contextRepository;

    @GetMapping
    public String listProjects(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("projects", projectService.findByUser(user.getId()));
        return "project/list";
    }

    @GetMapping("/{id}")
    public String viewProject(@PathVariable Long id, Model model) {
        Project project = projectService.findById(id);
        model.addAttribute("project", project);
        model.addAttribute("steps", project.getSteps());
        model.addAttribute("contexts", contextRepository.findByProjectId(id));
        model.addAttribute("status", orchestratorService.getProjectStatus(id));
        return "project/view";
    }

    @PostMapping("/{id}/continue")
    public String continueProjectGeneration(@PathVariable Long id, @ModelAttribute ArchitectQuestionResponse questionResponse) {
        questionResponse.setProjectId(id);
        orchestratorService.continueGeneration(questionResponse);
        return "redirect:/projects/" + id;
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadProjectZip(@PathVariable Long id) throws IOException {
        Resource archive = archiveService.getArchive(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"project_" + id + ".zip\"")
                .body(archive);
    }

    @PostMapping("/{id}/delete")
    public String deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return "redirect:/projects";
    }
}