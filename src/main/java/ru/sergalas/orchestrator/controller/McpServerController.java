package ru.sergalas.orchestrator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.sergalas.orchestrator.dto.request.AddMcpServerRequest;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.service.mcp.Context7Service;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;

@Controller
@RequestMapping("/projects/{projectId}/mcp")
@RequiredArgsConstructor
public class McpServerController {

    private final Context7Service context7Service;
    private final ProjectService projectService;

    @GetMapping
    public String listMcpServers(@PathVariable Long projectId, Model model) {
        Project project = projectService.getProjectById(projectId);
        List<ProjectMcpServer> servers = context7Service.getServersByProject(projectId);

        model.addAttribute("project", project);
        model.addAttribute("servers", servers);
        model.addAttribute("newServerRequest", new AddMcpServerRequest());
        return "mcp/list";
    }

    @PostMapping
    public String addServer(
            @PathVariable Long projectId,
            @Valid @ModelAttribute("newServerRequest") AddMcpServerRequest request
    ) {
        request.setProjectId(projectId);
        context7Service.registerServer(request);
        return "redirect:/projects/" + projectId + "/mcp";
    }

    @PostMapping("/{serverId}/toggle")
    public String toggleServer(@PathVariable Long projectId, @PathVariable Long serverId) {
        context7Service.toggleActive(serverId);
        return "redirect:/projects/" + projectId + "/mcp";
    }

    @PostMapping("/{serverId}/delete")
    public String deleteServer(@PathVariable Long projectId, @PathVariable Long serverId) {
        context7Service.deleteServer(serverId);
        return "redirect:/projects/" + projectId + "/mcp";
    }
}