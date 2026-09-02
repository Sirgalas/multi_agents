package ru.sergalas.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;

import java.util.List;

@Repository
public interface ProjectMcpServerRepository extends JpaRepository<ProjectMcpServer, Long> {
    List<ProjectMcpServer> findByProjectId(Long projectId);
    List<ProjectMcpServer> findByProjectIdAndIsActiveTrue(Long projectId);
}