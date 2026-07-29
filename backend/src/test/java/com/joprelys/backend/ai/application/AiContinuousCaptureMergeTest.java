package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiContinuousCaptureMergeTest {

    private final AiContinuousCaptureMerge merge = new AiContinuousCaptureMerge(new ObjectMapper());

    @Test
    void shouldAppendNarrativeFactsInsteadOfReplacingEarlierChunk() {
        ParsedChange change = change(
                "symptoms",
                "Le patient n'arrive plus à se lever.",
                "n'arrive plus à se lever");

        var result = merge.merge(
                Map.of("symptoms", "Céphalée aiguë depuis trois jours."),
                List.of(change));

        assertEquals(1, result.size());
        assertTrue(result.getFirst().proposedValue().contains("Céphalée aiguë depuis trois jours."));
        assertTrue(result.getFirst().proposedValue().contains("n'arrive plus à se lever"));
    }

    @Test
    void shouldKeepPrescriptionLinesFromPreviousAndCurrentChunks() {
        ParsedChange change = change(
                "prescription",
                "[{\"drugName\":\"Vitafer\",\"route\":\"voie orale\"}]",
                "Vitafer");

        var result = merge.merge(
                Map.of("prescription", "[{\"drugName\":\"Paracétamol\",\"dosage\":\"1000 mg\"}]"),
                List.of(change));

        String prescription = result.getFirst().proposedValue();
        assertTrue(prescription.contains("Paracétamol"));
        assertTrue(prescription.contains("1000 mg"));
        assertTrue(prescription.contains("Vitafer"));
    }

    @Test
    void shouldUnionLabOrdersAcrossChunks() {
        ParsedChange change = change(
                "labOrders",
                "[\"CRP\",\"Goutte épaisse\"]",
                "Goutte épaisse");

        var result = merge.merge(
                Map.of("labOrders", "[\"NFS\",\"CRP\"]"),
                List.of(change));

        String orders = result.getFirst().proposedValue();
        assertTrue(orders.contains("NFS"));
        assertTrue(orders.contains("CRP"));
        assertTrue(orders.contains("Goutte épaisse"));
    }

    private ParsedChange change(String field, String value, String evidence) {
        return new ParsedChange(
                field,
                "SET",
                value,
                "Explicit capture fact",
                "LOW",
                List.of(evidence));
    }
}
