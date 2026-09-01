package ru.sergalas.orchestrator.exception;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(String message) {
        super(message);
    }

    public ProjectNotFoundException(Long projectId) {
        super("Project with id " + projectId + " not found");
    }
}