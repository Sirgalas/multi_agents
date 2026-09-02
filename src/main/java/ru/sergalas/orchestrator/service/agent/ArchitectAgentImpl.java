package ru.sergalas.orchestrator.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.sergalas.orchestrator.dto.response.ArchitectQuestionsResponse;
import ru.sergalas.orchestrator.dto.response.ArchitectSpecification;
import ru.sergalas.orchestrator.entity.FileStructureTemplate;
import ru.sergalas.orchestrator.entity.ProjectContext;
import ru.sergalas.orchestrator.entity.ProjectMcpServer;
import ru.sergalas.orchestrator.service.McpService;
import ru.sergalas.orchestrator.util.Either;
import ru.sergalas.orchestrator.util.PromptBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ArchitectAgentImpl implements ArchitectAgent {

    private final ChatModel architectChatModel;
    private final McpService mcpService;
    private final ObjectMapper objectMapper;

    public ArchitectAgentImpl(
            @Qualifier("architectChatModel") ChatModel architectChatModel,
            McpService mcpService,
            ObjectMapper objectMapper
    ) {
        this.architectChatModel = architectChatModel;
        this.mcpService = mcpService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Either<ArchitectQuestionsResponse, ArchitectSpecification> analyze(
            Long projectId,
            String taskContent,
            FileStructureTemplate fileStructure,
            List<ProjectMcpServer> mcpServers,
            List<ProjectContext> contextFiles
    ) {
        Map<String, String> mcpContexts = new HashMap<>();
        if (mcpServers != null) {
            for (ProjectMcpServer server : mcpServers) {
                if (Boolean.TRUE.equals(server.getIsActive())) {
                    mcpContexts.put(server.getServerUrl(), mcpService.fetchMcpContext(server, "architecture"));
                }
            }
        }

        String structurePrompt = fileStructure != null ? fileStructure.getStructureTree() : "Generate a standard Clean Architecture Java 21 file structure.";

        String systemPrompt = """
                You are a Principal Software Architect (Claude Sonnet 4.6).
                Analyze the following Technical Task and Architecture requirements.
                
                Base File Structure:
                %s
                
                %s
                %s
                
                TASK DESCRIPTION:
                %s
                
                If the task has critical ambiguities that block full implementation, output a JSON array of clarifying questions.
                Otherwise, provide full architecture specifications, contracts, and DTO contracts with [FILE: path] markers.
                """.formatted(
                structurePrompt,
                PromptBuilder.buildMcpContextSection(mcpServers, mcpContexts),
                PromptBuilder.buildContextFilesSection(contextFiles),
                taskContent
        );

        String response = architectChatModel.call(systemPrompt);
        log.debug("Architect Agent response: {}", response);

        if (response != null && response.contains("\"questions\"")) {
            try {
                int jsonStart = response.indexOf("{");
                int jsonEnd = response.lastIndexOf("}");
                if (jsonStart != -1 && jsonEnd != -1) {
                    ArchitectQuestionsResponse questions = objectMapper.readValue(
                            response.substring(jsonStart, jsonEnd + 1),
                            ArchitectQuestionsResponse.class
                    );
                    questions.setProjectId(projectId);
                    return Either.left(questions);
                }
            } catch (Exception e) {
                log.warn("Could not parse clarifying questions JSON: {}", e.getMessage());
            }
        }

        return Either.right(ArchitectSpecification.builder()
                .structureTree(structurePrompt)
                .architectureSpec(response)
                .dtoList("Generated from Architect Specification")
                .serviceContracts("Generated from Architect Specification")
                .build());
    }

    @Override
    public ArchitectSpecification finalizeSpec(
            String taskContent,
            Map<String, String> answers,
            FileStructureTemplate fileStructure,
            List<ProjectMcpServer> mcpServers
    ) {
        String structurePrompt = fileStructure != null ? fileStructure.getStructureTree() : "Clean Architecture Java 21";

        String prompt = """
                You are a Principal Software Architect.
                Finalize the technical specification with user clarifications.
                
                TASK:
                %s
                
                ANSWERS:
                %s
                
                STRUCTURE:
                %s
                
                Output the final architectural specification with complete contracts.
                """.formatted(taskContent, answers.toString(), structurePrompt);

        String response = architectChatModel.call(prompt);

        return ArchitectSpecification.builder()
                .structureTree(structurePrompt)
                .architectureSpec(response)
                .dtoList("Finalized DTO contracts")
                .serviceContracts("Finalized Service contracts")
                .build();
    }
}