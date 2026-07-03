package com.joprelys.backend.prescription.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * DTO de requête pour la création ou mise à jour d'un stock de médicament (STORY-1103).
 */
public record CreateDrugStockRequest(
        @NotBlank(message = "Le nom du médicament est obligatoire.")
        String drugName,
        String genericName,
        String unit,
        @Min(value = 0, message = "La quantité ne peut pas être négative.")
        int quantityAvailable,
        @Min(value = 0, message = "Le seuil minimum ne peut pas être négatif.")
        int minimumThreshold,
        String batchNumber,
        LocalDate expiryDate,
        String supplier
) {}
