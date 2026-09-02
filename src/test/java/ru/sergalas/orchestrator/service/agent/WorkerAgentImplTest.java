package ru.sergalas.orchestrator.service.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import ru.sergalas.orchestrator.dto.response.ArchitectSpecification;
import ru.sergalas.orchestrator.service.McpService;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: WorkerAgentImpl (Gemini 3.7 Flash High)")
class WorkerAgentImplTest {

    @Mock
    private ChatModel workerChatModel;

    @Mock
    private McpService mcpService;

    @InjectMocks
    private WorkerAgentImpl workerAgent;

    @Test
    @DisplayName("generateCode invokes chat model and returns parsed files")
    void testGenerateCode() {
        String llmOutput = """
                [FILE: src/main/java/ru/sergalas/orchestrator/entity/Demo.java]
                ```java
                package ru.sergalas.orchestrator.entity;
                public class Demo {}
                ```
                """;

        when(workerChatModel.call(anyString())).thenReturn(llmOutput);

        ArchitectSpecification spec = ArchitectSpecification.builder()
                .architectureSpec("Spec 1")
                .structureTree("Tree")
                .build();

        Map<String, String> files = workerAgent.generateCode(spec, Collections.emptyList());

        assertThat(files).hasSize(1);
        assertThat(files).containsKey("src/main/java/ru/sergalas/orchestrator/entity/Demo.java");
        verify(workerChatModel).call(anyString());
    }
}