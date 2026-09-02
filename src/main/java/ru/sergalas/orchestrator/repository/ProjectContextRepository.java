package ru.sergalas.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectContextRepository extends JpaRepository<ProjectContext, Long> {
    List<ProjectContext> findByProjectId(Long projectId);
    Optional<ProjectContext> findByProjectIdAndFileName(Long projectId, String fileName);
    List<ProjectContext> findByProjectIdAndFileType(Long projectId, FileType fileType);
}