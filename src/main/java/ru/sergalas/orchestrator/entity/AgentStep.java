package ru.sergalas.orchestrator.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.sergalas.orchestrator.entity.enums.StepName;
import ru.sergalas.orchestrator.entity.enums.StepStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_name", nullable = false, length = 50)
    private StepName stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_status", nullable = false, length = 50)
    @Builder.Default
    private StepStatus stepStatus = StepStatus.PENDING;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}