package ru.sergalas.orchestrator.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.sergalas.orchestrator.entity.enums.StepName;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_agent_prompts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAgentPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_name", nullable = false)
    private StepName stepName;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "agent_prompt_id", nullable = false)
    private AgentPrompt agentPrompt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
