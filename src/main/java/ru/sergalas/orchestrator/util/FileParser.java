package ru.sergalas.orchestrator.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@UtilityClass
public class FileParser {

    private static final Pattern FILE_MARKER_PATTERN = Pattern.compile(
        "\\[FILE:\\s*([^\\]]+)\\]\\s*```(?:[a-zA-Z0-9_-]+)?\\s*\\n([\\s\\S]*?)\\n```",
        Pattern.MULTILINE
    );

    private static final Pattern FALLBACK_PATTERN = Pattern.compile(
        "\\[FILE:\\s*([^\\]]+)\\]\\s*\\n([\\s\\S]*?)(?=\\[FILE:|$)",
        Pattern.MULTILINE
    );

    public static Map<String, String> parseFiles(String responseText) {
        Map<String, String> files = new LinkedHashMap<>();
        if (responseText == null || responseText.isBlank()) {
            return files;
        }

        Matcher matcher = FILE_MARKER_PATTERN.matcher(responseText);
        while (matcher.find()) {
            String path = matcher.group(1).trim();
            String content = matcher.group(2).trim();
            files.put(path, content);
        }

        if (files.isEmpty()) {
            Matcher fallbackMatcher = FALLBACK_PATTERN.matcher(responseText);
            while (fallbackMatcher.find()) {
                String path = fallbackMatcher.group(1).trim();
                String rawContent = fallbackMatcher.group(2).trim();
                if (rawContent.startsWith("```")) {
                    rawContent = rawContent.replaceFirst("^```[a-zA-Z0-9_-]*\\n?", "");
                }
                if (rawContent.endsWith("```")) {
                    rawContent = rawContent.substring(0, rawContent.length() - 3).trim();
                }
                files.put(path, rawContent);
            }
        }

        log.debug("Extracted {} files from LLM output", files.size());
        return files;
    }
}