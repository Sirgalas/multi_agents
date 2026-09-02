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
public class TesterAgentImpl implements TesterAgent {

    private final ChatModel testerChatModel;

    public TesterAgentImpl(@Qualifier("testerChatModel") ChatModel testerChatModel) {
        this.testerChatModel = testerChatModel;
    }

    @Override
    public Map<String, String> generateTests(Map<String, String> generatedCode, ArchitectSpecification spec) {
        StringBuilder codeSummary = new StringBuilder();
        generatedCode.forEach((path, code) -> {
            if (path.endsWith(".java")) {
                codeSummary.append("[FILE: ").append(path).append("]\n```java\n").append(code).append("\n```\n\n");
            }
        });

        String prompt = """
                You are a Senior QA Automation Engineer (Gemini 3.7 Flash High).
                Write comprehensive Unit and Integration tests using JUnit 5, Mockito, and SpringBootTest for the provided codebase.
                
                REQUIREMENTS:
                1. Target coverage for Services, Controllers, and Utilities.
                2. Every file MUST be wrapped in:
                [FILE: src/test/java/path/to/Test.java]
                ```java
                // Test code
                ```
                
                CODEBASE:
                %s
                """.formatted(codeSummary.toString());

        String response = testerChatModel.call(prompt);
        log.debug("Tester Agent generated test files");

        return FileParser.parseFiles(response);
    }
}