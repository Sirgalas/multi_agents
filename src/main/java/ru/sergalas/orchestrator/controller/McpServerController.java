package ru.sergalas.orchestrator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.sergalas.orchestrator.dto.request.AddMcpServerRequest;
import ru.sergalas.orchestrator.entity.McpServer;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.repository.McpServerRepository;
import ru.sergalas.orchestrator.service.mcp.ProjectMcpServerService;
import ru.sergalas.orchestrator.service.project.ProjectService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/projects/{projectId}/mcp")
@RequiredArgsConstructor
public class McpServerController {

    private final ProjectMcpServerService context7Service;
    private final McpServerRepository mcpServerRepository;
    private final ProjectService projectService;

    @GetMapping
    public String listMcpServers(@PathVariable Long projectId, Model model) {
        Project project = projectService.getProjectById(projectId);
        List<ProjectMcpServer> servers = context7Service.getServersByProject(projectId);
        Set<Long> connectedIds = servers.stream()
                .filter(s -> s.getMcpServer() != null)
                .map(s -> s.getMcpServer().getId())
                .collect(Collectors.toSet());

        List<McpServer> availableCatalogServers = mcpServerRepository.findAll().stream()
                .filter(s -> !connectedIds.contains(s.getId()))
                .toList();

        model.addAttribute("project", project);
        model.addAttribute("servers", servers);
        model.addAttribute("availableCatalogServers", availableCatalogServers);
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