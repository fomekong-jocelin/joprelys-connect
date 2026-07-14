package com.joprelys.backend.auth.rbac;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RbacCatalog {

    public static final String ROLE_ADMIN_JOPRELYS = "ADMIN_JOPRELYS";
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_ADMIN_CLINIQUE = "ADMIN_CLINIQUE";

    public static final String PERMISSION_USER_READ = "USER_READ";
    public static final String PERMISSION_USER_MANAGE = "USER_MANAGE";
    public static final String PERMISSION_RBAC_READ = "RBAC_READ";
    public static final String PERMISSION_RBAC_MANAGE = "RBAC_MANAGE";
    public static final String PERMISSION_AUTH_SESSION_MANAGE = "AUTH_SESSION_MANAGE";
    public static final String PERMISSION_EMERGENCY_MEDICO_LEGAL_READ = "EMERGENCY_MEDICO_LEGAL_READ";
    public static final String PERMISSION_EMERGENCY_MEDICO_LEGAL_WRITE = "EMERGENCY_MEDICO_LEGAL_WRITE";
    public static final String PERMISSION_EMERGENCY_BELONGINGS_WRITE = "EMERGENCY_BELONGINGS_WRITE";

    private RbacCatalog() {
    }

    public static UUID roleId(String code) {
        return UUID.nameUUIDFromBytes(("joprelys-role:" + code).getBytes(StandardCharsets.UTF_8));
    }

    public static List<PermissionDefinition> permissions() {
        return List.of(
                permission("USER_READ", "ADMINISTRATION", "Consulter les utilisateurs", "Consulter les comptes de l'établissement."),
                permission("USER_MANAGE", "ADMINISTRATION", "Gérer les utilisateurs", "Inviter, modifier, activer ou désactiver un utilisateur."),
                permission("RBAC_READ", "ADMINISTRATION", "Consulter le RBAC", "Consulter les rôles, permissions et affectations."),
                permission("RBAC_MANAGE", "ADMINISTRATION", "Administrer le RBAC", "Créer des rôles personnalisés et gérer leurs permissions."),
                permission(PERMISSION_AUTH_SESSION_MANAGE, "ADMINISTRATION", "Gérer les sessions utilisateurs", "Consulter et révoquer les sessions des collaborateurs du même établissement."),
                permission("ORGANIZATION_MANAGE", "ADMINISTRATION", "Gérer les établissements", "Créer et administrer les établissements."),
                permission("PATIENT_READ", "PATIENT", "Consulter les patients", "Consulter les informations administratives des patients."),
                permission("PATIENT_WRITE", "PATIENT", "Gérer les patients", "Créer et modifier les informations administratives des patients."),
                permission("PATIENT_MERGE", "PATIENT", "Fusionner les dossiers patients", "Analyser, ignorer et fusionner les doublons patients."),
                permission("PATIENT_EMERGENCY_ACCESS", "PATIENT", "Déclencher un accès d'urgence", "Ouvrir un accès exceptionnel et traçable au dossier patient."),
                permission("CLINICAL_READ", "CLINIQUE", "Consulter le dossier clinique", "Consulter les données cliniques autorisées."),
                permission("CLINICAL_WRITE", "CLINIQUE", "Renseigner le dossier clinique", "Créer et modifier les données cliniques autorisées."),
                permission("EMERGENCY_READ", "URGENCES", "Consulter les urgences", "Consulter les dossiers d'urgence et leur historique."),
                permission("EMERGENCY_WRITE", "URGENCES", "Prendre en charge une urgence", "Créer une urgence et consigner les soins de réanimation."),
                permission("EMERGENCY_STABILIZE", "URGENCES", "Stabiliser une urgence", "Clôturer la prise en charge et orienter le patient."),
                permission(PERMISSION_EMERGENCY_MEDICO_LEGAL_READ, "URGENCES", "Consulter le contexte médico-légal", "Consulter les tiers, la capacité, la base légale et la chaîne de possession d'une urgence."),
                permission(PERMISSION_EMERGENCY_MEDICO_LEGAL_WRITE, "URGENCES", "Documenter le contexte médico-légal", "Tracer l'incapacité, les déclarations et la base légale de prise en charge urgente."),
                permission(PERMISSION_EMERGENCY_BELONGINGS_WRITE, "URGENCES", "Gérer les effets personnels", "Inventorier, sceller, transférer et remettre les effets personnels avec une chaîne de possession."),
                permission("LAB_ORDER_READ", "LABORATOIRE", "Consulter les analyses d'un patient", "Consulter les demandes et résultats de laboratoire rattachés à un patient autorisé."),
                permission("LAB_QUEUE_READ", "LABORATOIRE", "Consulter la file du laboratoire", "Consulter la file globale des demandes d'analyse de l'établissement."),
                permission("LAB_ORDER_CREATE", "LABORATOIRE", "Prescrire une analyse", "Créer une demande d'analyse pour un patient."),
                permission("LAB_ORDER_WRITE", "LABORATOIRE", "Traiter les analyses", "Prendre en charge et publier les résultats de laboratoire."),
                permission("PHARMACY_PRESCRIPTION_READ", "PHARMACIE", "Consulter les prescriptions", "Vérifier les prescriptions destinées à la pharmacie."),
                permission("PHARMACY_STOCK_MANAGE", "PHARMACIE", "Gérer les stocks pharmacie", "Gérer le stock et les mouvements de médicaments."),
                permission("STOCK_READ", "STOCK", "Consulter les stocks", "Consulter les articles et niveaux de stock."),
                permission("STOCK_MANAGE", "STOCK", "Gérer les stocks", "Créer et traiter les mouvements de stock."),
                permission("HOSPITALIZATION_READ", "HOSPITALISATION", "Consulter les hospitalisations", "Consulter les séjours, chambres et lits."),
                permission("HOSPITALIZATION_MANAGE", "HOSPITALISATION", "Gérer les hospitalisations", "Affecter les lits et piloter les séjours."),
                permission("BILLING_INVOICE_READ", "FACTURATION", "Consulter les factures", "Consulter les factures et leurs soldes."),
                permission("BILLING_INVOICE_WRITE", "FACTURATION", "Créer et valider les factures", "Créer, modifier et valider les factures."),
                permission("BILLING_INVOICE_CANCEL", "FACTURATION", "Annuler les factures", "Annuler une facture et gérer les actions exceptionnelles."),
                permission("CASH_QUEUE_READ", "CAISSE", "Consulter la file d'encaissement", "Consulter les règlements patient en attente."),
                permission("CASH_PAYMENT_COLLECT", "CAISSE", "Encaisser un patient", "Enregistrer un règlement patient."),
                permission("CASH_SESSION_OPEN", "CAISSE", "Ouvrir une caisse", "Ouvrir une session de caisse."),
                permission("CASH_SESSION_CLOSE", "CAISSE", "Clôturer une caisse", "Clôturer une session de caisse."),
                permission("CASH_MOVEMENT_WRITE", "CAISSE", "Consigner un mouvement", "Enregistrer une dépense ou un versement banque."),
                permission("CASH_HISTORY_READ", "CAISSE", "Consulter l'historique caisse", "Consulter les sessions, mouvements et bordereaux de clôture."),
                permission("CASH_DISCREPANCY_RESOLVE", "CAISSE", "Résoudre un écart de caisse", "Documenter et clôturer le traitement d'un écart de caisse."),
                permission("INSURANCE_BORDEREAU_READ", "ASSURANCE", "Consulter les bordereaux", "Consulter les bordereaux et créances assurance."),
                permission("INSURANCE_BORDEREAU_PROGRESS", "ASSURANCE", "Faire progresser un bordereau", "Envoyer, recevoir et documenter un bordereau."),
                permission("INSURANCE_BORDEREAU_SETTLE", "ASSURANCE", "Accepter et régler un bordereau", "Accepter, rejeter et enregistrer les règlements assurance."),
                permission("ACCOUNTING_DASHBOARD_READ", "COMPTABILITE", "Consulter le pilotage financier", "Consulter les indicateurs financiers et les écarts."),
                permission("ACCOUNTING_EXPORT", "COMPTABILITE", "Exporter la comptabilité", "Générer les exports comptables."),
                permission("AUDIT_READ", "AUDIT", "Consulter les journaux", "Consulter les journaux d'audit autorisés."));
    }

    public static List<RoleDefinition> systemRoles() {
        Map<String, Set<String>> mappings = new LinkedHashMap<>();
        Set<String> all = permissionCodes();

        mappings.put(ROLE_ADMIN_JOPRELYS, set("ORGANIZATION_MANAGE"));
        mappings.put(ROLE_SUPER_ADMIN, set("ORGANIZATION_MANAGE"));
        mappings.put(ROLE_ADMIN_CLINIQUE, without(all, "ORGANIZATION_MANAGE"));
        mappings.put("DAF", set(
                "USER_READ", "RBAC_READ", "BILLING_INVOICE_READ", "CASH_HISTORY_READ", "CASH_DISCREPANCY_RESOLVE",
                "INSURANCE_BORDEREAU_READ", "INSURANCE_BORDEREAU_PROGRESS", "INSURANCE_BORDEREAU_SETTLE",
                "ACCOUNTING_DASHBOARD_READ", "ACCOUNTING_EXPORT", "AUDIT_READ"));
        mappings.put("SECRETAIRE_COMPTABLE", set(
                "BILLING_INVOICE_READ", "BILLING_INVOICE_WRITE", "INSURANCE_BORDEREAU_READ",
                "INSURANCE_BORDEREAU_PROGRESS", "ACCOUNTING_DASHBOARD_READ"));
        mappings.put("CAISSIER", set(
                "BILLING_INVOICE_READ", "CASH_QUEUE_READ", "CASH_PAYMENT_COLLECT", "CASH_SESSION_OPEN",
                "CASH_SESSION_CLOSE", "CASH_MOVEMENT_WRITE", "CASH_HISTORY_READ"));
        mappings.put("AGENT_ACCUEIL", set(
                "PATIENT_READ", "PATIENT_WRITE", "EMERGENCY_READ",
                "BILLING_INVOICE_READ", "BILLING_INVOICE_WRITE"));
        mappings.put("MEDECIN", set(
                "PATIENT_READ", "PATIENT_EMERGENCY_ACCESS", "CLINICAL_READ", "CLINICAL_WRITE",
                "EMERGENCY_READ", "EMERGENCY_WRITE", "EMERGENCY_STABILIZE",
                PERMISSION_EMERGENCY_MEDICO_LEGAL_READ,
                PERMISSION_EMERGENCY_MEDICO_LEGAL_WRITE,
                PERMISSION_EMERGENCY_BELONGINGS_WRITE,
                "LAB_ORDER_READ", "LAB_ORDER_CREATE", "HOSPITALIZATION_READ", "HOSPITALIZATION_MANAGE",
                "BILLING_INVOICE_READ", "BILLING_INVOICE_WRITE"));
        mappings.put("INFIRMIER", set(
                "PATIENT_READ", "PATIENT_WRITE", "PATIENT_EMERGENCY_ACCESS", "CLINICAL_READ", "CLINICAL_WRITE",
                "EMERGENCY_READ", "EMERGENCY_WRITE",
                PERMISSION_EMERGENCY_MEDICO_LEGAL_READ,
                PERMISSION_EMERGENCY_BELONGINGS_WRITE,
                "LAB_ORDER_READ", "HOSPITALIZATION_READ", "HOSPITALIZATION_MANAGE"));
        mappings.put("BIOLOGISTE", set("PATIENT_READ", "LAB_ORDER_READ", "LAB_QUEUE_READ", "LAB_ORDER_WRITE"));
        mappings.put("PHARMACIEN", set(
                "PHARMACY_PRESCRIPTION_READ", "PHARMACY_STOCK_MANAGE", "STOCK_READ", "STOCK_MANAGE"));
        mappings.put("GESTIONNAIRE_STOCK", set("STOCK_READ", "STOCK_MANAGE"));
        mappings.put("RESPONSABLE_HOSPITALISATION", set(
                "PATIENT_READ", "HOSPITALIZATION_READ", "HOSPITALIZATION_MANAGE"));
        mappings.put("AUDITEUR", set(
                "AUDIT_READ", "BILLING_INVOICE_READ", "CASH_HISTORY_READ", "ACCOUNTING_DASHBOARD_READ",
                PERMISSION_EMERGENCY_MEDICO_LEGAL_READ));
        mappings.put("PATIENT", Set.of());

        return mappings.entrySet().stream()
                .map(entry -> new RoleDefinition(
                        roleId(entry.getKey()),
                        entry.getKey(),
                        roleName(entry.getKey()),
                        "Rôle système Joprelys.",
                        isSystemRoleAssignable(entry.getKey()),
                        entry.getValue()))
                .toList();
    }

    public static Set<String> adminRoleCodes() {
        return Set.of(ROLE_ADMIN_JOPRELYS, ROLE_SUPER_ADMIN, ROLE_ADMIN_CLINIQUE);
    }

    public static Set<String> platformRoleCodes() {
        return Set.of(ROLE_ADMIN_JOPRELYS, ROLE_SUPER_ADMIN);
    }

    public static Set<String> platformPermissionCodes() {
        return Set.of("ORGANIZATION_MANAGE");
    }

    public static Set<String> permissionCodes() {
        return permissions().stream().map(PermissionDefinition::code)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public static Set<String> permissionsForLegacyRoles(Set<String> roleCodes) {
        LinkedHashSet<String> permissions = new LinkedHashSet<>();
        systemRoles().stream()
                .filter(role -> roleCodes.contains(role.code()))
                .forEach(role -> permissions.addAll(role.permissions()));
        return permissions;
    }

    private static boolean isSystemRoleAssignable(String code) {
        return !"PATIENT".equals(code) && !platformRoleCodes().contains(code);
    }

    private static PermissionDefinition permission(String code, String domain, String name, String description) {
        return new PermissionDefinition(code, domain, name, description);
    }

    private static Set<String> set(String... values) {
        return new LinkedHashSet<>(List.of(values));
    }

    private static Set<String> without(Set<String> source, String value) {
        LinkedHashSet<String> result = new LinkedHashSet<>(source);
        result.remove(value);
        return result;
    }

    private static String roleName(String code) {
        return switch (code) {
            case ROLE_ADMIN_JOPRELYS -> "Administrateur Joprelys";
            case ROLE_SUPER_ADMIN -> "Super administrateur";
            case ROLE_ADMIN_CLINIQUE -> "Administrateur clinique";
            case "DAF" -> "Direction administrative et financière";
            case "SECRETAIRE_COMPTABLE" -> "Secrétaire comptable";
            case "CAISSIER" -> "Caissier";
            case "AGENT_ACCUEIL" -> "Agent d'accueil";
            case "MEDECIN" -> "Médecin";
            case "INFIRMIER" -> "Infirmier";
            case "BIOLOGISTE" -> "Biologiste";
            case "PHARMACIEN" -> "Pharmacien";
            case "GESTIONNAIRE_STOCK" -> "Gestionnaire de stock";
            case "RESPONSABLE_HOSPITALISATION" -> "Responsable hospitalisation";
            case "AUDITEUR" -> "Auditeur";
            case "PATIENT" -> "Patient";
            default -> code;
        };
    }

    public record PermissionDefinition(String code, String domain, String name, String description) {
    }

    public record RoleDefinition(
            UUID id,
            String code,
            String name,
            String description,
            boolean assignable,
            Set<String> permissions) {
    }
}
