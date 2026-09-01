package ru.sergalas.orchestrator.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import ru.sergalas.orchestrator.model.ProjectContext;
import ru.sergalas.orchestrator.model.User;
import ru.sergalas.orchestrator.model.enums.FileType;
import ru.sergalas.orchestrator.service.FileStorageService;
import ru.sergalas.orchestrator.service.ProjectService;
import ru.sergalas.orchestrator.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/projects/{projectId}/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final ProjectService projectService;
    private final UserService userService;

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> uploadFiles(
            @PathVariable("projectId") Long projectId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "fileType", defaultValue = "TASK") FileType fileType,
            @AuthenticationPrincipal UserDetails userDetails) {

        validateOwnership(projectId, userDetails);

        List<Map<String, Object>> uploadedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                ProjectContext saved = fileStorageService.store(projectId, file, fileType);
                uploadedFiles.add(Map.of(
                        "id", saved.getId(),
                        "fileName", saved.getFileName(),
                        "fileType", saved.getFileType().name()
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "uploadedCount", uploadedFiles.size(),
                "files", uploadedFiles
        ));
    }

    @DeleteMapping("/{fileId}")
    @ResponseBody
    public ResponseEntity<?> deleteFile(
            @PathVariable("projectId") Long projectId,
            @PathVariable("fileId") Long fileId,
            @AuthenticationPrincipal UserDetails userDetails) {

        validateOwnership(projectId, userDetails);
        fileStorageService.delete(fileId, projectId);
        return ResponseEntity.ok(Map.of("success", true, "deletedId", fileId));
    }

    private void validateOwnership(Long projectId, UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        projectService.getById(projectId, user.getId());
    }
}