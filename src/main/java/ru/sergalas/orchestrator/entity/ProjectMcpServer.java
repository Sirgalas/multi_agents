package ru.sergalas.orchestrator.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.sergalas.orchestrator.entity.enums.McpTarget;

import java.time.LocalDateTime;

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
    
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "mcp_server_id", nullable = false)
    private McpServer mcpServer;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Convenience delegating getters to keep seamless compatibility
    public String getName() {
        return mcpServer != null ? mcpServer.getName() : null;
    }

    public String getServerUrl() {
        return mcpServer != null ? mcpServer.getUrl() : null;
    }

    public String getUrl() {
        return mcpServer != null ? mcpServer.getUrl() : null;
    }

    public McpTarget getTarget() {
        return mcpServer != null ? mcpServer.getTarget() : null;
    }

    public String getToken() {
        return mcpServer != null ? mcpServer.getToken() : null;
    }

    public String getDescription() {
        return mcpServer != null ? mcpServer.getDescription() : null;
    }
}