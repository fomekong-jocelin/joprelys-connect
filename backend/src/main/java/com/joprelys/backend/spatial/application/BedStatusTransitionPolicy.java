package com.joprelys.backend.spatial.application;

import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BedStatusTransitionPolicy {

    public void validateManualTransition(
            BedStatus currentStatus,
            BedStatus requestedStatus,
            boolean hasActiveAssignment) {
        Objects.requireNonNull(currentStatus, "Le statut actuel du lit est obligatoire.");
        Objects.requireNonNull(requestedStatus, "Le nouveau statut du lit est obligatoire.");

        if (requestedStatus == BedStatus.OCCUPIED) {
            throw conflict("L'occupation d'un lit doit être créée par une admission ou un transfert.");
        }

        if (hasActiveAssignment) {
            throw conflict(
                    "Le statut d'un lit affecté ne peut pas être modifié manuellement. "
                            + "Utilisez le transfert ou la sortie physique.");
        }

        if (currentStatus == BedStatus.OCCUPIED) {
            throw conflict(
                    "Le lit est marqué occupé sans affectation active. "
                            + "Une réconciliation est requise avant toute transition.");
        }
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
