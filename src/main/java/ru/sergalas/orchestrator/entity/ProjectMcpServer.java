package ru.sergalas.orchestrator.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.sergalas.orchestrator.entity.enums.TransportType;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "project_mcp_servers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMcpServer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "server_url", nullable = false, length = 1000)
    private String serverUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", nullable = false)
    @Builder.Default
    private TransportType transportType = TransportType.SSE;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> config;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}