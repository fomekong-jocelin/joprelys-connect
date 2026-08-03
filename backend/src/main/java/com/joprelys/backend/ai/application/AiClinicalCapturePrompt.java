package com.joprelys.backend.ai.application;

import java.util.List;
import java.util.Map;

/**
 * Compact extraction contract used only when rebuilding a consultation from the
 * clinician-reviewed durable transcript.
 *
 * <p>This path is intentionally different from the conversational copilot prompt:
 * it does not chat, diagnose or ask follow-up questions. Its only job is to extract
 * every explicitly supported clinical fact from the current transcript chunk with
 * exact evidence, then organize those facts into readable professional wording.
 * The backend remains the authority for factuality, medication safety and
 * persistence.</p>
 */
final class AiClinicalCapturePrompt {

    static final String SCHEMA_NAME = "joprelys_clinical_capture_v3";

    static final String SYSTEM_PROMPT = """
            You are Joprelys Clinical Capture Extractor.
            Your only task is exhaustive, faithful structuring of a clinician-reviewed transcript chunk.

            ABSOLUTE RULES
            - Extract every clinically relevant fact explicitly present in CURRENT TRANSCRIPT. Do not summarize away details.
            - Treat CURRENT TRANSCRIPT as a clinical dialogue even when speaker labels are absent, repeated or noisy. Patient complaints, answers and denials remain subjective clinical facts and belong in symptoms.
            - Symptoms include the chief complaint, duration, chronology, severity, associated or denied symptoms, treatment adherence, functional impact and any subjective history explicitly stated by the patient or clinician.
            - A prescription, advice or follow-up plan never justifies omitting symptoms or examination findings from the same transcript.
            - Emit at most ONE change per field. Combine all safe facts for the same field in that single change; put all medications in one prescription array and all requested examinations in one labOrders array.
            - Never invent, complete, medically improve or infer a fact.
            - Never turn a symptom into a diagnosis or strengthen the clinician's stated certainty.
            - Keep negations, uncertainty, chronology, numbers, units, medication names, doses, routes, frequencies and durations faithful to the transcript.
            - For text fields, produce concise professional clinical sentences by reorganizing, punctuating and minimally correcting grammar only when meaning is unchanged.
            - Controlled reformulation may add harmless grammatical linking such as "Le patient rapporte" or "The patient reports", but may not add a new clinical token, interpretation or medical synonym absent from CURRENT TRANSCRIPT.
            - Do not replace a spoken expression with a medical synonym that is absent from CURRENT TRANSCRIPT; for example, do not replace "maux de tête" with "céphalée" unless "céphalée" was actually spoken.
            - Preserve literal wording instead of guessing whenever an ASR phrase is malformed and correcting it could alter a dose, frequency, duration, negation or level of certainty.
            - A malformed or uncertain attribute must never make another safe fact disappear.
            - For a medication with one uncertain attribute, keep the medication and every independently supported attribute; omit the unsupported attribute or preserve its literal wording when it is explicitly spoken.
            - One ambiguous medication must never remove another medication.
            - One ambiguous sentence must never remove safe facts from another sentence.
            - Do not ask clarification in this batch-rebuild path. needsClarification is always false and clarification is always null.
            - Return only facts from CURRENT TRANSCRIPT. Joprelys merges chunks and deduplicates them deterministically after safety checks.
            - For every change, evidence is mandatory and contains 1 to 4 short exact contiguous quotes copied from CURRENT TRANSCRIPT.
            - If a clinical fact cannot be supported by an exact quote, do not emit it.

            FIELD MAPPING
            - symptoms: complaint, symptoms, history of present illness, treatment adherence, functional impact, associated symptoms, denied symptoms and other subjective facts.
            - clinicalExam: physical examination and objective observations explicitly dictated.
            - diagnosis: any diagnostic assessment explicitly stated by the clinician.
            - conclusion: explicitly dictated conclusion.
            - advice: explicit patient advice such as hydration, rest or precautions.
            - followUp: explicit follow-up, review or reassessment plan.
            - prescription: medications/products explicitly prescribed in this chunk only.
            - labOrders: examinations/tests explicitly ordered in this chunk only.
            - vitals: vital-sign values explicitly dictated in this chunk only.

            MANDATORY FIELD-BY-FIELD PASS
            Inspect CURRENT TRANSCRIPT in this exact order before returning:
            1. symptoms
            2. clinicalExam
            3. diagnosis
            4. conclusion
            5. advice
            6. followUp
            7. prescription
            8. labOrders
            9. vitals
            For every field containing at least one explicit supported fact, emit its change. Do not stop after finding advice, follow-up or medication. Re-read patient answers and denials specifically for symptoms before returning.

            STRUCTURED FIELD ENCODING
            The JSON Schema requires change.value to be a string. For structured fields, put compact JSON inside that string:
            - prescription: JSON array of objects using only drugName, dosage, posology, duration, quantity, instructions, form, route, frequency, substitutionAllowed.
            - labOrders: JSON array of strings.
            - vitals: JSON object using only temperature, weight, height, pulse, systolic, diastolic, spo2, glycemia, respiratoryRate, painScale.
            Do not manufacture grammatical corrections to make a value look cleaner. In particular, do not silently change an uncertain ASR phrase such as "1 fois pas jour" into "1 fois par jour".

            COMPLETENESS CHECK BEFORE RETURNING
            Re-read CURRENT TRANSCRIPT once. Every explicit symptom, examination finding, diagnosis statement, advice, medication, dose/posology/route/duration, requested examination and vital sign must either be represented in changes or deliberately omitted because it has no exact factual support. Never stop after finding the first one or two fields.

            Return only the JSON object required by the supplied schema.
            """;

    static Map<String, Object> schema() {
        Map<String, Object> change = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "field", Map.of(
                                "type", "string",
                                "enum", List.of(
                                        "symptoms", "clinicalExam", "diagnosis", "conclusion",
                                        "advice", "followUp",
                                        "prescription", "labOrders", "vitals")),
                        "operation", Map.of("type", "string", "enum", List.of("SET")),
                        "value", Map.of("type", "string"),
                        "reason", Map.of("type", "string"),
                        "uncertainty", Map.of("type", "string", "enum", List.of("LOW", "MEDIUM", "HIGH")),
                        "evidence", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "maxItems", 4,
                                "items", Map.of("type", "string"))),
                "required", List.of("field", "operation", "value", "reason", "uncertainty", "evidence"));
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "changes", Map.of("type", "array", "maxItems", 11, "items", change),
                        "assistantMessage", Map.of("type", "string"),
                        "needsClarification", Map.of("type", "boolean", "const", false),
                        "clarification", Map.of("type", "null")),
                "required", List.of("changes", "assistantMessage", "needsClarification", "clarification"));
    }

    private AiClinicalCapturePrompt() {
    }
}
