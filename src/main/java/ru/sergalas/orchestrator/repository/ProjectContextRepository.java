package ru.sergalas.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sergalas.orchestrator.entity.Project;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.enums.FileType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectContextRepository extends JpaRepository<ProjectContext, Long> {
    List<ProjectContext> findAllByProjectOrderByCreatedAtAsc(Project project);
    List<ProjectContext> findAllByProjectAndFileType(Project project, FileType fileType);
    Optional<ProjectContext> findFirstByProjectAndFileTypeOrderByIterationDesc(Project project, FileType fileType);
    Optional<ProjectContext> findFirstByProjectAndFileName(Project project, String fileName);
    void deleteAllByProjectAndFileTypeNot(Project project, FileType fileType);
    void deleteAllByProjectAndFileType(Project project, FileType fileType);
    void deleteAllByProjectAndFileNameIn(Project project, java.util.Collection<String> fileNames);
}