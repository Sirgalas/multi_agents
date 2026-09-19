package ru.sergalas.orchestrator.service.project;

import ru.sergalas.orchestrator.dto.request.CreateProjectRequest;
import ru.sergalas.orchestrator.dto.response.ProjectResponse;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.User;
import ru.sergalas.orchestrator.entity.enums.ProjectStatus;

import java.util.List;

public interface ProjectService {
    Project createProject(CreateProjectRequest request, User user);
    Project getProjectById(Long id);
    List<Project> getProjectsForUser(User user);
    List<Project> getAllProjects();
    void updateStatus(Long projectId, ProjectStatus status);
    ProjectResponse toResponse(Project project);
    void deleteProject(Long id);
    void resetProject(Long id);
    void resetDevelopment(Long id);
    void resetFromStep(Long id, String stepName);
}