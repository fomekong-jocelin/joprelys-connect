package com.joprelys.backend.ai.ambient.application;

import com.joprelys.backend.ai.ambient.domain.AmbientNoteTemplate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Component
public class AmbientNoteResponseParser {

    private static final Set<String> ROOT_FIELDS = Set.of("statements");
    private static final Set<String> STATEMENT_FIELDS = Set.of(
            "section", "text", "evidenceItemIds");
    private static final int MAX_STATEMENTS = 100;
    private static final int MAX_STATEMENT_LENGTH = 2_000;
    private static final int MAX_EVIDENCE_ITEMS = 8;

    private final ObjectMapper objectMapper;

    public AmbientNoteResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public ParsedNote parse(String content, AmbientNoteTemplate template) {
        if (content == null || content.isBlank() || template == null) {
            throw invalidOutput();
        }
        String cleaned = content.trim();
        if (!cleaned.startsWith("{") || !cleaned.endsWith("}")) {
            throw invalidOutput();
        }
        try {
            Map<String, Object> root = objectMapper.readValue(cleaned, Map.class);
            rejectUnknownKeys(root, ROOT_FIELDS);
            if (!(root.get("statements") instanceof List<?> statements)) {
                throw invalidOutput();
            }
            if (statements.size() > MAX_STATEMENTS) {
                throw invalidOutput();
            }
            List<ParsedStatement> parsed = new ArrayList<>();
            for (Object item : statements) {
                parsed.add(parseStatement(item, template));
            }
            return new ParsedNote(List.copyOf(parsed));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidOutput();
        }
    }

    private ParsedStatement parseStatement(Object value, AmbientNoteTemplate template) {
        if (!(value instanceof Map<?, ?> map)) {
            throw invalidStatement();
        }
        rejectUnknownKeys(map, STATEMENT_FIELDS);
        String section = stringValue(map.get("section")).toUpperCase(Locale.ROOT);
        String text = stringValue(map.get("text"));
        if (!template.isAllowedSection(section) || text.isBlank() || text.length() > MAX_STATEMENT_LENGTH) {
            throw invalidStatement();
        }
        Object rawEvidence = map.get("evidenceItemIds");
        if (!(rawEvidence instanceof List<?> evidenceList)
                || evidenceList.isEmpty()
                || evidenceList.size() > MAX_EVIDENCE_ITEMS) {
            throw invalidStatement();
        }
        LinkedHashSet<UUID> evidenceIds = new LinkedHashSet<>();
        for (Object rawId : evidenceList) {
            if (!(rawId instanceof String id) || id.isBlank()) {
                throw invalidStatement();
            }
            try {
                evidenceIds.add(UUID.fromString(id.trim()));
            } catch (IllegalArgumentException exception) {
                throw invalidStatement();
            }
        }
        if (evidenceIds.isEmpty() || evidenceIds.size() != evidenceList.size()) {
            throw invalidStatement();
        }
        return new ParsedStatement(section, text.trim(), Set.copyOf(evidenceIds));
    }

    private void rejectUnknownKeys(Map<?, ?> map, Set<String> allowed) {
        for (Object key : map.keySet()) {
            if (!(key instanceof String stringKey) || !allowed.contains(stringKey)) {
                throw invalidOutput();
            }
        }
    }

    private String stringValue(Object value) {
        return value instanceof String text ? text.trim() : "";
    }

    private ResponseStatusException invalidOutput() {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_AMBIENT_NOTE_OUTPUT_INVALID");
    }

    private ResponseStatusException invalidStatement() {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_AMBIENT_NOTE_STATEMENT_INVALID");
    }

    public record ParsedNote(List<ParsedStatement> statements) {
    }

    public record ParsedStatement(
            String section,
            String text,
            Set<UUID> evidenceItemIds) {
    }
}
