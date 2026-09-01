package ru.sergalas.orchestrator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.sergalas.orchestrator.dto.request.McpServerRequest;
import ru.sergalas.orchestrator.dto.response.McpServerResponse;
import ru.sergalas.orchestrator.service.McpServerService;

import java.util.List;

@RestController
@RequestMapping("/projects/{id}/mcp-servers")
@RequiredArgsConstructor
public class McpServerController {

    private final McpServerService mcpServerService;

    @GetMapping
    public ResponseEntity<List<McpServerResponse>> list(@PathVariable("id") Long projectId, Authentication auth) {
        return ResponseEntity.ok(mcpServerService.getByProject(projectId, auth));
    }

    @PostMapping
    public ResponseEntity<McpServerResponse> create(
            @PathVariable("id") Long projectId,
            @Valid @RequestBody McpServerRequest request,
            Authentication auth
    ) {
        McpServerResponse response = mcpServerService.create(projectId, request, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{serverId}")
    public ResponseEntity<McpServerResponse> update(
            @PathVariable("id") Long projectId,
            @PathVariable("serverId") Long serverId,
            @Valid @RequestBody McpServerRequest request,
            Authentication auth
    ) {
        McpServerResponse response = mcpServerService.update(serverId, request, auth);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{serverId}")
    public ResponseEntity<Void> delete(
            @PathVariable("id") Long projectId,
            @PathVariable("serverId") Long serverId,
            Authentication auth
    ) {
        mcpServerService.delete(serverId, auth);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{serverId}/toggle")
    public ResponseEntity<Void> toggle(
            @PathVariable("id") Long projectId,
            @PathVariable("serverId") Long serverId,
            Authentication auth
    ) {
        mcpServerService.toggleActive(serverId, auth);
        return ResponseEntity.ok().build();
    }
}