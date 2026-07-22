package com.joprelys.backend.spatial.application;

import com.joprelys.backend.spatial.infrastructure.persistence.BedCapacityStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedReadinessStatus;
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
            BedCapacityStatus capacityStatus,
            boolean hasActiveAssignment) {
        Objects.requireNonNull(currentStatus, "Le statut actuel du lit est obligatoire.");
        Objects.requireNonNull(requestedStatus, "Le nouveau statut du lit est obligatoire.");
        Objects.requireNonNull(capacityStatus, "L'état de capacité du lit est obligatoire.");

        if (requestedStatus == BedStatus.OCCUPIED) {
            throw conflict("L'occupation d'un lit doit être créée par une admission ou un transfert.");
        }

        rejectUsedBed(currentStatus, hasActiveAssignment);

        if (requestedStatus == BedStatus.FREE && capacityStatus == BedCapacityStatus.CLOSED) {
            throw conflict("Le lit est fermé. Rouvrez sa capacité avant de le déclarer prêt et disponible.");
        }
    }

    public void validateCleaningTransition(
            BedStatus currentStatus,
            BedReadinessStatus currentReadinessStatus,
            BedReadinessStatus requestedReadinessStatus,
            BedCapacityStatus capacityStatus,
            boolean hasActiveAssignment) {
        validateReadinessTransition(
                currentStatus,
                currentReadinessStatus,
                requestedReadinessStatus,
                capacityStatus,
                hasActiveAssignment,
                BedReadinessStatus.CLEANING,
                "nettoyage");
    }

    public void validateMaintenanceTransition(
            BedStatus currentStatus,
            BedReadinessStatus currentReadinessStatus,
            BedReadinessStatus requestedReadinessStatus,
            BedCapacityStatus capacityStatus,
            boolean hasActiveAssignment) {
        validateReadinessTransition(
                currentStatus,
                currentReadinessStatus,
                requestedReadinessStatus,
                capacityStatus,
                hasActiveAssignment,
                BedReadinessStatus.MAINTENANCE,
                "maintenance");
    }

    public void validateCapacityTransition(
            BedStatus currentStatus,
            BedCapacityStatus requestedCapacityStatus,
            boolean hasActiveAssignment) {
        Objects.requireNonNull(currentStatus, "Le statut actuel du lit est obligatoire.");
        Objects.requireNonNull(requestedCapacityStatus, "Le nouvel état de capacité est obligatoire.");
        rejectUsedBed(currentStatus, hasActiveAssignment);
    }

    private void validateReadinessTransition(
            BedStatus currentStatus,
            BedReadinessStatus currentReadinessStatus,
            BedReadinessStatus requestedReadinessStatus,
            BedCapacityStatus capacityStatus,
            boolean hasActiveAssignment,
            BedReadinessStatus managedStatus,
            String operationLabel) {
        Objects.requireNonNull(currentReadinessStatus, "L'état de préparation actuel est obligatoire.");
        Objects.requireNonNull(requestedReadinessStatus, "Le nouvel état de préparation est obligatoire.");
        rejectUsedBed(currentStatus, hasActiveAssignment);

        if (requestedReadinessStatus != BedReadinessStatus.READY
                && requestedReadinessStatus != managedStatus) {
            throw conflict("Cette opération ne peut gérer que l'état " + operationLabel + " ou la remise à disposition.");
        }
        if (currentReadinessStatus != BedReadinessStatus.READY
                && currentReadinessStatus != managedStatus) {
            throw conflict("Le lit est actuellement géré par un autre circuit opérationnel.");
        }
        if (requestedReadinessStatus == BedReadinessStatus.READY
                && capacityStatus == BedCapacityStatus.CLOSED) {
            throw conflict("Le lit est fermé. Rouvrez sa capacité avant de confirmer sa remise à disposition.");
        }
    }

    private void rejectUsedBed(BedStatus currentStatus, boolean hasActiveAssignment) {
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
