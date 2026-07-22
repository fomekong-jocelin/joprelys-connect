package com.joprelys.backend.auth.rbac;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catalogue spécialisé des permissions d'écriture du séjour hospitalier.
 *
 * <p>La prescription médicamenteuse n'est volontairement pas définie ici : le contrat actuel
 * expose uniquement l'administration d'un médicament. Une future permission de prescription
 * devra être introduite avec un endpoint et un modèle métier distincts.</p>
 */
public final class HospitalizationPermissionCatalog {

    public static final String ADMIT = "HOSPITALIZATION_ADMIT";
    public static final String NOTE_WRITE = "HOSPITALIZATION_NOTE_WRITE";
    public static final String CONSENT_MANAGE = "HOSPITALIZATION_CONSENT_MANAGE";
    public static final String CARE_WRITE = "HOSPITALIZATION_CARE_WRITE";
    public static final String MEDICATION_ADMINISTER = "HOSPITALIZATION_MEDICATION_ADMINISTER";
    public static final String CONSUMABLE_MANAGE = "HOSPITALIZATION_CONSUMABLE_MANAGE";

    private HospitalizationPermissionCatalog() {
    }

    public static List<PermissionDefinition> permissions() {
        return List.of(
                new PermissionDefinition(ADMIT, "Admettre un patient", "Créer un séjour hospitalier et affecter un lit disponible."),
                new PermissionDefinition(NOTE_WRITE, "Renseigner les notes d'hospitalisation", "Ajouter une observation ou une transmission attribuée au séjour."),
                new PermissionDefinition(CONSENT_MANAGE, "Gérer les consentements hospitaliers", "Enregistrer un consentement, sa signature et son éventuelle pièce justificative."),
                new PermissionDefinition(CARE_WRITE, "Renseigner les soins hospitaliers", "Tracer les soins et actes infirmiers réalisés pendant le séjour."),
                new PermissionDefinition(MEDICATION_ADMINISTER, "Tracer l'administration médicamenteuse", "Enregistrer l'administration effective d'un médicament sans créer ni modifier une prescription."),
                new PermissionDefinition(CONSUMABLE_MANAGE, "Tracer les consommables du séjour", "Enregistrer les consommables utilisés pour le patient hospitalisé."));
    }

    public static Map<String, Set<String>> systemRolePermissions() {
        return Map.of(
                RbacCatalog.ROLE_ADMIN_CLINIQUE, Set.of(
                        ADMIT, NOTE_WRITE, CONSENT_MANAGE, CARE_WRITE,
                        MEDICATION_ADMINISTER, CONSUMABLE_MANAGE),
                "MEDECIN", Set.of(ADMIT, NOTE_WRITE, CONSENT_MANAGE),
                "INFIRMIER", Set.of(NOTE_WRITE, CARE_WRITE, MEDICATION_ADMINISTER, CONSUMABLE_MANAGE),
                "RESPONSABLE_HOSPITALISATION", Set.of(ADMIT));
    }

    public record PermissionDefinition(String code, String name, String description) {
    }
}
