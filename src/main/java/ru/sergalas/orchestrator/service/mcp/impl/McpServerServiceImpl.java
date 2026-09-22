package ru.sergalas.orchestrator.service.mcp.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.dto.request.CreateMcpServerRequest;
import ru.sergalas.orchestrator.entity.McpServer;
import ru.sergalas.orchestrator.entity.enums.McpTarget;
import ru.sergalas.orchestrator.repository.McpServerRepository;
import ru.sergalas.orchestrator.service.mcp.McpServerService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerServiceImpl implements McpServerService {

    private final McpServerRepository mcpServerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<McpServer> getAllServers() {
        return mcpServerRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpServer> getServersByTarget(McpTarget target) {
        return mcpServerRepository.findAllByTarget(target);
    }

    @Override
    @Transactional
    public McpServer createServer(CreateMcpServerRequest request) {
        String trimmedName = request.getName().trim();
        if (mcpServerRepository.findByNameIgnoreCase(trimmedName).isPresent()) {
            throw new IllegalArgumentException("MCP сервер с именем '" + trimmedName + "' уже существует");
        }

        McpServer server = McpServer.builder()
                .name(trimmedName)
                .url(request.getUrl().trim())
                .target(request.getTarget() != null ? request.getTarget() : McpTarget.COMMON)
                .token(request.getToken() != null && !request.getToken().isBlank() ? request.getToken().trim() : null)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .build();

        McpServer saved = mcpServerRepository.save(server);
        log.info("Registered new MCP server: id={}, name='{}', target={}", saved.getId(), saved.getName(), saved.getTarget());
        return saved;
    }

    @Override
    @Transactional
    public void deleteServer(Long id) {
        mcpServerRepository.deleteById(id);
        log.info("Deleted MCP server id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public McpServer getServerById(Long id) {
        return mcpServerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MCP сервер с id=" + id + " не найден"));
    }
}
