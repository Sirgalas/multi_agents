package ru.sergalas.orchestrator.service.project;

import ru.sergalas.orchestrator.dto.request.CreateProjectRequest;
import ru.sergalas.orchestrator.dto.response.ProjectResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.User;

import java.util.List;

public interface ProjectService {
    Project createProject(CreateProjectRequest request, User user);
    Project getProjectById(Long id);
    List<Project> getProjectsForUser(User user);
    List<Project> getAllProjects();
    void updateStatus(Long projectId, String status);
    ProjectResponse toResponse(Project project);
    void deleteProject(Long id);
}