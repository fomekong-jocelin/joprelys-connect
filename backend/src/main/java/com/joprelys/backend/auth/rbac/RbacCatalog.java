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
    public static final String ROLE_AGENT_HYGIENE = "AGENT_HYGIENE";
    public static final String ROLE_TECHNICIEN_MAINTENANCE = "TECHNICIEN_MAINTENANCE";

    public static final String PERMISSION_USER_READ = "USER_READ";
    public static final String PERMISSION_USER_MANAGE = "USER_MANAGE";
    public static final String PERMISSION_RBAC_READ = "RBAC_READ";
    public static final String PERMISSION_RBAC_MANAGE = "RBAC_MANAGE";
    public static final String PERMISSION_ORGANIZATION_MANAGE = "ORGANIZATION_MANAGE";
    public static final String PERMISSION_AUTH_SESSION_MANAGE = "AUTH_SESSION_MANAGE";
    public static final String PERMISSION_EMERGENCY_MEDICO_LEGAL_READ = "EMERGENCY_MEDICO_LEGAL_READ";
    public static final String PERMISSION_EMERGENCY_MEDICO_LEGAL_WRITE = "EMERGENCY_MEDICO_LEGAL_WRITE";
    public static final String PERMISSION_EMERGENCY_BELONGINGS_WRITE = "EMERGENCY_BELONGINGS_WRITE";
    public static final String PERMISSION_APPOINTMENT_READ = "APPOINTMENT_READ";
    public static final String PERMISSION_APPOINTMENT_READ_OWN = "APPOINTMENT_READ_OWN";
    public static final String PERMISSION_APPOINTMENT_WRITE = "APPOINTMENT_WRITE";
    public static final String PERMISSION_AVAILABILITY_MANAGE = "AVAILABILITY_MANAGE";
    public static final String PERMISSION_AVAILABILITY_MANAGE_ALL = "AVAILABILITY_MANAGE_ALL";
    public static final String PERMISSION_PATIENT_PORTAL_ACCESS = "PATIENT_PORTAL_ACCESS";
    public static final String PERMISSION_PATIENT_APPOINTMENT_MANAGE = "PATIENT_APPOINTMENT_MANAGE";
    public static final String PERMISSION_PATIENT_NOTIFICATION_MANAGE = "PATIENT_NOTIFICATION_MANAGE";
    public static final String PERMISSION_HOSPITALIZATION_ADMIT = "HOSPITALIZATION_ADMIT";
    public static final String PERMISSION_HOSPITALIZATION_NOTE_WRITE = "HOSPITALIZATION_NOTE_WRITE";
    public static final String PERMISSION_HOSPITALIZATION_CONSENT_MANAGE = "HOSPITALIZATION_CONSENT_MANAGE";
    public static final String PERMISSION_HOSPITALIZATION_CARE_WRITE = "HOSPITALIZATION_CARE_WRITE";
    public static final String PERMISSION_HOSPITALIZATION_MEDICATION_ADMINISTER =
            "HOSPITALIZATION_MEDICATION_ADMINISTER";
    public static final String PERMISSION_HOSPITALIZATION_CONSUMABLE_WRITE =
            "HOSPITALIZATION_CONSUMABLE_WRITE";
    public static final String PERMISSION_BED_OPERATIONAL_STATUS_MANAGE = "BED_OPERATIONAL_STATUS_MANAGE";
    public static final String PERMISSION_HOSPITALIZATION_TRANSFER = "HOSPITALIZATION_TRANSFER";
    public static final String PERMISSION_HOSPITALIZATION_DISCHARGE_DECIDE = "HOSPITALIZATION_DISCHARGE_DECIDE";
    public static final String PERMISSION_HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM =
            "HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM";
    public static final String PERMISSION_BED_CLEANING_MANAGE = "BED_CLEANING_MANAGE";
    public static final String PERMISSION_BED_MAINTENANCE_MANAGE = "BED_MAINTENANCE_MANAGE";

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
                permission(PERMISSION_ORGANIZATION_MANAGE, "ADMINISTRATION", "Gérer les établissements", "Créer et administrer les établissements."),
                permission("WEBHOOK_MANAGE", "ADMINISTRATION", "Gérer les webhooks", "Créer, consulter, modifier et supprimer les webhooks de l'établissement."),
                permission("STAFF_PROFILE_ACCESS", "ADMINISTRATION", "Gérer son profil collaborateur", "Consulter et modifier uniquement son propre profil collaborateur."),
                permission("FILE_UPLOAD", "ADMINISTRATION", "Téléverser des fichiers autorisés", "Téléverser une photo, un logo, une signature ou un cachet selon les règles métier."),
                permission("PATIENT_READ", "PATIENT", "Consulter les patients", "Consulter les informations administratives des patients."),
                permission("PATIENT_WRITE", "PATIENT", "Gérer les patients", "Créer et modifier les informations administratives des patients."),
                permission("PATIENT_MERGE", "PATIENT", "Fusionner les dossiers patients", "Analyser, ignorer et fusionner les doublons patients."),
                permission("PATIENT_EMERGENCY_ACCESS", "PATIENT", "Déclencher un accès d'urgence", "Ouvrir un accès exceptionnel et traçable au dossier patient."),
                permission("CLINICAL_READ", "CLINIQUE", "Consulter le dossier clinique", "Consulter les données cliniques autorisées."),
                permission("CLINICAL_WRITE", "CLINIQUE", "Renseigner le dossier clinique", "Créer et modifier les données cliniques autorisées."),
                permission("VISIT_READ", "CLINIQUE", "Consulter les visites", "Consulter les visites, leurs QR codes et leurs constantes."),
                permission("VISIT_CREATE", "CLINIQUE", "Créer les visites", "Ouvrir une visite pour un patient."),
                permission("VISIT_VITALS_WRITE", "CLINIQUE", "Saisir les constantes", "Enregistrer les constantes vitales d'une visite."),
                permission("VISIT_MANAGE", "CLINIQUE", "Gérer le cycle des visites", "Corriger, clôturer ou annuler une visite."),
                permission("DOCUMENT_READ", "CLINIQUE", "Consulter les documents médicaux", "Télécharger les documents médicaux autorisés."),
                permission("DOCUMENT_MANAGE", "CLINIQUE", "Gérer les documents médicaux", "Révoquer ou annuler un document médical."),
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
                permission("HOSPITALIZATION_MANAGE", "HOSPITALISATION", "Gérer les hospitalisations (historique)", "Permission historique conservée pour migration des rôles personnalisés ; aucun nouvel endpoint sensible ne doit l'utiliser."),
                permission(PERMISSION_HOSPITALIZATION_ADMIT, "HOSPITALISATION", "Admettre un patient", "Créer administrativement un séjour après décision d'hospitalisation et affecter un lit disponible."),
                permission(PERMISSION_HOSPITALIZATION_NOTE_WRITE, "HOSPITALISATION", "Renseigner les notes de séjour", "Ajouter une note clinique ou soignante au séjour actif selon le périmètre actuellement partagé."),
                permission(PERMISSION_HOSPITALIZATION_CONSENT_MANAGE, "HOSPITALISATION", "Gérer les consentements", "Enregistrer un consentement clinique et sa preuve documentaire lorsque le professionnel est habilité."),
                permission(PERMISSION_HOSPITALIZATION_CARE_WRITE, "HOSPITALISATION", "Renseigner les soins hospitaliers", "Tracer les soins journaliers réalisés pendant un séjour actif."),
                permission(PERMISSION_HOSPITALIZATION_MEDICATION_ADMINISTER, "HOSPITALISATION", "Tracer l'administration médicamenteuse", "Tracer l'administration effective d'un médicament prescrit ; ce droit n'autorise pas la prescription."),
                permission(PERMISSION_HOSPITALIZATION_CONSUMABLE_WRITE, "HOSPITALISATION", "Tracer les consommables patient", "Rattacher au séjour les consommables effectivement utilisés pour les soins."),
                permission(PERMISSION_HOSPITALIZATION_TRANSFER, "HOSPITALISATION", "Transférer un patient hospitalisé", "Changer le lit, la chambre ou le service d'un séjour actif."),
                permission(PERMISSION_HOSPITALIZATION_DISCHARGE_DECIDE, "HOSPITALISATION", "Décider la sortie médicale", "Valider le diagnostic, les consignes et la décision médicale de sortie sans libérer le lit."),
                permission(PERMISSION_HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM, "HOSPITALISATION", "Confirmer le départ physique", "Confirmer que le patient a réellement quitté l'unité, clôturer son affectation et déclencher la remise en état du lit."),
                permission(PERMISSION_BED_OPERATIONAL_STATUS_MANAGE, "HOSPITALISATION", "Superviser la capacité des lits", "Ouvrir ou fermer la capacité d'un lit et superviser exceptionnellement son état opérationnel."),
                permission(PERMISSION_BED_CLEANING_MANAGE, "HOSPITALISATION", "Gérer le nettoyage des lits", "Placer un lit non affecté en nettoyage et confirmer sa remise à disposition après nettoyage."),
                permission(PERMISSION_BED_MAINTENANCE_MANAGE, "HOSPITALISATION", "Gérer la maintenance des lits", "Placer un lit non affecté en maintenance et confirmer sa remise en service technique."),
                permission("SPATIAL_CONFIGURATION_MANAGE", "HOSPITALISATION", "Configurer les espaces de soins", "Configurer bâtiments, services, chambres et lits."),
                permission("RECEPTION_READ", "ACCUEIL", "Consulter le registre d'accueil", "Consulter les entrées et départs du registre d'accueil."),
                permission("RECEPTION_WRITE", "ACCUEIL", "Gérer le registre d'accueil", "Créer une entrée et enregistrer un départ."),
                permission("BILLING_INVOICE_READ", "FACTURATION", "Consulter les factures", "Consulter les factures et leurs soldes."),
                permission("BILLING_INVOICE_WRITE", "FACTURATION", "Créer et valider les factures", "Créer, modifier et valider les factures."),
                permission("BILLING_INVOICE_CANCEL", "FACTURATION", "Annuler les factures", "Annuler une facture et gérer les actions exceptionnelles."),
                permission("RECEIVABLE_REMINDER_READ", "FACTURATION", "Consulter les relances", "Consulter l'historique des relances de créances."),
                permission("RECEIVABLE_REMINDER_WRITE", "FACTURATION", "Consigner les relances", "Ajouter une action de relance sur une créance."),
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
                permission("AUDIT_READ", "AUDIT", "Consulter les journaux", "Consulter les journaux d'audit autorisés."),
                permission("AUDIT_CROSS_TENANT_READ", "AUDIT", "Auditer plusieurs établissements", "Consulter les journaux d'audit au-delà de son établissement."),
                permission(PERMISSION_APPOINTMENT_READ, "RENDEZ_VOUS", "Consulter les rendez-vous", "Consulter l'agenda et les rendez-vous de l'établissement."),
                permission(PERMISSION_APPOINTMENT_READ_OWN, "RENDEZ_VOUS", "Consulter son agenda médecin", "Consulter uniquement les rendez-vous affectés au médecin connecté."),
                permission(PERMISSION_APPOINTMENT_WRITE, "RENDEZ_VOUS", "Gérer les rendez-vous", "Réserver pour un patient, enregistrer les arrivées et gérer le cycle de vie des rendez-vous."),
                permission(PERMISSION_AVAILABILITY_MANAGE, "RENDEZ_VOUS", "Gérer les disponibilités médecins", "Définir les plages de disponibilité récurrentes et les indisponibilités des médecins."),
                permission(PERMISSION_AVAILABILITY_MANAGE_ALL, "RENDEZ_VOUS", "Gérer les disponibilités de tous les médecins", "Administrer les disponibilités de tous les médecins de l'établissement."),
                permission(PERMISSION_PATIENT_PORTAL_ACCESS, "PORTAIL_PATIENT", "Accéder au portail patient", "Consulter et gérer uniquement ses propres données patient."),
                permission(PERMISSION_PATIENT_APPOINTMENT_MANAGE, "PORTAIL_PATIENT", "Gérer ses rendez-vous", "Consulter, réserver et annuler uniquement ses propres rendez-vous."),
                permission(PERMISSION_PATIENT_NOTIFICATION_MANAGE, "PORTAIL_PATIENT", "Gérer ses notifications", "Consulter et supprimer uniquement ses propres notifications."));
    }

    public static List<RoleDefinition> systemRoles() {
        Map<String, Set<String>> mappings = new LinkedHashMap<>();
        Set<String> all = permissionCodes();

        mappings.put(ROLE_ADMIN_JOPRELYS, set(
                PERMISSION_ORGANIZATION_MANAGE, "WEBHOOK_MANAGE", "SPATIAL_CONFIGURATION_MANAGE",
                "USER_READ", "RBAC_READ", "RBAC_MANAGE"));
        mappings.put(ROLE_SUPER_ADMIN, set(
                PERMISSION_ORGANIZATION_MANAGE, "SPATIAL_CONFIGURATION_MANAGE",
                "USER_READ", "RBAC_READ", "RBAC_MANAGE"));
        mappings.put(ROLE_ADMIN_CLINIQUE, without(
                all,
                PERMISSION_ORGANIZATION_MANAGE,
                "AUDIT_CROSS_TENANT_READ",
                PERMISSION_APPOINTMENT_READ_OWN,
                PERMISSION_PATIENT_PORTAL_ACCESS,
                PERMISSION_PATIENT_APPOINTMENT_MANAGE,
                PERMISSION_PATIENT_NOTIFICATION_MANAGE));
        mappings.put("DAF", set(
                "USER_READ", "RBAC_READ", "BILLING_INVOICE_READ", "CASH_HISTORY_READ", "CASH_DISCREPANCY_RESOLVE",
                "INSURANCE_BORDEREAU_READ", "INSURANCE_BORDEREAU_PROGRESS", "INSURANCE_BORDEREAU_SETTLE",
                "ACCOUNTING_DASHBOARD_READ", "ACCOUNTING_EXPORT", "AUDIT_READ",
                "RECEIVABLE_REMINDER_READ", "RECEIVABLE_REMINDER_WRITE"));
        mappings.put("SECRETAIRE_COMPTABLE", set(
                "BILLING_INVOICE_READ", "BILLING_INVOICE_WRITE", "INSURANCE_BORDEREAU_READ",
                "INSURANCE_BORDEREAU_PROGRESS", "ACCOUNTING_DASHBOARD_READ",
                "RECEIVABLE_REMINDER_READ", "RECEIVABLE_REMINDER_WRITE"));
        mappings.put("CAISSIER", set(
                "BILLING_INVOICE_READ", "CASH_QUEUE_READ", "CASH_PAYMENT_COLLECT", "CASH_SESSION_OPEN",
                "CASH_SESSION_CLOSE", "CASH_MOVEMENT_WRITE", "CASH_HISTORY_READ"));
        mappings.put("AGENT_ACCUEIL", set(
                "PATIENT_READ", "PATIENT_WRITE", "EMERGENCY_READ",
                "BILLING_INVOICE_READ", "BILLING_INVOICE_WRITE",
                "VISIT_READ", "VISIT_CREATE", "VISIT_VITALS_WRITE", "DOCUMENT_READ",
                "RECEPTION_READ", "RECEPTION_WRITE",
                PERMISSION_APPOINTMENT_READ, PERMISSION_APPOINTMENT_WRITE));
        mappings.put("MEDECIN", set(
                "USER_READ", "PATIENT_READ", "PATIENT_EMERGENCY_ACCESS", "CLINICAL_READ", "CLINICAL_WRITE",
                "EMERGENCY_READ", "EMERGENCY_WRITE", "EMERGENCY_STABILIZE",
                PERMISSION_EMERGENCY_MEDICO_LEGAL_READ,
                PERMISSION_EMERGENCY_MEDICO_LEGAL_WRITE,
                PERMISSION_EMERGENCY_BELONGINGS_WRITE,
                "LAB_ORDER_READ", "LAB_ORDER_CREATE", "HOSPITALIZATION_READ",
                PERMISSION_HOSPITALIZATION_ADMIT,
                PERMISSION_HOSPITALIZATION_NOTE_WRITE,
                PERMISSION_HOSPITALIZATION_CONSENT_MANAGE,
                PERMISSION_HOSPITALIZATION_CARE_WRITE,
                PERMISSION_HOSPITALIZATION_TRANSFER,
                PERMISSION_HOSPITALIZATION_DISCHARGE_DECIDE,
                "VISIT_READ", "VISIT_CREATE", "VISIT_VITALS_WRITE", "VISIT_MANAGE",
                "DOCUMENT_READ", "DOCUMENT_MANAGE", "RECEPTION_READ",
                PERMISSION_APPOINTMENT_READ, PERMISSION_APPOINTMENT_READ_OWN, PERMISSION_AVAILABILITY_MANAGE));
        mappings.put("INFIRMIER", set(
                "PATIENT_READ", "PATIENT_WRITE", "PATIENT_EMERGENCY_ACCESS", "CLINICAL_READ", "CLINICAL_WRITE",
                "EMERGENCY_READ", "EMERGENCY_WRITE",
                PERMISSION_EMERGENCY_MEDICO_LEGAL_READ,
                PERMISSION_EMERGENCY_BELONGINGS_WRITE,
                "LAB_ORDER_READ", "HOSPITALIZATION_READ",
                PERMISSION_HOSPITALIZATION_NOTE_WRITE,
                PERMISSION_HOSPITALIZATION_CARE_WRITE,
                PERMISSION_HOSPITALIZATION_MEDICATION_ADMINISTER,
                PERMISSION_HOSPITALIZATION_CONSUMABLE_WRITE,
                PERMISSION_HOSPITALIZATION_TRANSFER,
                "VISIT_READ", "VISIT_CREATE", "VISIT_VITALS_WRITE", "DOCUMENT_READ", "RECEPTION_READ"));
        mappings.put("BIOLOGISTE", set("PATIENT_READ", "LAB_ORDER_READ", "LAB_QUEUE_READ", "LAB_ORDER_WRITE"));
        mappings.put("PHARMACIEN", set(
                "PHARMACY_PRESCRIPTION_READ", "PHARMACY_STOCK_MANAGE", "STOCK_READ", "STOCK_MANAGE",
                "DOCUMENT_READ"));
        mappings.put("GESTIONNAIRE_STOCK", set("STOCK_READ", "STOCK_MANAGE"));
        mappings.put("RESPONSABLE_HOSPITALISATION", set(
                "PATIENT_READ", "HOSPITALIZATION_READ",
                PERMISSION_HOSPITALIZATION_ADMIT,
                PERMISSION_HOSPITALIZATION_TRANSFER,
                PERMISSION_HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM,
                PERMISSION_BED_OPERATIONAL_STATUS_MANAGE,
                PERMISSION_BED_CLEANING_MANAGE,
                PERMISSION_BED_MAINTENANCE_MANAGE));
        mappings.put(ROLE_AGENT_HYGIENE, set(
                "HOSPITALIZATION_READ", PERMISSION_BED_CLEANING_MANAGE));
        mappings.put(ROLE_TECHNICIEN_MAINTENANCE, set(
                "HOSPITALIZATION_READ", PERMISSION_BED_MAINTENANCE_MANAGE));
        mappings.put("AUDITEUR", set(
                "AUDIT_READ", "BILLING_INVOICE_READ", "CASH_HISTORY_READ", "ACCOUNTING_DASHBOARD_READ",
                "AUDIT_CROSS_TENANT_READ", PERMISSION_EMERGENCY_MEDICO_LEGAL_READ));
        mappings.put("PATIENT", set(
                PERMISSION_PATIENT_PORTAL_ACCESS,
                PERMISSION_PATIENT_APPOINTMENT_MANAGE,
                PERMISSION_PATIENT_NOTIFICATION_MANAGE));

        mappings.forEach((roleCode, permissionCodes) -> {
            if (!"PATIENT".equals(roleCode)) {
                permissionCodes.add("STAFF_PROFILE_ACCESS");
                permissionCodes.add("FILE_UPLOAD");
            }
        });

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

    private static Set<String> without(Set<String> source, String... values) {
        LinkedHashSet<String> result = new LinkedHashSet<>(source);
        result.removeAll(Set.of(values));
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
            case ROLE_AGENT_HYGIENE -> "Agent d'hygiène";
            case ROLE_TECHNICIEN_MAINTENANCE -> "Technicien de maintenance";
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
