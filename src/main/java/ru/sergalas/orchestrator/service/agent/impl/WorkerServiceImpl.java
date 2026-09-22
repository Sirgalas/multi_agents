package ru.sergalas.orchestrator.service.agent.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.service.agent.AgentsService;
import ru.sergalas.orchestrator.service.agent.WorkerService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerServiceImpl implements WorkerService {

    private final List<AgentsService> agents;

    @Override
    @Transactional
    public void generateSourceCode(Long projectId) {
        log.info("WorkerService delegating to worker agents for project ID: {}", projectId);
        if (agents != null) {
            agents.stream()
                    .filter(AgentsService::isWorker)
                    .forEach(agent -> agent.work(projectId));
        }
    }
}