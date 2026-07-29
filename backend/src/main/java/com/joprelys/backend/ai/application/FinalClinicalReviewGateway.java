package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.domain.AiChatResponse;
import java.util.Map;

/**
 * Dedicated gateway for the clinician-triggered deep final review.
 *
 * <p>This path is intentionally separate from the continuous consultation model
 * so an expensive reasoning model is never invoked once per speech turn. The
 * durable transcript is supplied only on this explicit final-review action so the
 * reviewer can recover an explicitly dictated fact that the fast extraction pass
 * may have missed.</p>
 */
public interface FinalClinicalReviewGateway {

    AiChatResponse review(
            Map<String, String> acceptedDraft,
            String sourceTranscript,
            String locale);
}
