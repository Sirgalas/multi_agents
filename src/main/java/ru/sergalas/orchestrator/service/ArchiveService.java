package ru.sergalas.orchestrator.service;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Map;

public interface ArchiveService {
    String createProjectArchive(Long projectId, Map<String, String> files);
    Resource getArchive(Long projectId) throws IOException;
}