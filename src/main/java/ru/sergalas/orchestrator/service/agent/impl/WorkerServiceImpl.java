package ru.sergalas.orchestrator.service.agent.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sergalas.orchestrator.service.agent.BackendWorkerService;
import ru.sergalas.orchestrator.service.agent.FrontendWorkerService;
import ru.sergalas.orchestrator.service.agent.WorkerService;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerServiceImpl implements WorkerService {

    private final BackendWorkerService backendWorkerService;
    private final FrontendWorkerService frontendWorkerService;

    @Override
    @Transactional
    public void generateSourceCode(Long projectId) {
        log.info("WorkerService delegating to BackendWorkerService and FrontendWorkerService for project ID: {}", projectId);
        backendWorkerService.generateBackendCode(projectId);
        frontendWorkerService.generateFrontendCode(projectId);
    }
}