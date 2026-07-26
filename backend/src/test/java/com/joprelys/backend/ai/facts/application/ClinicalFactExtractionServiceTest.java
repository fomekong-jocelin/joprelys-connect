package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptLedgerView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptLedgerService;
import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactExtractionEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactExtractionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class ClinicalFactExtractionServiceTest {

    private final AmbientTranscriptLedgerService transcriptLedger = mock(AmbientTranscriptLedgerService.class);
    private final ClinicalFactLedgerService factLedger = mock(ClinicalFactLedgerService.class);
    private final ClinicalFactExtractionRepository extractionRepository = mock(ClinicalFactExtractionRepository.class);
    private final AiProvider aiProvider = mock(AiProvider.class);
    private final ClinicalFactExtractionService service = new ClinicalFactExtractionService(
            transcriptLedger,
            factLedger,
            extractionRepository,
            aiProvider,
            new ObjectMapper());

    @Test
    void shouldExtractTwoAtomicFactsFromOneBatchAndBuildExactEvidenceOffsets() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView doctor = item("DOCTOR", "Tension 120/80. Pouls 72.");
        TranscriptItemView patient = item("PATIENT", "Sans fièvre depuis 2 jours.");
        prepareTranscript(visitId, organizationId, doctor, patient);
        noPreviousExtraction(visitId, doctor, patient);
        when(aiProvider.chatStructured(anyList(), anyString(), anyString(), anyMap()))
                .thenReturn(new AiChatResponse("""
                        {"facts":[
                          {
                            "transcriptItemId":"%s",
                            "factType":"VITAL",
                            "authority":"CLINICIAN_OBSERVED",
                            "conceptCode":"BLOOD_PRESSURE",
                            "conceptText":"Tension",
                            "polarity":"POSITIVE",
                            "valuePrimary":"120",
                            "valueSecondary":"80",
                            "unitCode":"MMHG",
                            "temporalityText":null,
                            "laterality":"UNSPECIFIED",
                            "frequencyText":null,
                            "routeText":null,
                            "quoteText":"Tension 120/80"
                          },
                          {
                            "transcriptItemId":"%s",
                            "factType":"SYMPTOM",
                            "authority":"PATIENT_REPORTED",
                            "conceptCode":"FEVER",
                            "conceptText":"fièvre",
                            "polarity":"NEGATIVE",
                            "valuePrimary":null,
                            "valueSecondary":null,
                            "unitCode":null,
                            "temporalityText":"depuis 2 jours",
                            "laterality":"UNSPECIFIED",
                            "frequencyText":null,
                            "routeText":null,
                            "quoteText":"Sans fièvre depuis 2 jours"
                          }
                        ]}
                        """.formatted(doctor.id(), patient.id()), 44, "gpt-4.1"));
        when(factLedger.appendValidatedFact(eq(visitId), eq(userId), eq(organizationId), any(FactCandidate.class)))
                .thenAnswer(invocation -> view(invocation.getArgument(3)));
        when(extractionRepository.saveAndFlush(any(ClinicalFactExtractionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var report = service.extractNewFacts(visitId, userId, organizationId);

        assertThat(report.processedItems()).isEqualTo(2);
        assertThat(report.candidateCount()).isEqualTo(2);
        assertThat(report.acceptedCount()).isEqualTo(2);
        assertThat(report.rejectedCount()).isZero();
        assertThat(report.model()).isEqualTo("gpt-4.1");

        ArgumentCaptor<FactCandidate> candidates = ArgumentCaptor.forClass(FactCandidate.class);
        verify(factLedger, times(2)).appendValidatedFact(
                eq(visitId), eq(userId), eq(organizationId), candidates.capture());
        FactCandidate bp = candidates.getAllValues().get(0);
        assertThat(bp.evidence().getFirst().quoteStartChar()).isZero();
        assertThat(bp.evidence().getFirst().quoteEndChar()).isEqualTo("Tension 120/80".length());
        assertThat(bp.sourceEventId()).startsWith("extract-v1-");
        assertThat(bp.sourceEventId()).hasSize(75);
        FactCandidate fever = candidates.getAllValues().get(1);
        assertThat(fever.evidence().getFirst().quoteText()).isEqualTo("Sans fièvre depuis 2 jours");
    }

    @Test
    void shouldTreatEmptyFactsAsSuccessfulCompletedExtraction() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView item = item("DOCTOR", "Bonjour madame.");
        prepareTranscript(visitId, organizationId, item);
        noPreviousExtraction(visitId, item);
        when(aiProvider.chatStructured(anyList(), anyString(), anyString(), anyMap()))
                .thenReturn(new AiChatResponse("{\"facts\":[]}", 8, "gpt-4.1"));
        when(extractionRepository.saveAndFlush(any(ClinicalFactExtractionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var report = service.extractNewFacts(visitId, userId, organizationId);

        assertThat(report.processedItems()).isEqualTo(1);
        assertThat(report.candidateCount()).isZero();
        assertThat(report.acceptedCount()).isZero();
        assertThat(report.rejectedCount()).isZero();
        verify(factLedger, never()).appendValidatedFact(any(), any(), any(), any());
        verify(extractionRepository).saveAndFlush(any(ClinicalFactExtractionEntity.class));
    }

    @Test
    void shouldRejectNonExactQuoteButCompleteTheItemWithoutPersistingFact() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView item = item("PATIENT", "Douleur genou gauche depuis hier.");
        prepareTranscript(visitId, organizationId, item);
        noPreviousExtraction(visitId, item);
        when(aiProvider.chatStructured(anyList(), anyString(), anyString(), anyMap()))
                .thenReturn(new AiChatResponse(candidateJson(
                        item.id(),
                        "SYMPTOM",
                        "PATIENT_REPORTED",
                        "KNEE_PAIN",
                        "douleur genou",
                        "POSITIVE",
                        "Douleur du genou gauche"), 20, "gpt-4.1"));
        when(extractionRepository.saveAndFlush(any(ClinicalFactExtractionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var report = service.extractNewFacts(visitId, userId, organizationId);

        assertThat(report.acceptedCount()).isZero();
        assertThat(report.rejectedCount()).isEqualTo(1);
        assertThat(report.rejections().getFirst().reason()).isEqualTo("AI_CLINICAL_FACT_QUOTE_NOT_EXACT");
        verify(factLedger, never()).appendValidatedFact(any(), any(), any(), any());
    }

    @Test
    void shouldKeepOtherValidFactsWhenOneCandidateIsRejectedByDeterministicLedgerGuard() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView item = item("DOCTOR", "Paracétamol 500 mg et ibuprofène 200 mg");
        prepareTranscript(visitId, organizationId, item);
        noPreviousExtraction(visitId, item);
        when(aiProvider.chatStructured(anyList(), anyString(), anyString(), anyMap()))
                .thenReturn(new AiChatResponse("""
                        {"facts":[
                          {
                            "transcriptItemId":"%s","factType":"MEDICATION","authority":"CLINICIAN_DECISION",
                            "conceptCode":"PARACETAMOL","conceptText":"Paracétamol","polarity":"POSITIVE",
                            "valuePrimary":"200","valueSecondary":null,"unitCode":"MG","temporalityText":null,
                            "laterality":"UNSPECIFIED","frequencyText":null,"routeText":null,
                            "quoteText":"Paracétamol 500 mg et ibuprofène 200 mg"
                          },
                          {
                            "transcriptItemId":"%s","factType":"MEDICATION","authority":"CLINICIAN_DECISION",
                            "conceptCode":"IBUPROFENE","conceptText":"ibuprofène","polarity":"POSITIVE",
                            "valuePrimary":"200","valueSecondary":null,"unitCode":"MG","temporalityText":null,
                            "laterality":"UNSPECIFIED","frequencyText":null,"routeText":null,
                            "quoteText":"Paracétamol 500 mg et ibuprofène 200 mg"
                          }
                        ]}
                        """.formatted(item.id(), item.id()), 30, "gpt-4.1"));
        when(factLedger.appendValidatedFact(eq(visitId), eq(userId), eq(organizationId), any(FactCandidate.class)))
                .thenAnswer(invocation -> {
                    FactCandidate candidate = invocation.getArgument(3);
                    if (candidate.conceptCode().equals("PARACETAMOL")) {
                        throw new ResponseStatusException(
                                org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                                "AI_CLINICAL_FACT_MEDICATION_RELATION_MISMATCH");
                    }
                    return view(candidate);
                });
        when(extractionRepository.saveAndFlush(any(ClinicalFactExtractionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var report = service.extractNewFacts(visitId, userId, organizationId);

        assertThat(report.candidateCount()).isEqualTo(2);
        assertThat(report.acceptedCount()).isEqualTo(1);
        assertThat(report.rejectedCount()).isEqualTo(1);
        assertThat(report.rejections().getFirst().reason())
                .isEqualTo("AI_CLINICAL_FACT_MEDICATION_RELATION_MISMATCH");
    }

    @Test
    void shouldFailWholeBatchWhenModelCitesTranscriptItemOutsideInput() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView item = item("DOCTOR", "Pouls 72");
        prepareTranscript(visitId, organizationId, item);
        noPreviousExtraction(visitId, item);
        when(aiProvider.chatStructured(anyList(), anyString(), anyString(), anyMap()))
                .thenReturn(new AiChatResponse(candidateJson(
                        UUID.randomUUID(),
                        "VITAL",
                        "CLINICIAN_OBSERVED",
                        "HEART_RATE",
                        "Pouls",
                        "POSITIVE",
                        "Pouls 72"), 20, "gpt-4.1"));

        assertThatThrownBy(() -> service.extractNewFacts(
                visitId, UUID.randomUUID(), organizationId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_EVIDENCE_OUTSIDE_BATCH");
        verify(extractionRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldSkipAlreadyProcessedAndUnspecifiedItemsWithoutCallingModel() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView processed = item("DOCTOR", "Pouls 72");
        TranscriptItemView unspecified = item("UNSPECIFIED", "Je prends un comprimé");
        prepareTranscript(visitId, organizationId, processed, unspecified);
        ClinicalFactExtractionEntity existing = new ClinicalFactExtractionEntity(
                organizationId,
                visitId,
                processed.id(),
                sha256(processed.text()),
                ClinicalFactExtractionService.EXTRACTOR_VERSION,
                "gpt-4.1",
                1,
                1,
                0,
                userId);
        when(extractionRepository.findByVisitIdAndTranscriptItemIdAndExtractorVersion(
                visitId, processed.id(), ClinicalFactExtractionService.EXTRACTOR_VERSION))
                .thenReturn(Optional.of(existing));

        var report = service.extractNewFacts(visitId, userId, organizationId);

        assertThat(report.processedItems()).isZero();
        assertThat(report.alreadyProcessedItems()).isEqualTo(1);
        assertThat(report.unspecifiedSpeakerItems()).isEqualTo(1);
        verify(aiProvider, never()).chatStructured(anyList(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldRejectClinicianAuthorityGeneratedFromPatientSpeakerBeforeLedger() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView patient = item("PATIENT", "Paracétamol 500 mg");
        prepareTranscript(visitId, organizationId, patient);
        noPreviousExtraction(visitId, patient);
        when(aiProvider.chatStructured(anyList(), anyString(), anyString(), anyMap()))
                .thenReturn(new AiChatResponse(candidateJson(
                        patient.id(),
                        "MEDICATION",
                        "CLINICIAN_DECISION",
                        "PARACETAMOL",
                        "Paracétamol",
                        "POSITIVE",
                        "Paracétamol 500 mg"), 18, "gpt-4.1"));
        when(extractionRepository.saveAndFlush(any(ClinicalFactExtractionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var report = service.extractNewFacts(visitId, userId, organizationId);

        assertThat(report.acceptedCount()).isZero();
        assertThat(report.rejections().getFirst().reason())
                .isEqualTo("AI_CLINICAL_FACT_PATIENT_AUTHORITY_INVALID");
        verify(factLedger, never()).appendValidatedFact(any(), any(), any(), any());
    }

    private void prepareTranscript(
            UUID visitId,
            UUID organizationId,
            TranscriptItemView... items) {
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of(items)));
    }

    private void noPreviousExtraction(UUID visitId, TranscriptItemView... items) {
        for (TranscriptItemView item : items) {
            when(extractionRepository.findByVisitIdAndTranscriptItemIdAndExtractorVersion(
                    visitId,
                    item.id(),
                    ClinicalFactExtractionService.EXTRACTOR_VERSION))
                    .thenReturn(Optional.empty());
        }
    }

    private TranscriptItemView item(String speakerType, String text) {
        return new TranscriptItemView(
                UUID.randomUUID(),
                1,
                "source-1",
                "AMBIENT_DIARIZED",
                speakerType,
                speakerType.toLowerCase(),
                text,
                "fr",
                0,
                1_000,
                "FINAL",
                null,
                Instant.now());
    }

    private FactView view(FactCandidate candidate) {
        return new FactView(
                UUID.randomUUID(),
                1,
                candidate.sourceEventId(),
                candidate.factType().name(),
                candidate.authority().name(),
                candidate.conceptCode(),
                candidate.conceptText(),
                candidate.polarity().name(),
                candidate.valuePrimary(),
                candidate.valueSecondary(),
                candidate.unitCode(),
                candidate.temporalityText(),
                candidate.laterality().name(),
                candidate.frequencyText(),
                candidate.routeText(),
                candidate.status().name(),
                candidate.supersedesFactId(),
                Instant.now(),
                List.of());
    }

    private String candidateJson(
            UUID itemId,
            String factType,
            String authority,
            String conceptCode,
            String conceptText,
            String polarity,
            String quoteText) {
        return """
                {"facts":[{
                  "transcriptItemId":"%s",
                  "factType":"%s",
                  "authority":"%s",
                  "conceptCode":"%s",
                  "conceptText":"%s",
                  "polarity":"%s",
                  "valuePrimary":null,
                  "valueSecondary":null,
                  "unitCode":null,
                  "temporalityText":null,
                  "laterality":"UNSPECIFIED",
                  "frequencyText":null,
                  "routeText":null,
                  "quoteText":"%s"
                }]}
                """.formatted(
                itemId,
                factType,
                authority,
                conceptCode,
                conceptText,
                polarity,
                quoteText);
    }

    private String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
