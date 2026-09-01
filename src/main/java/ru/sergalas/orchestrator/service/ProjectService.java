package ru.sergalas.orchestrator.service;

import org.springframework.security.core.Authentication;
import ru.sergalas.orchestrator.dto.request.ProjectCreateRequest;
import ru.sergalas.orchestrator.dto.response.ProjectResponse;
import ru.sergalas.orchestrator.entity.Project;

import java.util.List;

public interface ProjectService {
    List<ProjectResponse> getProjectsForUser(Authentication auth);
    ProjectResponse getProjectById(Long projectId, Authentication auth);
    Project getProjectEntity(Long projectId, Authentication auth);
    ProjectResponse createProject(ProjectCreateRequest request, Authentication auth);
    void deleteProject(Long projectId, Authentication auth);
}