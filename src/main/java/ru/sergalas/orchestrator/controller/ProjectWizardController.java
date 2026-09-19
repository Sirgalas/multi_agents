package ru.sergalas.orchestrator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.sergalas.orchestrator.config.properties.McpProperties;
import ru.sergalas.orchestrator.dto.request.CreateProjectRequest;
import ru.sergalas.orchestrator.entity.McpServer;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.McpTarget;
import ru.sergalas.orchestrator.service.mcp.McpServerService;
import ru.sergalas.orchestrator.service.project.FileStructureService;
import ru.sergalas.orchestrator.service.project.ProjectService;
import ru.sergalas.orchestrator.service.project.TaskTemplateService;
import ru.sergalas.orchestrator.service.user.UserService;

import ru.sergalas.orchestrator.entity.AgentPrompt;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.service.prompt.AgentPromptService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/projects/new")
@RequiredArgsConstructor
public class ProjectWizardController {

    private final ProjectService projectService;
    private final TaskTemplateService taskTemplateService;
    private final FileStructureService fileStructureService;
    private final McpProperties mcpProperties;
    private final McpServerService mcpServerService;
    private final UserService userService;
    private final AgentPromptService agentPromptService;

    @GetMapping
    public String showWizard(Model model) {
        model.addAttribute("projectRequest", new CreateProjectRequest());
        model.addAttribute("taskTemplates", taskTemplateService.getAllTemplates());
        model.addAttribute("structureTemplates", fileStructureService.getAllTemplates());
        populateMcpServers(model);
        populateAgentPrompts(model);
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
            populateMcpServers(model);
            populateAgentPrompts(model);
            return "project/wizard";
        }

        User user = userService.getCurrentUser();
        Project createdProject = projectService.createProject(request, user);
        return "redirect:/projects/" + createdProject.getId();
    }

    private void populateMcpServers(Model model) {
        List<McpServer> allServers = mcpServerService.getAllServers();
        if (allServers.isEmpty() && mcpProperties != null && mcpProperties.getDefaultServers() != null) {
            allServers = mcpProperties.getDefaultServers().stream()
                    .map(cfg -> McpServer.builder()
                            .name(cfg.getName())
                            .url(cfg.getUrl())
                            .target(cfg.getTarget() != null ? cfg.getTarget() : McpTarget.COMMON)
                            .build())
                    .toList();
        }
        model.addAttribute("defaultMcpServers", allServers);
        model.addAttribute("backendMcpServers", allServers.stream()
                .filter(s -> s.getTarget() == McpTarget.BACKEND)
                .toList());
        model.addAttribute("frontendMcpServers", allServers.stream()
                .filter(s -> s.getTarget() == McpTarget.FRONTEND)
                .toList());
        model.addAttribute("commonMcpServers", allServers.stream()
                .filter(s -> s.getTarget() == McpTarget.COMMON)
                .toList());
    }

    private void populateAgentPrompts(Model model) {
        List<AgentPrompt> allPrompts = agentPromptService != null ? agentPromptService.getAllPrompts() : List.of();
        Map<String, List<AgentPrompt>> promptsByStep = allPrompts.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsFinal()))
                .collect(Collectors.groupingBy(p -> p.getStepName().name()));
        model.addAttribute("promptsByStep", promptsByStep);
        model.addAttribute("allPrompts", allPrompts);
    }
}