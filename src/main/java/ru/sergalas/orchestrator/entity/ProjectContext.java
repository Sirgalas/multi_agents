package ru.sergalas.orchestrator.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.sergalas.orchestrator.entity.enums.FileType;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_contexts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_content", nullable = false, columnDefinition = "TEXT")
    private String fileContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 50)
    private FileType fileType;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}