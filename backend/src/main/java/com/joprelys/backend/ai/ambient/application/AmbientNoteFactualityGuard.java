package com.joprelys.backend.ai.ambient.application;

import com.joprelys.backend.ai.ambient.application.AmbientNoteResponseParser.ParsedNote;
import com.joprelys.backend.ai.ambient.application.AmbientNoteResponseParser.ParsedStatement;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.domain.AmbientNoteTemplate;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AmbientNoteFactualityGuard {

    private static final Logger log = LoggerFactory.getLogger(AmbientNoteFactualityGuard.class);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9]+(?:[.,][0-9]+)?");
    private static final Set<String> NEGATIONS = Set.of(
            "pas", "sans", "aucun", "aucune", "non", "nie",
            "not", "no", "without", "denies", "denied");
    private static final Set<String> SAFE_GLUE = Set.of(
            "patient", "patiente", "presente", "signale", "rapporte", "decrit", "indique",
            "avec", "pour", "depuis", "dans", "chez", "une", "des", "les", "est", "sont",
            "avait", "avoir", "sur", "sous", "par", "et", "ou", "the", "and", "with", "from",
            "for", "has", "have", "reports", "reported", "presents", "presenting", "of", "to");
    private static final Set<String> NEGATION_SKIP = Set.of(
            "de", "du", "des", "la", "le", "les", "un", "une", "d", "a", "any", "the");

    public GroundedNote enforce(
            ParsedNote parsed,
            AmbientNoteTemplate template,
            List<TranscriptItemView> effectiveTranscript) {
        if (parsed == null || template == null || effectiveTranscript == null) {
            throw ungrounded();
        }
        Map<UUID, TranscriptItemView> byId = new LinkedHashMap<>();
        for (TranscriptItemView item : effectiveTranscript) {
            if (item != null && item.id() != null && "FINAL".equals(item.status())) {
                byId.put(item.id(), item);
            }
        }

        List<GroundedStatement> result = new ArrayList<>();
        Map<String, Integer> orders = new LinkedHashMap<>();
        for (ParsedStatement statement : parsed.statements()) {
            List<TranscriptItemView> evidence = resolveEvidence(statement, byId);
            boolean critical = template.isCriticalSection(statement.section());
            validateSpeakerEvidence(statement, evidence, critical);
            validateLexicalGrounding(statement, evidence);
            int order = orders.merge(statement.section(), 1, Integer::sum);
            result.add(new GroundedStatement(
                    statement.section(),
                    order,
                    statement.text(),
                    critical,
                    Set.copyOf(statement.evidenceItemIds())));
        }
        return new GroundedNote(List.copyOf(result));
    }

    private List<TranscriptItemView> resolveEvidence(
            ParsedStatement statement,
            Map<UUID, TranscriptItemView> byId) {
        List<TranscriptItemView> evidence = new ArrayList<>();
        for (UUID id : statement.evidenceItemIds()) {
            TranscriptItemView item = byId.get(id);
            if (item == null) {
                log.warn("Ambient note blocked: unknown/superseded evidence item {}", id);
                throw ungrounded();
            }
            evidence.add(item);
        }
        if (evidence.isEmpty()) throw ungrounded();
        return evidence;
    }

    private void validateSpeakerEvidence(
            ParsedStatement statement,
            List<TranscriptItemView> evidence,
            boolean critical) {
        boolean explicitSpeaker = evidence.stream()
                .anyMatch(item -> "DOCTOR".equals(item.speakerType()) || "PATIENT".equals(item.speakerType()));
        if (!explicitSpeaker) {
            log.warn("Ambient note blocked: statement {} is supported only by UNSPECIFIED speakers", statement.section());
            throw ungrounded();
        }
        if (critical && evidence.stream().noneMatch(item -> "DOCTOR".equals(item.speakerType()))) {
            log.warn("Ambient note blocked: critical statement {} has no DOCTOR evidence", statement.section());
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "AI_AMBIENT_NOTE_CRITICAL_REQUIRES_DOCTOR_EVIDENCE");
        }
    }

    private void validateLexicalGrounding(
            ParsedStatement statement,
            List<TranscriptItemView> evidence) {
        String source = evidence.stream()
                .map(TranscriptItemView::text)
                .filter(text -> text != null && !text.isBlank())
                .reduce("", (left, right) -> left + " " + right);
        Set<String> sourceTokens = significantTokens(source);
        Set<String> statementTokens = significantTokens(statement.text());
        if (statementTokens.isEmpty()) throw ungrounded();

        for (String token : statementTokens) {
            if (SAFE_GLUE.contains(token)) continue;
            if (!sourceTokens.contains(token)) {
                log.warn("Ambient note blocked: unsupported token '{}' in section {}", token, statement.section());
                throw ungrounded();
            }
        }
        Set<String> sourceNumbers = numericTokens(source);
        for (String number : numericTokens(statement.text())) {
            if (!sourceNumbers.contains(number)) {
                throw ungrounded();
            }
        }
        validateNegations(statement.text(), source);
    }

    private void validateNegations(String statement, String source) {
        List<String> sourceTokens = orderedTokens(source);
        Set<String> statementTokens = significantTokens(statement);
        boolean statementHasNegation = statementTokens.stream().anyMatch(NEGATIONS::contains);

        for (int index = 0; index < sourceTokens.size(); index++) {
            String token = sourceTokens.get(index);
            if (!NEGATIONS.contains(token)) continue;
            String negatedConcept = nextConcept(sourceTokens, index + 1);
            if (negatedConcept != null
                    && statementTokens.contains(negatedConcept)
                    && !statementHasNegation) {
                throw ungrounded();
            }
        }
    }

    private String nextConcept(List<String> tokens, int start) {
        for (int index = start; index < Math.min(tokens.size(), start + 5); index++) {
            String token = tokens.get(index);
            if (NEGATION_SKIP.contains(token) || SAFE_GLUE.contains(token) || NEGATIONS.contains(token)) continue;
            if (token.length() >= 3 || isNumeric(token)) return token;
        }
        return null;
    }

    private Set<String> significantTokens(String value) {
        Set<String> result = new HashSet<>();
        for (String token : orderedTokens(value)) {
            if (token.length() >= 3 || isNumeric(token) || NEGATIONS.contains(token)) {
                result.add(token);
            }
        }
        return result;
    }

    private Set<String> numericTokens(String value) {
        Set<String> result = new HashSet<>();
        for (String token : orderedTokens(value)) {
            if (isNumeric(token)) result.add(token);
        }
        return result;
    }

    private List<String> orderedTokens(String value) {
        List<String> result = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(normalize(value));
        while (matcher.find()) result.add(matcher.group());
        return result;
    }

    private boolean isNumeric(String token) {
        return token.matches("\\d+(?:[.,]\\d+)?");
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9.,]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private ResponseStatusException ungrounded() {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_AMBIENT_NOTE_UNGROUNDED");
    }

    public record GroundedNote(List<GroundedStatement> statements) {
    }

    public record GroundedStatement(
            String section,
            int order,
            String text,
            boolean critical,
            Set<UUID> evidenceItemIds) {
    }
}
