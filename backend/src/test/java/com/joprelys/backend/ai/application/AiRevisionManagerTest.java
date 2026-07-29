package com.joprelys.backend.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AiRevisionManagerTest {

    private final AiRevisionManager manager = new AiRevisionManager();

    @Test
    void repeatedSymptomsMustBeConsolidatedInsteadOfLastWriteWins() {
        AiConsultationSessionState state = state();

        var revision = manager.createRevision(state, List.of(
                change(
                        "symptoms",
                        "Patient âgé de 12 ans présentant une céphalée aiguë depuis trois jours.",
                        "patient âgé de 12 ans qui se plaint d'une céphalée aiguë depuis trois jours"),
                change(
                        "symptoms",
                        "Il n'arrive plus à se lever.",
                        "Il n'arrive plus à se lever")));

        assertThat(revision).isNotNull();
        assertThat(revision.proposals()).hasSize(1);
        assertThat(revision.proposals().getFirst().field()).isEqualTo("symptoms");
        assertThat(revision.proposals().getFirst().proposedValue())
                .contains("céphalée aiguë depuis trois jours")
                .contains("Il n'arrive plus à se lever");

        manager.decideRevision(state, revision.id(), "ACCEPT");
        assertThat(state.draft.get("symptoms"))
                .contains("céphalée aiguë depuis trois jours")
                .contains("Il n'arrive plus à se lever");
    }

    @Test
    void laterNarrativeChunkMustAppendWithoutErasingEarlierAcceptedFact() {
        AiConsultationSessionState state = state();
        state.draft.put("symptoms", "Céphalée aiguë depuis trois jours.");

        var revision = manager.createRevision(state, List.of(
                change("symptoms", "Il n'arrive plus à se lever.", "Il n'arrive plus à se lever")));

        assertThat(revision.proposals().getFirst().proposedValue())
                .contains("Céphalée aiguë depuis trois jours")
                .contains("Il n'arrive plus à se lever");
    }

    @Test
    void laterPrescriptionChunkMustPreserveEarlierMedicationLine() {
        AiConsultationSessionState state = state();
        state.draft.put(
                "prescription",
                "[{\"drugName\":\"Paracétamol\",\"dosage\":\"1000 mg\",\"frequency\":\"matin midi soir\"}]");

        var revision = manager.createRevision(state, List.of(new ParsedChange(
                "prescription",
                "SET",
                "[{\"drugName\":\"Vitafer\",\"route\":\"voie orale\"}]",
                "Deuxième médicament explicitement dicté.",
                "LOW",
                List.of("Vitafer"))));

        assertThat(revision).isNotNull();
        assertThat(revision.proposals()).hasSize(1);
        assertThat(revision.proposals().getFirst().proposedValue())
                .contains("Paracétamol")
                .contains("Vitafer");
    }

    @Test
    void repeatedLabOrdersMustAccumulateWithoutDuplicates() {
        AiConsultationSessionState state = state();
        state.draft.put("labOrders", "[\"NFS\"]");

        var revision = manager.createRevision(state, List.of(new ParsedChange(
                "labOrders",
                "SET",
                "[\"NFS\",\"Goutte épaisse\"]",
                "Examens dictés.",
                "LOW",
                List.of("Goutte épaisse"))));

        assertThat(revision.proposals().getFirst().proposedValue())
                .isEqualTo("[\"NFS\",\"Goutte épaisse\"]");
    }

    @Test
    void duplicateNonAdditiveDiagnosisMustFailClosedRatherThanPickLastOne() {
        AiConsultationSessionState state = state();

        assertThatThrownBy(() -> manager.createRevision(state, List.of(
                change("diagnosis", "Migraine", "Migraine"),
                change("diagnosis", "Céphalée secondaire", "Céphalée secondaire"))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CHANGE_DUPLICATE_FIELD");
    }

    private ParsedChange change(String field, String value, String evidence) {
        return new ParsedChange(
                field,
                "SET",
                value,
                "Fait explicitement dicté.",
                "LOW",
                List.of(evidence));
    }

    private AiConsultationSessionState state() {
        return new AiConsultationSessionState(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now().plusSeconds(600),
                "fr");
    }
}
