package ru.sergalas.orchestrator.service;

import ru.sergalas.orchestrator.dto.request.AddMcpServerRequest;
import ru.sergalas.orchestrator.model.ProjectMcpServer;

import java.util.List;

public interface ProjectMcpServerService {
    ProjectMcpServer addServer(Long projectId, Long userId, AddMcpServerRequest req);
    List<ProjectMcpServer> getActiveServers(Long projectId);
    List<ProjectMcpServer> getAllServers(Long projectId);
    ProjectMcpServer toggleActive(Long serverId, Long projectId, Long userId);
    void deleteServer(Long serverId, Long projectId, Long userId);
}