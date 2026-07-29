package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedClarification;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provider-agnostic boundary between model output and Joprelys clinical actions.
 *
 * <p>The provider may be OpenAI, Gemini or Claude. None of them directly owns
 * a business capability. This dispatcher converts already parsed model output
 * into the finite set of clinical tools Joprelys permits. The resulting tools
 * still create proposals only; they never persist clinical data.</p>
 *
 * <p>Repeated fields are intentionally transported unchanged. The revision layer
 * owns the clinical merge policy: additive narrative/structured facts are merged,
 * while incompatible repeated fields fail closed. Silently choosing the last model
 * value is never allowed.</p>
 */
final class AiClinicalToolDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AiClinicalToolDispatcher.class);

    ToolPlan dispatch(ParsedResponse response) {
        if (response == null) {
            throw invalidToolPlan();
        }

        List<ClinicalToolInvocation> invocations = new ArrayList<>();
        List<ParsedChange> changes = new ArrayList<>();

        for (ParsedChange change : response.changes()) {
            ClinicalToolType type = toolTypeFor(change.field());
            invocations.add(new ClinicalToolInvocation(
                    type,
                    change.field(),
                    change.operation(),
                    change.reason(),
                    change.uncertainty()));
            changes.add(change);
        }

        ParsedClarification clarification = response.needsClarification()
                ? response.clarification()
                : null;
        if (clarification != null) {
            invocations.add(new ClinicalToolInvocation(
                    ClinicalToolType.ASK_CLARIFICATION,
                    clarification.field(),
                    "ASK",
                    clarification.question(),
                    "UNKNOWN"));
        }

        if (log.isDebugEnabled()) {
            log.debug("Clinical tool plan: {}", invocations.stream()
                    .map(invocation -> invocation.type().name() + ":" + invocation.field())
                    .toList());
        }

        return new ToolPlan(
                List.copyOf(changes),
                clarification,
                List.copyOf(invocations));
    }

    private ClinicalToolType toolTypeFor(String field) {
        return switch (field) {
            case "symptoms", "clinicalExam", "suspectedDiagnosis", "diagnosis",
                    "finalDiagnosis", "conclusion", "advice", "followUp" ->
                    ClinicalToolType.PROPOSE_CLINICAL_NOTE;
            case "prescription" -> ClinicalToolType.PROPOSE_PRESCRIPTION;
            case "labOrders" -> ClinicalToolType.PROPOSE_LAB_ORDERS;
            case "vitals" -> ClinicalToolType.PROPOSE_VITALS;
            default -> throw invalidToolPlan();
        };
    }

    private ResponseStatusException invalidToolPlan() {
        return new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY, "AI_TOOL_PLAN_INVALID");
    }

    enum ClinicalToolType {
        PROPOSE_CLINICAL_NOTE,
        PROPOSE_PRESCRIPTION,
        PROPOSE_LAB_ORDERS,
        PROPOSE_VITALS,
        ASK_CLARIFICATION
    }

    record ClinicalToolInvocation(
            ClinicalToolType type,
            String field,
            String operation,
            String rationale,
            String uncertainty) {
    }

    record ToolPlan(
            List<ParsedChange> changes,
            ParsedClarification clarification,
            List<ClinicalToolInvocation> invocations) {
    }
}
