package ru.sergalas.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sergalas.orchestrator.entity.McpServer;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;

import java.util.List;

@Repository
public interface ProjectMcpServerRepository extends JpaRepository<ProjectMcpServer, Long> {
    List<ProjectMcpServer> findAllByProject(Project project);
    List<ProjectMcpServer> findAllByProjectAndIsActiveTrue(Project project);
    boolean existsByProjectAndMcpServer(Project project, McpServer mcpServer);
    boolean existsByProjectAndMcpServerId(Project project, Long mcpServerId);
    boolean existsByProjectAndMcpServer_Name(Project project, String name);

    default boolean existsByProjectAndName(Project project, String name) {
        return existsByProjectAndMcpServer_Name(project, name);
    }
}