package ru.sergalas.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sergalas.orchestrator.entity.McpServer;
import ru.sergalas.orchestrator.entity.enums.McpTarget;

import java.util.List;
import java.util.Optional;

@Repository
public interface McpServerRepository extends JpaRepository<McpServer, Long> {
    List<McpServer> findAllByOrderByCreatedAtDesc();
    List<McpServer> findAllByTarget(McpTarget target);
    Optional<McpServer> findByNameIgnoreCase(String name);
    boolean existsByName(String name);
}
