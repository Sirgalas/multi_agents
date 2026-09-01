package ru.sergalas.orchestrator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.sergalas.orchestrator.dto.request.ProjectContextUploadRequest;
import ru.sergalas.orchestrator.dto.response.ProjectContextResponse;
import ru.sergalas.orchestrator.service.ProjectContextService;

import java.util.List;

@RestController
@RequestMapping("/projects/{id}/context")
@RequiredArgsConstructor
public class ProjectContextController {

    private final ProjectContextService projectContextService;

    @GetMapping
    public ResponseEntity<List<ProjectContextResponse>> getContexts(@PathVariable("id") Long projectId, Authentication auth) {
        return ResponseEntity.ok(projectContextService.getByProject(projectId, auth));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProjectContextResponse> upload(
            @PathVariable("id") Long projectId,
            @Valid @ModelAttribute ProjectContextUploadRequest request,
            Authentication auth
    ) {
        ProjectContextResponse response = projectContextService.upload(projectId, request, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{contextId}")
    public ResponseEntity<Void> delete(
            @PathVariable("id") Long projectId,
            @PathVariable("contextId") Long contextId,
            Authentication auth
    ) {
        projectContextService.delete(contextId, auth);
        return ResponseEntity.noContent().build();
    }
}