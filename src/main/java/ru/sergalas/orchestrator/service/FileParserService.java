package ru.sergalas.orchestrator.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileParserService {
    String parseFileContent(MultipartFile file) throws IOException;
    void validateFile(MultipartFile file);
}