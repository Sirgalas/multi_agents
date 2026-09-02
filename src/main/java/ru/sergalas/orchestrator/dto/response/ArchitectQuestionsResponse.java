package ru.sergalas.orchestrator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sergalas.orchestrator.entity.enums.QuestionType;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchitectQuestionsResponse {
    private Long projectId;
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Question {
        private String id;
        private String text;
        @Builder.Default
        private QuestionType type = QuestionType.TEXT;
        @Builder.Default
        private List<String> options = new ArrayList<>();
    }
}