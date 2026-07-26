package com.joprelys.backend.ai.ambient.application;

import com.joprelys.backend.ai.ambient.application.AmbientNoteContract.NoteRevisionView;
import com.joprelys.backend.ai.ambient.application.AmbientNoteFactualityGuard.GroundedNote;
import com.joprelys.backend.ai.ambient.application.AmbientNoteFactualityGuard.GroundedStatement;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.domain.AmbientNoteTemplate;
import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.domain.AiProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class AmbientNoteEngineService {

    private static final int MAX_BATCH_ITEMS = 40;
    private static final int MAX_BATCH_CHARACTERS = 18_000;
    private static final int MAX_PROVIDER_CALLS = 30;
    private static final Comparator<TranscriptItemView> AUDIO_ORDER = Comparator
            .comparingLong(TranscriptItemView::startOffsetMs)
            .thenComparingLong(TranscriptItemView::endOffsetMs)
            .thenComparingLong(TranscriptItemView::sequence);

    private final AiProvider aiProvider;
    private final AmbientTranscriptLedgerService ledgerService;
    private final AmbientNotePersistenceService persistenceService;
    private final AmbientNoteResponseParser responseParser;
    private final AmbientNoteFactualityGuard factualityGuard;
    private final ObjectMapper objectMapper;

    public AmbientNoteEngineService(
            AiProvider aiProvider,
            AmbientTranscriptLedgerService ledgerService,
            AmbientNotePersistenceService persistenceService,
            AmbientNoteResponseParser responseParser,
            AmbientNoteFactualityGuard factualityGuard,
            ObjectMapper objectMapper) {
        this.aiProvider = aiProvider;
        this.ledgerService = ledgerService;
        this.persistenceService = persistenceService;
        this.responseParser = responseParser;
        this.factualityGuard = factualityGuard;
        this.objectMapper = objectMapper;
    }

    public NoteRevisionView generate(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String templateValue,
            String localeValue) {
        AmbientNoteTemplate template = parseTemplate(templateValue);
        String locale = normalizeLocale(localeValue);
        List<TranscriptItemView> effective = new ArrayList<>(
                ledgerService.listFinal(visitId, organizationId).items());
        effective.sort(AUDIO_ORDER);
        if (effective.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_AMBIENT_TRANSCRIPT_EMPTY");
        }

        long transcriptMaxSequence = effective.stream()
                .mapToLong(TranscriptItemView::sequence)
                .max()
                .orElse(0);
        Optional<NoteRevisionView> latest = persistenceService.latest(visitId, organizationId);
        boolean canReuse = latest.filter(note -> canReuse(note, template, effective)).isPresent();
        if (canReuse) {
            NoteRevisionView reusable = latest.orElseThrow();
            if (!"REJECTED".equals(reusable.status())
                    && reusable.transcriptMaxSequence() >= transcriptMaxSequence) {
                return reusable;
            }
        }

        GroundedNote currentNote = canReuse && !"REJECTED".equals(latest.orElseThrow().status())
                ? groundedFromView(latest.orElseThrow())
                : new GroundedNote(List.of());
        long alreadyProcessedSequence = canReuse && !"REJECTED".equals(latest.orElseThrow().status())
                ? latest.orElseThrow().transcriptMaxSequence()
                : 0;

        List<TranscriptItemView> pending = effective.stream()
                .filter(item -> item.sequence() > alreadyProcessedSequence)
                .sorted(AUDIO_ORDER)
                .toList();
        if (pending.isEmpty()) {
            pending = List.copyOf(effective);
            currentNote = new GroundedNote(List.of());
            alreadyProcessedSequence = 0;
        }

        List<List<TranscriptItemView>> batches = batches(pending);
        if (batches.size() > MAX_PROVIDER_CALLS) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "AI_AMBIENT_NOTE_TRANSCRIPT_TOO_LARGE");
        }

        Set<UUID> processedEvidenceIds = new HashSet<>();
        long initialSequence = alreadyProcessedSequence;
        effective.stream()
                .filter(item -> item.sequence() <= initialSequence)
                .map(TranscriptItemView::id)
                .forEach(processedEvidenceIds::add);

        String lastModel = null;
        int totalTokens = 0;
        boolean hasTokenUsage = false;
        for (List<TranscriptItemView> batch : batches) {
            AiChatResponse response = aiProvider.chat(
                    List.of(AiMessage.user(userPayload(template, currentNote, batch))),
                    systemPrompt(locale, template));
            var parsed = responseParser.parse(response.content(), template);
            batch.stream().map(TranscriptItemView::id).forEach(processedEvidenceIds::add);
            List<TranscriptItemView> processedEvidence = effective.stream()
                    .filter(item -> processedEvidenceIds.contains(item.id()))
                    .toList();
            currentNote = factualityGuard.enforce(parsed, template, processedEvidence);
            if (response.model() != null && !response.model().isBlank()) lastModel = response.model();
            if (response.tokensUsed() != null) {
                totalTokens += response.tokensUsed();
                hasTokenUsage = true;
            }
        }

        return persistenceService.persistGenerated(
                visitId,
                userId,
                organizationId,
                template,
                locale,
                transcriptMaxSequence,
                lastModel,
                hasTokenUsage ? totalTokens : null,
                currentNote);
    }

    private boolean canReuse(
            NoteRevisionView note,
            AmbientNoteTemplate template,
            List<TranscriptItemView> effective) {
        if (!template.name().equals(note.template())) return false;
        Set<UUID> effectiveIds = effective.stream()
                .map(TranscriptItemView::id)
                .collect(java.util.stream.Collectors.toSet());
        return note.statements().stream()
                .flatMap(statement -> statement.evidenceItemIds().stream())
                .allMatch(effectiveIds::contains);
    }

    private GroundedNote groundedFromView(NoteRevisionView note) {
        List<GroundedStatement> statements = note.statements().stream()
                .map(statement -> new GroundedStatement(
                        statement.section(),
                        statement.order(),
                        statement.text(),
                        statement.critical(),
                        Set.copyOf(statement.evidenceItemIds())))
                .toList();
        return new GroundedNote(statements);
    }

    private List<List<TranscriptItemView>> batches(List<TranscriptItemView> items) {
        List<List<TranscriptItemView>> result = new ArrayList<>();
        List<TranscriptItemView> current = new ArrayList<>();
        int characters = 0;
        for (TranscriptItemView item : items) {
            int itemCharacters = item.text() == null ? 0 : item.text().length();
            if (!current.isEmpty()
                    && (current.size() >= MAX_BATCH_ITEMS
                    || characters + itemCharacters > MAX_BATCH_CHARACTERS)) {
                result.add(List.copyOf(current));
                current.clear();
                characters = 0;
            }
            current.add(item);
            characters += itemCharacters;
        }
        if (!current.isEmpty()) result.add(List.copyOf(current));
        return List.copyOf(result);
    }

    private String userPayload(
            AmbientNoteTemplate template,
            GroundedNote currentNote,
            List<TranscriptItemView> batch) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("template", template.name());
            payload.put("allowedSections", template.sections());
            payload.put("currentNote", currentNote.statements().stream()
                    .map(statement -> Map.of(
                            "section", statement.section(),
                            "text", statement.text(),
                            "evidenceItemIds", statement.evidenceItemIds().stream().map(UUID::toString).toList()))
                    .toList());
            payload.put("newTranscriptItems", batch.stream()
                    .sorted(AUDIO_ORDER)
                    .map(item -> Map.of(
                            "id", item.id().toString(),
                            "sequence", item.sequence(),
                            "speaker", item.speakerType(),
                            "startOffsetMs", item.startOffsetMs(),
                            "endOffsetMs", item.endOffsetMs(),
                            "text", item.text()))
                    .toList());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("AI_AMBIENT_NOTE_PAYLOAD_FAILED", exception);
        }
    }

    private String systemPrompt(String locale, AmbientNoteTemplate template) {
        if (locale.startsWith("en")) {
            return """
                    You are a clinical ambient note compiler, not a diagnostic reasoner.
                    Return ONLY strict JSON with this exact root shape: {"statements":[{"section":"...","text":"...","evidenceItemIds":["uuid"]}]}.
                    Allowed sections: %s.
                    The transcript items and the already-grounded currentNote are the ONLY evidence.
                    Never infer, complete, normalize, calculate, translate, convert units, add diagnoses, medications, orders, vitals, durations, negations, laterality or numbers that are not explicitly present in evidence.
                    Preserve clinical vocabulary, numbers, units and negations lexically. Prefer omission over paraphrase.
                    Keep unchanged currentNote statements verbatim unless new transcript explicitly corrects or supersedes them.
                    Every statement must cite 1 to 8 evidence item IDs. Use only IDs present in currentNote or newTranscriptItems.
                    SUBJECTIVE/HISTORY/CHIEF_COMPLAINT/FOLLOW_UP may use PATIENT or DOCTOR evidence.
                    Critical sections require DOCTOR evidence. Never create a critical statement from PATIENT or UNSPECIFIED evidence alone.
                    UNSPECIFIED must never be the sole evidence of any statement.
                    Do not output markdown, prose, explanations or extra keys.
                    """.formatted(String.join(", ", template.sections()));
        }
        return """
                Vous êtes un compilateur de note clinique ambient, pas un moteur de diagnostic.
                Retournez UNIQUEMENT un JSON strict de forme exacte : {"statements":[{"section":"...","text":"...","evidenceItemIds":["uuid"]}]}.
                Sections autorisées : %s.
                Les items de transcript et la currentNote déjà validée sont les SEULES preuves autorisées.
                N'inférez, ne complétez, ne normalisez, ne calculez, ne traduisez et ne convertissez jamais une information absente des preuves : diagnostic, médicament, examen, constante, durée, négation, latéralité, nombre ou unité.
                Conservez lexicalement le vocabulaire clinique, les nombres, unités et négations. Préférez l'omission à la paraphrase.
                Conservez mot pour mot les phrases inchangées de currentNote sauf correction explicite dans le nouveau transcript.
                Chaque phrase doit citer 1 à 8 IDs d'items de preuve. Utilisez uniquement les IDs présents dans currentNote ou newTranscriptItems.
                SUBJECTIVE/HISTORY/CHIEF_COMPLAINT/FOLLOW_UP peuvent utiliser des preuves PATIENT ou DOCTOR.
                Les sections critiques exigent une preuve DOCTOR. Ne créez jamais de phrase critique à partir de PATIENT ou UNSPECIFIED seuls.
                UNSPECIFIED ne doit jamais être la seule preuve d'une phrase.
                Aucun markdown, aucune prose libre, aucune explication et aucune clé supplémentaire.
                """.formatted(String.join(", ", template.sections()));
    }

    private AmbientNoteTemplate parseTemplate(String value) {
        try {
            return AmbientNoteTemplate.parse(value);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_NOTE_TEMPLATE_INVALID");
        }
    }

    private String normalizeLocale(String value) {
        String locale = value == null || value.isBlank()
                ? "fr"
                : value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (!locale.matches("[a-z]{2,3}(?:-[a-z]{2})?")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_NOTE_LOCALE_INVALID");
        }
        return locale;
    }
}
