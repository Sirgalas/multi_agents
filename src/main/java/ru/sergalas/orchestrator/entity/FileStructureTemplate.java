package ru.sergalas.orchestrator.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_structure_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileStructureTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "structure_tree", nullable = false, columnDefinition = "TEXT")
    private String structureTree;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}