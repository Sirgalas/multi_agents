package ru.sergalas.orchestrator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.sergalas.orchestrator.dto.request.McpServerRequest;
import ru.sergalas.orchestrator.dto.response.McpResource;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.repository.ProjectMcpServerRepository;
import ru.sergalas.orchestrator.service.McpService;

import java.util.List;

@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpController {

    private final McpService mcpService;
    private final ProjectMcpServerRepository mcpServerRepository;

    @PostMapping("/projects/{projectId}/servers")
    public ResponseEntity<ProjectMcpServer> addServer(
            @PathVariable Long projectId,
            @Valid @RequestBody McpServerRequest request
    ) {
        ProjectMcpServer server = mcpService.addMcpServer(projectId, request);
        return ResponseEntity.ok(server);
    }

    @PatchMapping("/servers/{serverId}/toggle")
    public ResponseEntity<Void> toggleServer(
            @PathVariable Long serverId,
            @RequestParam boolean active
    ) {
        mcpService.toggleMcpServer(serverId, active);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/servers/{serverId}/resources")
    public ResponseEntity<List<McpResource>> getResources(@PathVariable Long serverId) {
        ProjectMcpServer server = mcpServerRepository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("Server not found"));
        return ResponseEntity.ok(mcpService.listResources(server));
    }
}