package ru.sergalas.orchestrator.service.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import ru.sergalas.orchestrator.dto.response.ArchitectSpecification;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: HelperAgentImpl (Gemini 3.6 Flash High)")
class HelperAgentImplTest {

    @Mock
    private ChatModel helperChatModel;

    @InjectMocks
    private HelperAgentImpl helperAgent;

    @Test
    @DisplayName("generateConfig generates build and container configuration files")
    void testGenerateConfig() {
        String helperOutput = """
                [FILE: docker-compose.yml]
                ```yaml
                version: '3.9'
                ```
                [FILE: build.gradle]
                ```gradle
                plugins { id 'java' }
                ```
                """;

        when(helperChatModel.call(anyString())).thenReturn(helperOutput);

        Map<String, String> configs = helperAgent.generateConfig(
                ArchitectSpecification.builder().architectureSpec("Spec").build(),
                Map.of("Demo.java", "class Demo{}")
        );

        assertThat(configs).hasSize(2);
        assertThat(configs).containsKey("docker-compose.yml");
        assertThat(configs).containsKey("build.gradle");
    }
}