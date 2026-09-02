package ru.sergalas.orchestrator.service.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.sergalas.orchestrator.dto.response.ArchitectSpecification;
import ru.sergalas.orchestrator.util.FileParser;

import java.util.Map;

@Slf4j
@Service
public class HelperAgentImpl implements HelperAgent {

    private final ChatModel helperChatModel;

    public HelperAgentImpl(@Qualifier("helperChatModel") ChatModel helperChatModel) {
        this.helperChatModel = helperChatModel;
    }

    @Override
    public Map<String, String> generateConfig(ArchitectSpecification spec, Map<String, String> generatedCode) {
        String prompt = """
                You are a DevOps & Configuration Assistant (Gemini 3.6 Flash High).
                Generate all necessary build, container, and configuration files for this Spring Boot 3.4 project:
                - build.gradle & settings.gradle
                - docker-compose.yml & Dockerfiles
                - application.yaml & .env.example
                - README.md with clear launch instructions
                
                Format each file strictly with:
                [FILE: path]
                ```
                // content
                ```
                
                PROJECT SPECIFICATION:
                %s
                """.formatted(spec.getArchitectureSpec());

        String response = helperChatModel.call(prompt);
        log.debug("Helper Agent generated build/config files");

        return FileParser.parseFiles(response);
    }
}