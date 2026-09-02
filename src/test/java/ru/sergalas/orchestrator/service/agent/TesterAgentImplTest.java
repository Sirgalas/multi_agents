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
@DisplayName("Unit Tests: TesterAgentImpl (Gemini 3.7 Flash High)")
class TesterAgentImplTest {

    @Mock
    private ChatModel testerChatModel;

    @InjectMocks
    private TesterAgentImpl testerAgent;

    @Test
    @DisplayName("generateTests parses unit test files from LLM response")
    void testGenerateTests() {
        String testOutput = """
                [FILE: src/test/java/ru/sergalas/DemoTest.java]
                ```java
                package ru.sergalas;
                import org.junit.jupiter.api.Test;
                class DemoTest {}
                ```
                """;

        when(testerChatModel.call(anyString())).thenReturn(testOutput);

        Map<String, String> generatedCode = Map.of(
                "src/main/java/ru/sergalas/Demo.java", "public class Demo {}"
        );

        Map<String, String> tests = testerAgent.generateTests(generatedCode, ArchitectSpecification.builder().build());

        assertThat(tests).hasSize(1);
        assertThat(tests).containsKey("src/test/java/ru/sergalas/DemoTest.java");
    }
}