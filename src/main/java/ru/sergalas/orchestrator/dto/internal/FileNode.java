package ru.sergalas.orchestrator.dto.internal;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FileNode {
    private String type;
    private String name;
    private List<FileNode> children = new ArrayList<>();
    
    public boolean isDirectory() {
        return "directory".equalsIgnoreCase(type);
    }
    
    public boolean isFile() {
        return "file".equalsIgnoreCase(type);
    }
}