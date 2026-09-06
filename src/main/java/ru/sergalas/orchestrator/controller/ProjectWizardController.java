package ru.sergalas.orchestrator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.sergalas.orchestrator.config.properties.McpProperties;
import ru.sergalas.orchestrator.dto.request.CreateProjectRequest;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.service.project.FileStructureService;
import ru.sergalas.orchestrator.service.project.ProjectService;
import ru.sergalas.orchestrator.service.project.TaskTemplateService;
import ru.sergalas.orchestrator.service.user.UserService;

@Controller
@RequestMapping("/projects/new")
@RequiredArgsConstructor
public class ProjectWizardController {

    private final ProjectService projectService;
    private final TaskTemplateService taskTemplateService;
    private final FileStructureService fileStructureService;
    private final McpProperties mcpProperties;
    private final UserService userService;

    @GetMapping
    public String showWizard(Model model) {
        model.addAttribute("projectRequest", new CreateProjectRequest());
        model.addAttribute("taskTemplates", taskTemplateService.getAllTemplates());
        model.addAttribute("structureTemplates", fileStructureService.getAllTemplates());
        model.addAttribute("defaultMcpServers", mcpProperties.getDefaultServers());
        return "project/wizard";
    }

    @PostMapping
    public String submitWizard(
            @Valid @ModelAttribute("projectRequest") CreateProjectRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("taskTemplates", taskTemplateService.getAllTemplates());
            model.addAttribute("structureTemplates", fileStructureService.getAllTemplates());
            model.addAttribute("defaultMcpServers", mcpProperties.getDefaultServers());
            return "project/wizard";
        }

        User user = userService.getCurrentUser();
        Project createdProject = projectService.createProject(request, user);
        return "redirect:/projects/" + createdProject.getId();
    }
}