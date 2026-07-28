package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.domain.AiChatResponse;
import java.util.Map;

/**
 * Dedicated gateway for the clinician-triggered deep final review.
 *
 * <p>This path is intentionally separate from the continuous consultation model
 * so an expensive reasoning model is never invoked once per speech turn.</p>
 */
public interface FinalClinicalReviewGateway {

    AiChatResponse review(Map<String, String> acceptedDraft, String locale);
}
