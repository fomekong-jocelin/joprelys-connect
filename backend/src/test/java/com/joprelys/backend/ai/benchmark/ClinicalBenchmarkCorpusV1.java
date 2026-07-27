package com.joprelys.backend.ai.benchmark;

import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Evidence;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Fact;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Operation;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Scenario;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.TranscriptTurn;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ClinicalBenchmarkCorpusV1 {

    static final String VERSION = "clinical-ai-benchmark-v1";
    private static final long MINUTE = 60_000L;
    private static final String[] DISTRACTORS = {
            "Nous poursuivons calmement l'entretien sans ajouter de nouvelle information médicale.",
            "D'accord, je vous écoute et nous continuons la conversation.",
            "Je prends un instant pour réfléchir avant de poursuivre.",
            "Nous revenons ensuite au déroulement normal de l'entretien.",
            "Très bien, continuons sans nouvelle donnée clinique à ce moment.",
            "Le reste de cet échange ne contient pas de nouveau fait médical.",
            "Nous poursuivons la discussion générale quelques instants.",
            "D'accord, je reste attentif et nous continuons l'entretien."
    };

    private ClinicalBenchmarkCorpusV1() {
    }

    static List<Scenario> scenarios() {
        return List.of(fiveMinutes(), fifteenMinutes(), thirtyMinutes(), sixtyMinutes());
    }

    private static Scenario fiveMinutes() {
        TranscriptTurn pain = turn(
                "s5-pain", 40_000, "PATIENT",
                "J'ai une douleur abdominale depuis trois jours à droite.");
        TranscriptTurn fever = turn(
                "s5-fever", 95_000, "PATIENT",
                "Je n'ai pas de fièvre.");
        TranscriptTurn bp = turn(
                "s5-bp", 180_000, "DOCTOR",
                "La tension artérielle est à 120 sur 80 mmHg.");

        Fact painFact = fact(
                "s5-f-pain", false, "SYMPTOM", "PATIENT_REPORTED",
                "ABDOMINAL_PAIN", "douleur abdominale", "POSITIVE",
                null, null, null, "depuis trois jours", "RIGHT", null, null,
                evidence(pain, "douleur abdominale depuis trois jours à droite"));
        Fact feverFact = fact(
                "s5-f-fever", false, "SYMPTOM", "PATIENT_REPORTED",
                "FEVER", "fièvre", "NEGATIVE",
                null, null, null, null, "UNSPECIFIED", null, null,
                evidence(fever, "pas de fièvre"));
        Fact bpFact = fact(
                "s5-f-bp", true, "VITAL", "CLINICIAN_OBSERVED",
                "BP", "tension artérielle", "POSITIVE",
                "120", "80", "MMHG", null, "UNSPECIFIED", null, null,
                evidence(bp, "tension artérielle est à 120 sur 80 mmHg"));

        return scenario(
                "fr-5m-baseline", 5,
                List.of(pain, fever, bp),
                List.of(),
                List.of(painFact, feverFact, bpFact),
                List.of(
                        add(painFact),
                        add(feverFact),
                        add(bpFact)));
    }

    private static Scenario fifteenMinutes() {
        TranscriptTurn allergy = turn(
                "s15-allergy", 90_000, "PATIENT",
                "Je suis allergique à la pénicilline.");
        TranscriptTurn metformin = turn(
                "s15-metformin", 240_000, "PATIENT",
                "Je prends metformine 500 mg deux fois par jour par voie orale.");
        TranscriptTurn assessment = turn(
                "s15-assessment", 510_000, "DOCTOR",
                "Je suspecte un paludisme.");
        TranscriptTurn order = turn(
                "s15-order", 650_000, "DOCTOR",
                "Je prescris une goutte épaisse.");

        Fact allergyFact = fact(
                "s15-f-allergy", true, "ALLERGY", "PATIENT_REPORTED",
                "PENICILLIN", "pénicilline", "POSITIVE",
                null, null, null, null, "UNSPECIFIED", null, null,
                evidence(allergy, "allergique à la pénicilline"));
        Fact metforminFact = fact(
                "s15-f-metformin", true, "MEDICATION", "PATIENT_REPORTED",
                "METFORMIN", "metformine", "POSITIVE",
                "500", null, "MG", null, "UNSPECIFIED", "deux fois par jour", "voie orale",
                evidence(metformin, "metformine 500 mg deux fois par jour par voie orale"));
        Fact assessmentFact = fact(
                "s15-f-assessment", true, "ASSESSMENT", "CLINICIAN_DECISION",
                "MALARIA", "paludisme", "UNCERTAIN",
                null, null, null, null, "UNSPECIFIED", null, null,
                evidence(assessment, "suspecte un paludisme"));
        Fact orderFact = fact(
                "s15-f-order", true, "ORDER", "CLINICIAN_DECISION",
                "THICK_BLOOD_FILM", "goutte épaisse", "POSITIVE",
                null, null, null, null, "UNSPECIFIED", null, null,
                evidence(order, "prescris une goutte épaisse"));

        return scenario(
                "fr-15m-medication-assessment", 15,
                List.of(allergy, metformin, assessment, order),
                List.of(),
                List.of(allergyFact, metforminFact, assessmentFact, orderFact),
                List.of(
                        add(allergyFact),
                        add(metforminFact),
                        add(assessmentFact),
                        add(orderFact)));
    }

    private static Scenario thirtyMinutes() {
        TranscriptTurn medicationInitial = turn(
                "s30-med-initial", 60_000, "DOCTOR",
                "Je prescris amoxicilline 500 mg trois fois par jour par voie orale.");
        TranscriptTurn orderInitial = turn(
                "s30-order-initial", 130_000, "DOCTOR",
                "Je demande un scanner abdominal.");
        TranscriptTurn nauseaInitial = turn(
                "s30-nausea-initial", 210_000, "PATIENT",
                "J'ai des nausées.");
        TranscriptTurn medicationCancel = turn(
                "s30-med-cancel", 1_280_000, "DOCTOR",
                "J'annule amoxicilline.");
        TranscriptTurn orderCancel = turn(
                "s30-order-cancel", 1_360_000, "DOCTOR",
                "J'annule le scanner abdominal.");
        TranscriptTurn nauseaNegated = turn(
                "s30-nausea-negated", 1_480_000, "PATIENT",
                "Je n'ai pas de nausées.");

        Fact medication = fact(
                "s30-f-medication", true, "MEDICATION", "CLINICIAN_DECISION",
                "AMOXICILLIN", "amoxicilline", "POSITIVE",
                "500", null, "MG", null, "UNSPECIFIED", "trois fois par jour", "voie orale",
                evidence(medicationInitial, "amoxicilline 500 mg trois fois par jour par voie orale"));
        Fact order = fact(
                "s30-f-order", true, "ORDER", "CLINICIAN_DECISION",
                "ABDOMINAL_CT", "scanner abdominal", "POSITIVE",
                null, null, null, null, "UNSPECIFIED", null, null,
                evidence(orderInitial, "scanner abdominal"));
        Fact nausea = fact(
                "s30-f-nausea", false, "SYMPTOM", "PATIENT_REPORTED",
                "NAUSEA", "nausées", "POSITIVE",
                null, null, null, null, "UNSPECIFIED", null, null,
                evidence(nauseaInitial, "nausées"));

        return scenario(
                "fr-30m-explicit-retractions", 30,
                List.of(
                        medicationInitial, orderInitial, nauseaInitial,
                        medicationCancel, orderCancel, nauseaNegated),
                List.of(medication, order, nausea),
                List.of(),
                List.of(
                        retract(
                                medication,
                                "CLINICIAN_CANCELLATION",
                                evidence(medicationCancel, "annule amoxicilline")),
                        retract(
                                order,
                                "CLINICIAN_CANCELLATION",
                                evidence(orderCancel, "annule le scanner abdominal")),
                        retract(
                                nausea,
                                "EXPLICIT_NEGATION",
                                evidence(nauseaNegated, "pas de nausées"))));
    }

    private static Scenario sixtyMinutes() {
        TranscriptTurn kneeInitial = turn(
                "s60-knee-initial", 70_000, "PATIENT",
                "J'ai une douleur au genou gauche depuis deux semaines.");
        TranscriptTurn diabetesInitial = turn(
                "s60-diabetes-initial", 150_000, "PATIENT",
                "J'ai un diabète de type 2.");
        TranscriptTurn paracetamolInitial = turn(
                "s60-paracetamol-initial", 230_000, "PATIENT",
                "Je prends paracétamol 1 g trois fois par jour par voie orale.");
        TranscriptTurn kneeCorrection = turn(
                "s60-knee-correction", 2_620_000, "PATIENT",
                "Correction, la douleur au genou dure depuis deux semaines mais elle est à droite, pas à gauche.");
        TranscriptTurn diabetesRestated = turn(
                "s60-diabetes-restated", 2_760_000, "PATIENT",
                "J'ai toujours mon diabète de type 2.");
        TranscriptTurn paracetamolChange = turn(
                "s60-paracetamol-change", 2_940_000, "PATIENT",
                "Je prends maintenant paracétamol 500 mg trois fois par jour par voie orale.");
        TranscriptTurn assessment = turn(
                "s60-assessment", 3_180_000, "DOCTOR",
                "Je retiens une entorse du genou droit.");
        TranscriptTurn order = turn(
                "s60-order", 3_300_000, "DOCTOR",
                "Je prescris une radiographie du genou droit.");

        Fact kneeLeft = fact(
                "s60-f-knee-left", false, "SYMPTOM", "PATIENT_REPORTED",
                "KNEE_PAIN", "douleur au genou", "POSITIVE",
                null, null, null, "depuis deux semaines", "LEFT", null, null,
                evidence(kneeInitial, "douleur au genou gauche depuis deux semaines"));
        Fact diabetes = fact(
                "s60-f-diabetes", true, "HISTORY", "PATIENT_REPORTED",
                "TYPE_2_DIABETES", "diabète de type 2", "POSITIVE",
                null, null, null, null, "UNSPECIFIED", null, null,
                evidence(diabetesInitial, "diabète de type 2"));
        Fact paracetamol1g = fact(
                "s60-f-paracetamol-1g", true, "MEDICATION", "PATIENT_REPORTED",
                "PARACETAMOL", "paracétamol", "POSITIVE",
                "1", null, "G", null, "UNSPECIFIED", "trois fois par jour", "voie orale",
                evidence(paracetamolInitial, "paracétamol 1 g trois fois par jour par voie orale"));

        Fact kneeRight = fact(
                "s60-f-knee-right", false, "SYMPTOM", "PATIENT_REPORTED",
                "KNEE_PAIN", "douleur au genou", "POSITIVE",
                null, null, null, "depuis deux semaines", "RIGHT", null, null,
                evidence(kneeCorrection, "douleur au genou dure depuis deux semaines mais elle est à droite"));
        Fact paracetamol500 = fact(
                "s60-f-paracetamol-500", true, "MEDICATION", "PATIENT_REPORTED",
                "PARACETAMOL", "paracétamol", "POSITIVE",
                "500", null, "MG", null, "UNSPECIFIED", "trois fois par jour", "voie orale",
                evidence(paracetamolChange, "paracétamol 500 mg trois fois par jour par voie orale"));
        Fact sprain = fact(
                "s60-f-sprain", true, "ASSESSMENT", "CLINICIAN_DECISION",
                "KNEE_SPRAIN", "entorse du genou", "POSITIVE",
                null, null, null, null, "RIGHT", null, null,
                evidence(assessment, "entorse du genou droit"));
        Fact xray = fact(
                "s60-f-xray", true, "ORDER", "CLINICIAN_DECISION",
                "KNEE_XRAY", "radiographie du genou", "POSITIVE",
                null, null, null, null, "RIGHT", null, null,
                evidence(order, "radiographie du genou droit"));

        return scenario(
                "fr-60m-longitudinal-corrections", 60,
                List.of(
                        kneeInitial, diabetesInitial, paracetamolInitial,
                        kneeCorrection, diabetesRestated, paracetamolChange,
                        assessment, order),
                List.of(kneeLeft, diabetes, paracetamol1g),
                List.of(kneeRight, diabetes, paracetamol500, sprain, xray),
                List.of(
                        replace(kneeLeft, kneeRight),
                        keep(diabetes),
                        replace(paracetamol1g, paracetamol500),
                        add(sprain),
                        add(xray)));
    }

    private static Scenario scenario(
            String id,
            int durationMinutes,
            List<TranscriptTurn> clinicalTurns,
            List<Fact> initialFacts,
            List<Fact> expectedFacts,
            List<Operation> expectedOperations) {
        return new Scenario(
                id,
                "fr-CM",
                durationMinutes,
                materialize(durationMinutes, clinicalTurns),
                List.copyOf(initialFacts),
                List.copyOf(expectedFacts),
                List.copyOf(expectedOperations));
    }

    private static List<TranscriptTurn> materialize(
            int durationMinutes,
            List<TranscriptTurn> clinicalTurns) {
        List<TranscriptTurn> turns = new ArrayList<>(clinicalTurns);
        long durationMs = durationMinutes * MINUTE;
        int fillerIndex = 0;
        for (long offset = 10_000; offset < durationMs; offset += 10_000) {
            if (clinicalTurns.stream().anyMatch(turn -> Math.abs(turn.startOffsetMs() - offset) < 5_000)) continue;
            String speaker = fillerIndex % 2 == 0 ? "DOCTOR" : "PATIENT";
            String text = DISTRACTORS[fillerIndex % DISTRACTORS.length];
            turns.add(new TranscriptTurn(
                    "filler-" + durationMinutes + "m-" + fillerIndex,
                    offset,
                    Math.min(offset + 4_000, durationMs),
                    speaker,
                    text));
            fillerIndex++;
        }
        turns.sort(Comparator
                .comparingLong(TranscriptTurn::startOffsetMs)
                .thenComparing(TranscriptTurn::id));
        return List.copyOf(turns);
    }

    private static TranscriptTurn turn(String id, long offsetMs, String speaker, String text) {
        return new TranscriptTurn(id, offsetMs, offsetMs + 5_000, speaker, text);
    }

    private static List<Evidence> evidence(TranscriptTurn turn, String quote) {
        if (!turn.text().contains(quote)) {
            throw new IllegalArgumentException("gold quote not found in transcript: " + quote);
        }
        return List.of(new Evidence(turn.id(), quote));
    }

    private static Fact fact(
            String key,
            boolean critical,
            String factType,
            String authority,
            String conceptCode,
            String conceptText,
            String polarity,
            String valuePrimary,
            String valueSecondary,
            String unitCode,
            String temporality,
            String laterality,
            String frequency,
            String route,
            List<Evidence> evidence) {
        return new Fact(
                key, critical, factType, authority, conceptCode, conceptText, polarity,
                valuePrimary, valueSecondary, unitCode, temporality, laterality,
                frequency, route, evidence);
    }

    private static Operation add(Fact fact) {
        return new Operation("ADD", null, fact, null, List.of());
    }

    private static Operation keep(Fact fact) {
        return new Operation("KEEP", fact.key(), null, null, List.of());
    }

    private static Operation replace(Fact target, Fact result) {
        return new Operation("REPLACE", target.key(), result, null, List.of());
    }

    private static Operation retract(
            Fact target,
            String reason,
            List<Evidence> evidence) {
        return new Operation("RETRACT", target.key(), null, reason, List.copyOf(evidence));
    }
}
