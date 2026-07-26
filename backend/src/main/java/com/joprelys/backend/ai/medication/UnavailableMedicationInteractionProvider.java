package com.joprelys.backend.ai.medication;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "joprelys.ai.medication-safety",
        name = "interaction-provider",
        havingValue = "none",
        matchIfMissing = true)
public class UnavailableMedicationInteractionProvider implements MedicationInteractionProvider {

    @Override
    public InteractionResult check(
            List<MedicationKnowledgeProvider.MedicationKnowledge> medications) {
        return InteractionResult.unavailable("NONE");
    }
}
