# Changelog

Tous les changements notables du projet doivent être documentés ici.

Le format suit l'esprit de Keep a Changelog et le versioning suit Semantic Versioning.

## [Unreleased]

### Fixed

- **Résolution des échecs de tests d'intégration H2 (Corrections de contraintes d'unicité et isolation de la base de test)** :
  - **Migration V39 (`V39__fix_medical_documents_h2_unique_constraint.java`)** : Écriture d'une migration Java Flyway dynamique pour supprimer la contrainte d'unicité sur `medical_documents(visit_id)` (introduite par erreur dans la création de table initiale) sous H2 de manière robuste : suppression de la clé étrangère dépendante, reformatage de la colonne via `ALTER COLUMN` pour purger l'attribut d'unicité de H2, suppression de l'index unique et recréation de la clé étrangère.
  - **Migration V40 (`V40__fix_lab_results_result_number_uniqueness.java`)** : Écriture d'une migration Java Flyway dynamique et insensible à la casse (`UPPER(...)`) pour supprimer la contrainte/index d'unicité sur `lab_results(result_number)` sous Postgres et H2 (permettant le multi-analyte et le versioning de résultats) et création d'une contrainte d'unicité composite sur `(result_number, version, analyte_name)`.
  - **Isolation et robustesse de la base de test** :
    - Remplacement du nom de la base H2 en mémoire dans `application-test.yml` de `testdb` à `testdb_fresh` pour éviter tout conflit de cache ou d'historique de migrations corrompues lors des lancements successifs dans le même JVM.
    - Ajout de l'annotation `@ActiveProfiles("test")` sur `JoprelysBackendApplicationTests` pour éviter qu'il n'essaie de s'exécuter sur le profil par défaut (et donc d'interroger la base Postgres locale).
    - Nettoyage des instructions de débogage temporaires de base de données dans `LabIntegrationControllerTest.java`.
  - Passage de l'ensemble des 240 tests du backend au vert (`BUILD SUCCESS`).

### Added

- **Alignement Module 12 — Portails patient, pro, labo, pharmacie et vérification publique conformes CDC (STORY-1910)** :
  - Externalisation complète de l'i18n Angular vers `web/src/assets/i18n/fr.json` et `en.json` (~730 clés) avec chargement via `HttpClient`, `APP_INITIALIZER` et fallback FR.
  - Création de `AppTitleService` et mise à jour dynamique du titre de page (`<title>`) via les données de route ; mise à jour de `index.html` et `app.ts`.
  - Refactor de `app-shell.component.ts` (547 → 359 lignes) par extraction de la navigation dans `app-shell-nav.component.ts` pour respecter la limite de 500 lignes.
  - Création des pages patient manquantes : `/patient/profile`, `/patient/documents`, `/patient/qr-code`, `/patient/privacy` avec i18n FR/EN, design system Tailwind v4 et gestion des états vide/erreur.
  - Mise à jour des routes dans `app.routes.ts` avec les clés de titre et intégration du menu patient dans `app-shell-nav.component.ts`.
  - Déplacement du helper de test i18n vers `src/testing/i18n-testing.ts` et exclusion de `src/testing/**/*.ts` dans `tsconfig.app.json` pour isoler le code Node.js du build applicatif.
  - Tests unitaires Vitest pour les nouvelles pages patient (`patient-pages.spec.ts`, 7 tests) et correction des tests existants (`patient-portal.spec.ts`, `pharmacy-portal.spec.ts`).
  - Compilation Angular (`npm run build`) et 79 tests unitaires (`npm run test`) au vert.
  - Documentation : `docs/features/STORY-1910/FUNCTIONAL-SPEC.md`, `TECHNICAL-DESIGN.md`.

- **Audit et correction des traductions manquantes du portail patient (TICKET-I18N-PATIENT-PORTAL-TRANSLATIONS-AUDIT)** :
  - Remplacement des textes codés en dur dans le portail patient (dashboard, login, synthèse médicale, résultats, demandes d'accès, consentements, audit, notifications, profil, vaccinations) par des clés i18n.
  - Traduction des statuts/enum affichés bruts (`patient.access.requests.status.*`, `patient.audit.status.*`, `patient.consent.status.*`, `patient.consent.type.*`, `patient.consent.channel.*`).
  - Ajout d'environ 136 clés dans `web/src/assets/i18n/fr.json` et `en.json`.
  - Mise à jour du test `patient-portal.spec.ts` rendu obsolète par la traduction du statut d'audit (`SUCCESS` -> `Succès`).
  - Compilation Angular (`npm run build`) et 79 tests unitaires (`npm run test`) au vert.
  - Ticket : `docs/ai/tickets/TICKET-I18N-PATIENT-PORTAL-TRANSLATIONS-AUDIT.md`.

- **Alignement Module 11 — Documents médicaux vérifiables conformes CDC (STORY-1908)** :
  - Migration DB `V37__medical_documents_cdc_alignment.sql` : ajout des colonnes `qr_code_url`, `verification_url`, `author_user_id`, `version`, `previous_document_id` à `medical_documents` ; création de la séquence `medical_document_number_seq` ; migration des types legacy `SYNTHESE` → `COMPTE_RENDU_CONSULTATION` ; ajout de la contrainte CHECK sur `status`.
  - `DocumentType.java` (nouveau) : enum des 12 types CDC (COMPTE_RENDU_CONSULTATION, ORDONNANCE, RESULTAT_LABORATOIRE, FICHE_SORTIE, etc.).
  - `DocumentStatus.java` (nouveau) : enum VALID, REVOQUE, ANNULE, REMPLACE.
  - `MedicalDocumentEntity` : mappage des enums, versionnement (previousDocumentId, version), auteur, URLs, QR code et hash.
  - `MedicalDocumentRepository` : ajout de `findAllByVisitIdAndDocumentTypeOrderByVersionDesc()`.
  - `DocumentNumberGenerator` : séquence DB garantissant l'unicité en cluster.
  - `DocumentService` : calcul SHA-256 du PDF, versionnement automatique, exposition QR code.
  - `DocumentVerificationResponse` : ajout des champs `documentType`, `serviceName`, `legalNotice`.
  - `DocumentController` : endpoint public `GET /api/public/documents/search?number=` pour recherche par numéro de document, endpoint `GET /api/public/documents/{id}/qr` pour image QR.
  - `HospitalizationService` : intégration du versionnement et de la génération du document `FICHE_SORTIE` dans `medical_documents` avec hash et URLs corrects.
  - `LabResultService` : injection de `DocumentNumberGenerator` et `MedicalDocumentRepository` ; enregistrement automatique du PDF labo comme `RESULTAT_LABORATOIRE` dans `medical_documents` avec hash SHA-256, versionnement, auteur et URLs.
  - `ConsultationResponse` et `PatientPortalController` : correction de la conversion `DocumentStatus` enum → String (appel `.name()`).
  - Frontend Angular :
    - `document-search.component.ts` (nouveau) : page publique `/verify` avec formulaire de recherche par numéro de document + i18n FR/EN.
    - `verification.component.ts` : affichage des nouveaux champs `documentType`, `serviceName`, `legalNotice` (mention légale CDC) ; correction des radius (rounded-3xl/2xl → rounded) ; gestion du statut `REMPLACE`.
    - `consultation-api.service.ts` : ajout de `verifyDocumentByNumber()`.
    - `app.routes.ts` : ajout de la route `/verify` (sans ID) pour le formulaire de recherche.
    - `i18n.service.ts` : ajout des clés FR/EN `verify.documentType`, `verify.serviceName`, `verify.legalNotice`, `verify.search.*`.
  - Compilation backend Maven : BUILD SUCCESS (232 fichiers compilés, 0 erreur).

- **Alignement Module 10 — Hospitalisations conformes CDC (STORY-1907)** :

  - Migration DB `V36__hospitalizations_cdc_alignment.sql` : ajout de la séquence `hospitalization_number_seq` et des colonnes `hospitalization_number`, `visit_id`, `responsible_practitioner_id` et `document_id` à la table `hospitalizations`. Ajout de la colonne `hash` à la table `medical_documents`.
  - `HospitalizationEntity` : ajout du support de versioning, du numéro unique d'hospitalisation, du lien visite, du médecin responsable et de la fiche de sortie.
  - `HospitalizationService` :
    - `admitPatient` : génération d'un numéro d'hospitalisation unique (`HOSP-YYYYMMDD-XXXXXX`), lien avec la visite et le médecin responsable, et validation d'unicité de lit (un lit ne peut être occupé que par plus d'un séjour actif 'EN_COURS').
    - `dischargePatient` : génération de la fiche de sortie officielle avec calcul du hash SHA-256 du PDF de sortie, enregistrement dans `medical_documents` sous le type `FICHE_SORTIE` et liaison de l'ID du document généré.
  - `PatientSummaryService` : intégration de l'historique complet des hospitalisations du patient dans la synthèse médicale.
  - `VisitController` & `VisitService` : ajout de l'endpoint `GET /api/visits/patient/{patientId}` pour récupérer toutes les visites d'un patient.
  - `HospitalizationControllerTest` : mise à jour et validation avec création de visites de test, vérification d'unicité concurrentielle, de notes d'évolution et téléchargement du PDF de sortie officiel.
  - Frontend Angular :
    - `patient-hospitalization.component.ts` : refonte du formulaire d'admission pour y ajouter les menus déroulants de sélection de la visite associée et du médecin responsable (chargement dynamique). Affichage du numéro de séjour et du médecin responsable dans le séjour en cours et les anciens séjours.
    - `patient-api.service.ts` : ajout de la méthode `getPatientVisits(patientId)` pour requêter l'API.
    - `i18n.service.ts` : ajout des clés d'internationalisation FR/EN pour les nouveaux concepts d'hospitalisation.
    - Compilation Angular build de production et 72 tests unitaires Vitest validés avec succès.

- **Alignement Module 9 — Résultats d'examens conformes CDC (STORY-1906)** :
  - Migration DB `V35__lab_results_cdc_alignment.sql` : ajout de la séquence `lab_result_number_seq` et des colonnes `status` (DRAFT, VALIDATED, CANCELLED), `validator_user_id`, `conclusion`, `document_id`, `version` et `parent_result_id`.
  - `LabResultEntity` : mappage des attributs status (enum `LabResultStatus`), validatorUserId, conclusion, documentId, version et parentResultId.
  - `LabResultService` : implémentation de l'immutabilité et du versioning (un résultat VALIDATED génère une nouvelle version en préservant le même numéro de résultat). Ajout de la recherche de validateur par nom pour intégration API et filtrage automatique des anciennes versions dans `getPatientResults`.
  - `PatientPortalController` : ajout de l'endpoint `GET /api/patient/results` (récupération des analyses biologiques de l'utilisateur connecté sans paramètre d'URL pour prévenir toute vulnérabilité IDOR) et `GET /api/patient/results/{resultId}/pdf` pour télécharger le compte-rendu biologique en PDF.
  - `LabOrderController` : ajout de l'endpoint `GET /api/lab-orders/results/{resultId}/pdf` pour les professionnels de santé.
  - `PatientExamResultsController` : endpoint d'exportation structurée CSV/JSON (`GET /api/patients/{id}/exam-results/export?format=csv|json`).
  - FHIR diagnostic report et observation endpoints : intégration du parser et mapping FHIR pour les DiagnosticReport et Observations biologiques.
  - Tests unitaires et d'intégration : ajout de tests dans `LabIntegrationControllerTest.java` (immutabilité, versioning, export) et correction d'ambiguïté dans `FhirMappersTest.java`.
  - Frontend Angular :
    - Page de résultats patient **"Mes Analyses & Résultats"** (`/patient/results`) affichant l'historique des examens, les badges d'interprétation couleur, les conclusions cliniques, l'exportation CSV/JSON et le téléchargement PDF.
    - Ajout de la carte d'accès rapide sur le Tableau de bord Patient.
    - Ajout du lien de navigation dans la Sidebar de l'App Shell.
    - Support multilingue FR/EN complet dans `i18n.service.ts`.

- **Alignement Module 8 — Examens médicaux conformes CDC (STORY-1905)** :
  - Migration Flyway Java `V34__lab_orders_cdc_alignment.java` : ajout de la colonne `source_organization_id`, création de la table `lab_order_items` (migration des données CSV historiques de manière compatible H2/PostgreSQL).
  - `LabOrderItemEntity` : création pour modéliser les items d'examens individuels.
  - `LabOrderEntity` : refactorisation pour porter la relation `@OneToMany` avec les items et utiliser les nouveaux enums `ExamType` et `LabOrderStatus`.
  - `LabOrderService` : refactorisation d' `updateStatus()` pour sécuriser la modification en validant que seul le biologiste de l'organisation cible peut modifier le statut de la demande. Utilisation de `findByIdGlobally()` et d'un basculement de `TenantContext` pour prendre en compte le multi-tenancy.
  - `LabOrderController` : support de l'autorisation et de la transmission de l'identité du biologiste lors du changement de statut.
  - Tests unitaires et d'intégration backend : validation dans `LabOrderControllerTest.java` et `LabIntegrationControllerTest.java` (sécurité, cycle de vie du paiement, et intégrité de la base).
  - Frontend Angular :
    - `lab.models.ts` : typage fort avec les enums `ExamType` and `LabOrderStatus`.
    - `consultation.component.ts` : utilisation de l'enum `ExamType.LABORATOIRE` pour le formulaire de demande d'examen.
    - `lab-orders-page.component.ts` : intégration du workflow de paiement (statuts `AWAITING_PAYMENT` et `PAID`) et liaison avec les items d'examens.
    - `i18n.service.ts` : ajout des traductions françaises et anglaises pour les nouveaux statuts d'examens.
    - Tests unitaires frontend Vitest : mise à niveau de `lab-orders-page.spec.ts` pour utiliser les types et enums typés forts.

- **Alignement Module 4 — Dossier patient partagé et synthèse médicale conforme CDC (STORY-1901)** :
  - `PatientSummaryService` : service d'orchestration pour composer la synthèse médicale structurée (DPU, identité, allergies actives, antécédents importants/en cours, traitements actifs, 3 dernières visites, 3 derniers diagnostics, derniers résultats critiques) et génération du PDF.
  - Endpoints REST créés :
    - `GET /api/patients/{id}/medical-summary` (pour les praticiens, sécurisé par tenant/consentement/scopes via `validateAccess`).
    - `GET /api/patient/medical-summary` (pour le portail patient, récupérant le patient connecté sans paramètre d'URL pour éviter tout IDOR).
    - `GET /api/patient/summary-pdf` (pour le téléchargement sécurisé du PDF par le patient lui-même).
  - Modification de `PatientController./{id}/summary-pdf` pour déléguer à `PatientSummaryService` et éviter ainsi une dépendance cyclique avec `PatientService`.
  - Modification de `PatientService.validateAccess()` pour autoriser le rôle `PATIENT` à accéder à ses propres données (vérification d'ID) tout en bloquant l'accès à d'autres dossiers.
  - Modification de `PatientService.createPatient()` pour persister initialement les champs textes libres d'allergies/antécédents dans les tables structurées correspondantes.
  - Implémentation des méthodes de synchronisation dans `PatientMedicalInfoService` : toute modification structurelle d'une allergie ou d'un antécédent met à jour de façon synchrone le texte consolidé (`PatientEntity.allergies` et `PatientEntity.medicalHistory`) pour la rétro-compatibilité.
  - Refactorisation du PDF de synthèse médicale dans `PdfGeneratorService` pour présenter des tableaux structurés et soignés pour chaque type d'information médicale requise par le CDC (Allergies, Antécédents principaux, Traitements, Diagnostics, Visites, Résultats critiques).
  - Ajout de tests unitaires et d'intégration MockMvc complets dans `PatientPortalControllerTest.java` validant la récupération de la synthèse structurée.
  - Frontend Angular :
    - Page de synthèse médicale **"Ma synthèse médicale"** (`/patient/summary`) affichant l'ensemble de la synthèse sous forme de cartes d'informations soignées avec indicateurs de gravité et de criticité (animations légères pour résultats critiques et badges de sévérité).
    - Option de téléchargement du document PDF officiel en direct.
    - Ajout du lien dans la barre latérale (Sidebar) du portail patient.
    - Support complet multilingue (FR/EN) pour tous les libellés de la synthèse via `I18nService`.
    - Tests unitaires et d'intégration Vitest (`patient-portal.spec.ts`) validant la récupération des données et le téléchargement du PDF.

- **Alignement Module 6 — Allergies et antécédents conformes CDC (STORY-1903)** :
  - Migration Flyway `V32__allergies_history_soft_delete_important.sql` : ajout de la colonne `important` (BOOLEAN DEFAULT FALSE) sur `patient_medical_history` ; ajout de `deleted_at` (TIMESTAMP) et `deleted_by` (UUID) sur `patient_medical_history` et `patient_allergies` pour le soft delete.
  - `PatientMedicalHistoryEntity` mis à jour avec `important`, `deletedAt`, `deletedBy` et getters/setters associés.
  - `PatientAllergyEntity` mis à jour avec `deletedAt`, `deletedBy` et getters/setters.
  - Repositories mis à jour avec des méthodes interrogeant uniquement les enregistrements non supprimés (`findAllBy...AndDeletedAtIsNull`, `findByIdAnd...AndDeletedAtIsNull`).
  - `PatientMedicalInfoService` mis à jour pour filtrer par statut non supprimé, gérer le flag `important` dans l'ajout/modification, et implémenter `deleteAllergy()` et `deleteMedicalHistory()` avec journalisation de l'action (`DELETE_ALLERGY`, `DELETE_HISTORY`) dans la table d'audit.
  - `PatientMedicalInfoController` : exposition des endpoints `DELETE /api/patients/{patientId}/allergies/{allergyId}` et `DELETE /api/patients/{patientId}/medical-history/{historyId}`.
  - DTOs `CreatePatientMedicalHistoryRequest` et `PatientMedicalHistoryResponse` mis à jour avec le champ `important`.
  - Tests unitaires et d'intégration backend (`PatientMedicalInfoControllerTest.java`) : validation de l'ajout du flag d'importance, de la suppression logique et de l'invisibilité des données supprimées.
  - Modèles et interfaces frontend `patient.models.ts` mis à jour avec le flag `important` et les nouvelles catégories d'antécédents (`ALLERGIC` et `SOCIAL`).
  - `PatientApiService` mis à jour avec les méthodes d'appels delete `deleteAllergy` et `deleteMedicalHistory`.
  - Frontend Angular `patient-medical-info.component.ts` : modale d'ajout mise à jour avec les catégories `Allergique` et `Social / Habitudes` ainsi qu'une case à cocher pour marquer l'antécédent comme important ; liste d'antécédents mise à jour pour afficher un badge clignotant rouge "⚠️ Important" ; ajout d'une option de suppression logique (bouton corbeille) avec pop-up de confirmation.
  - Internationalisation `i18n.service.ts` mise à jour avec les traductions pour les nouvelles catégories en français et en anglais.
  - Tests unitaires frontend `patient-medical-info.component.spec.ts` créés de manière isolée sous Vitest : validation de l'initialisation, de la sauvegarde et du soft delete des allergies et antécédents.

- **Alignement Module 7 — Prescriptions et ordonnances conformes CDC (STORY-1904)** :
  - `PrescriptionItemEntity` : ajout des champs `form`, `route`, `frequency` et `substitutionAllowed` (boolean).
  - `PrescriptionEntity` : ajout des champs `issuedAt`, `visitId`, `documentId` (UUID) pour faire le lien avec `MedicalDocumentEntity`.
  - `MedicalDocumentEntity` : changement de la relation `@OneToOne` à `@ManyToOne` sur `visit` pour autoriser plusieurs documents par visite (synthèse + ordonnance PDF) et suppression de la contrainte unique en DB ; ajout du champ `documentType` (default `"SYNTHESE"`).
  - `MedicalDocumentRepository` : filtrage par type `"SYNTHESE"` par défaut pour préserver la rétrocompatibilité des requêtes de dossiers et éviter toute exception de type `NonUniqueResultException`.
  - `DocumentController` : ajout de l'endpoint `GET /api/documents/{id}/download` pour permettre de télécharger n'importe quel document médical (dont l'ordonnance PDF) à partir de son ID unique de document.
  - `DocumentService` : implémentation de `generatePrescriptionDocument` qui génère le PDF ordonnance structuré dédié avec QR code de vérification publique et code PIN d'accès sécurisé.
  - `PdfGeneratorService` : implémentation de la mise en page de l'ordonnance PDF officielle (en-tête clinique, info patient/prescripteur, tableau des médicaments détaillés avec substitution autorisée/interdite, PIN de vérification).
  - `PrescriptionService` : implémentation du cycle de vie complet (`DRAFT` par défaut à l'enregistrement, finalisation vers `ACTIVE` avec appel de génération PDF, et annulation avec révocation synchrone du document PDF associé).
  - Scheduler `@Scheduled` : job de nuit pour invalider automatiquement les ordonnances actives dont la date d'expiration est dépassée.
  - Endpoints REST : exposition de `POST /api/prescriptions/{id}/finalize` et `PATCH /api/prescriptions/{id}/cancel` sur `PrescriptionController`.
  - Tests d'intégration : ajout de scénarios dans `PrescriptionControllerTest.java` validant la finalisation (génération PDF ordonnance, passage à ACTIVE) et l'annulation.
  - Frontend Angular :
    - `ConsultationComponent` : enrichissement du formulaire de prescription avec les colonnes Forme, Voie, Fréquence, Substitution Aut. et Instructions (layout sur double-ligne très soigné et responsive) ; désactivation des champs lorsque l'ordonnance est active ou annulée.
    - Ajout de badges de statuts clairs et colorés pour l'ordonnance (`DRAFT`, `ACTIVE`, `CANCELLED`).
    - Intégration de la barre d'action du cycle de vie en bas de carte avec les boutons "Valider l'ordonnance", "Annuler l'ordonnance" et "Télécharger PDF".
    - Traduction multilingue (FR/EN) complète de l'ensemble des libellés de prescription et de cycle de vie dans `i18n.service.ts`.
    - Eager loading de la prescription existante lors de l'initialisation de la consultation pour s'assurer que l'utilisateur voit toujours l'ordonnance précédemment saisie.

- **Alignement Module 5 — Visites et consultations conformes CDC (STORY-1902)** :
  - Migration Flyway `V31__visits_consultations_cdc_alignment.sql` : ajout de `service_name`, `main_practitioner_id`, `arrival_at` sur la table `visits` ; ajout de `suspected_diagnosis`, `final_diagnosis`, `conclusion` sur la table `consultations` ; ajout de `pain_scale` sur la table `vitals` ; création de la table `visit_corrections` pour la traçabilité des corrections (FR-VISIT-005).
  - `VisitEntity` mis à jour avec les champs `service`, `mainPractitionerId`, `arrivalAt`.
  - `ConsultationEntity` mis à jour avec `suspectedDiagnosis`, `finalDiagnosis`, `conclusion`.
  - `VitalsEntity` mis à jour avec `painScale` (INTEGER, échelle 0-10).
  - `SaveVitalsRequest` : validation `@Min(0)` / `@Max(10)` sur `painScale`.
  - `VitalsResponse` : exposition du champ `painScale`.
  - `CreateVisitRequest` : support de `service`, `mainPractitionerId`, `arrivalAt`.
  - `VisitService.saveVitals()` : persistance de `painScale` ; `createVisit()` : persistance des nouveaux champs.
  - `VisitService.correctVisit()` : correction traçable de visite terminée via `VisitCorrectionEntity` (log avant modification).
  - `VisitController` : endpoint `POST /api/visits/{id}/correct` pour corriger une visite clôturée avec traçabilité.
  - Import `Authentication` ajouté dans `VisitController` (correctif compilation).
  - `SaveConsultationRequest` : support de `suspectedDiagnosis`, `finalDiagnosis`, `conclusion`.
  - Frontend Angular `dashboard.component.ts` : propriété `vitalsPain`, `isPainInvalid()`, initialisation depuis `visit.vitals.painScale`, inclusion dans `isAnyVitalInvalid()`, envoi de `painScale` dans le payload API.
  - Frontend Angular `dashboard.component.html` : section **Douleur (Échelle 0-10)** dans le formulaire des constantes vitales, avec badge code couleur contextuel (vert/jaune/orange/rouge) et légende textuelle.
  - Frontend Angular `consultation.component.ts` : affichage de la carte **Douleur** dans la grille des constantes vitales en lecture seule, avec code couleur contextuel selon l'intensité.
  - Rénovation de la page de saisie de consultation (`ConsultationComponent`) :
    - Extraction du template HTML vers [consultation.component.html](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/consultation.component.html) pour séparer la vue du contrôleur et réduire la taille de la classe TypeScript sous la barre des 300 lignes, conformément aux standards de design et de code SOLID.
    - Élargissement de la mise en page pour utiliser l'intégralité de la largeur d'écran (`app-container py-6 space-y-6` au lieu de `max-w-5xl mx-auto`).
    - Suppression des aplats et dégradés de couleurs trop contrastés au profit de cartes blanches/slate-900 sobres à bordures discrètes pour s'aligner sur la charte graphique et supporter proprement les thèmes clair/sombre.
    - Application de la politique stricte d'arrondis sobres (radius max 8px via `rounded-[6px]` et `rounded-[4px]`).
    - Remplacement de toutes les chaînes de caractères brutes en français par l'injection de `I18nService` avec des clés spécifiques sous l'espace de nom `consultation.*`.
    - Ajout des traductions françaises et anglaises complètes dans [i18n.service.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/core/i18n/i18n.service.ts).


  - Audit complet du backend et frontend par rapport aux exigences des modules 4 à 12 du Cahier des charges.
  - Création de l’Epic `EPIC-0014` et de 11 User Stories détaillées (`STORY-1901` à `STORY-1911`).
  - Documentation fonctionnelle et technique dans `docs/features/alignment-modules-4-12/FUNCTIONAL-SPEC.md` et `TECHNICAL-DESIGN.md`.
  - Mise à jour de `docs/ai/PROJECT-TRACKING.md` avec le nouveau sprint `SPRINT-0011` et le planning d’alignement.
  - Identification des écarts critiques : synthèse médicale, champs visites/consultations, allergies/antécédents, prescriptions, examens, résultats, hospitalisations, documents vérifiables, consentements et portails frontend.

- **Conformité du Module 2 - Gestion des utilisateurs et rôles (TICKET-0113)** :
  - Ajout de la colonne `last_login_at` à la table `users` (migration Flyway `V29`) pour stocker la date/heure de dernière connexion des professionnels.
  - Implémentation du support de rôles multiples séparés par des virgules dans le champ `role` existant (ex: `"MEDECIN,PHARMACIEN"`) afin de préserver la structure sans breaking change.
  - Décodage des rôles multiples dans `JwtAuthenticationFilter` en authorities Spring Security distinctes pour supporter le RBAC multi-rôles de Spring Boot.
  - Sécurisation forte par authentification double facteur OTP à 6 chiffres (OTP Staff) pour les rôles professionnels sensibles (`ADMIN_JOPRELYS`, `ADMIN_CLINIQUE`, `MEDECIN`, `BIOLOGISTE`, `PHARMACIEN`).
  - Ajout du endpoint `/api/auth/verify-otp` (autorisé en accès public dans `SecurityConfig`) pour la validation de l'OTP et la génération finale du jeton JWT.
  - Rénovation de la page de login Angular pour supporter le login double étape (Email/Mot de passe -> OTP à 6 chiffres) pour le personnel ayant des rôles sensibles.
  - Rénovation de l'IHM d'administration de l'équipe clinique pour permettre la sélection et modification multi-rôles via des cases à cocher (check-boxes) au lieu d'un simple dropdown.
  - Mise en conformité de l'ensemble de la suite de tests unitaires et d'intégration frontend (Vitest 63/63) et backend (Maven 231/231).

- **Complétion du Module 1 - Gestion des établissements (TICKET-0112)** :
  - Enrichissement de la table `organizations` (migration Flyway `V28`) et de l'entité `OrganizationEntity` avec les propriétés `type` (type d'acteur), `country` (pays), `responsible_name` (nom du responsable) et `api_enabled` (autorisation API).
  - Implémentation du système de clés API par établissement avec empreinte hachée sécurisée (SHA-256) en base de données.
  - Endpoints sécurisés pour la génération de clés randomisées (`jop_live_...`), listing sécurisé avec masquage, et révocation immédiate (statut `REVOKED` et journalisation `revoked_at`).
  - Filtre de sécurité Spring Boot (`ApiKeyAuthenticationFilter`) validant le header `X-API-KEY`, injectant le rôle `ROLE_API_CLIENT` dans le contexte de sécurité et paramétrant le `TenantContext` pour les requêtes multi-tenant.
  - Validation de l'accès API (vérification que l'organisation est active et possède `apiEnabled = true`).
  - Bypass du filtre de clé API pour les routes publiques `/api/public/**` (qui exécutent leur propre validation, par exemple pour le téléversement FHIR de résultats biologiques).
  - Rénovation des formulaires Angular de création, d'édition et du tiroir de détails pour prendre en compte les nouveaux attributs d'organisation.
  - Intégration dans le drawer Angular d'un gestionnaire complet de clés API (création avec affichage unique de la clé générée, copie dans le presse-papiers, affichage des clés actives/révoquées, et bouton de révocation).
  - Validation complète par tests unitaires et d'intégration MockMvc backend et Jest/Vitest frontend Angular.

- **Navigation et responsivité mobile (TICKET-0111)** :
  - Masquage de la Sidebar de bureau sur mobile (`hidden md:flex`) pour libérer l'espace.
  - Implémentation d'un tiroir de navigation mobile (Drawer) coulissant avec arrière-plan estompé (`backdrop-blur-xs`) et bouton Hamburger dans le header.
  - Déplacement des sélecteurs de langue/thème et du bouton de déconnexion de la Topbar vers la base du Drawer mobile pour désencombrer l'entête.
  - Factorisation réutilisable des liens et icônes du menu de navigation dans un `<ng-template>` pour éviter la duplication de code et faciliter la maintenance.
  - Ajustement des espacements et paddings des Breadcrumbs et du conteneur de contenu principal sur mobile.
  - Restauration de la conformité de la suite de tests unitaires (correction des mocks unitaires de `forgot-password.component.spec.ts` et `patient-detail.component.spec.ts`).
  - **Phase 2 : Optimisation fine des espaces et mise en page mobile** :
    - Suppression du double padding mobile dans le Shell en passant la section principale à `p-0` sur mobile (les pages contrôlent ainsi leur propre padding).
    - Réduction du padding horizontal des conteneurs `.app-container` et `.app-container-wide` sur mobile de `1.5rem` à `1rem`.
    - Réduction du padding interne des cartes `.ui-card` sur mobile de `p-6` à `p-4` pour éviter le vide latéral.
    - Passage des boutons d'actions principales du Dossier Patient en disposition verticale étirée (`flex-col items-stretch`) sur mobile pour supprimer la compression horizontale.
    - Réduction de la marge interne des panneaux médicaux de la fiche patient (`p-5` à `p-4 md:p-5`).
  - **Phase 3 : Résolution du wrap DPU et du bouton Actualiser sur mobile** :
    - Remplacement de l'affichage standard du DPU sur mobile par un format `text-xs font-mono whitespace-nowrap` pour empêcher les retours à la ligne indésirables.
    - Réorganisation de l'en-tête de la file d'attente sur le tableau de bord pour placer le bouton "Actualiser" sur la même ligne que le titre sur mobile, déchargeant ainsi la description en dessous.
  - **Phase 4 : Remplacement du bouton Actualiser par une icône et réduction des polices sur mobile** :
    - Remplacement du bouton texte "Actualiser" par un bouton icône d'actualisation (`svg` de rafraîchissement) sur mobile sur le tableau de bord, masqué sur grand écran.
    - Réduction de la taille de la police du message d'accueil de `text-3xl` à `text-xl md:text-3xl` sur mobile pour éviter les débordements de texte sur petit écran.
    - Réduction de la taille du titre de la file d'attente active de `text-xl` à `text-base sm:text-xl` sur mobile.

- **Module API Joprelys Connect — Sous-tâches 3, 4 & 5 (TICKET-0016)** :
  - `RateLimitingFilter` — filtre HTTP avec bucket token simplifié par IP et par utilisateur. Configurable via `application.yml` (`joprelys.rate-limiting.enabled`, `max-requests-per-window`, `window-seconds`). Désactivé en profil `test`.
  - `RateLimitingProperties` — `@ConfigurationProperties` pour la config rate limiting.
  - `WebhookEntity`, `WebhookRepository`, `WebhookService`, `WebhookController` — CRUD complet des webhooks (rôles ADMIN_CLINIQUE/ADMIN_JOPRELYS). Validation HTTPS obligatoire. Suppression logique (status INACTIVE). Déclenchement simulé (log) pour le pilote.
  - Migration Flyway `V27__webhooks_schema.sql` — table `webhooks` avec indexes.
  - Annotations OpenAPI (`@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`) sur 6 controllers principaux : `PatientController`, `VisitController`, `ConsultationController`, `PrescriptionController`, `LabOrderController`, `DocumentController`.
  - Tests unitaires `RateLimitingFilterTest` (5 cas : under limit, over limit, disabled, authenticated user, different IPs) et `WebhookServiceTest` (9 cas : create, invalid URL, list, update, update forbidden, delete, delete, trigger matching, trigger non-matching).

- **Module API Joprelys Connect — Sous-tâche 1 & 2 (TICKET-0016)** :
  - `TraceIdFilter` — génère un `X-Trace-Id` unique par requête HTTP, propagé dans les logs (MDC) et les headers de réponse.
  - `ApiErrorResponse` — DTO du format d'erreur CDC : `{ "error": { "code", "message", "trace_id" } }`.
  - `GlobalExceptionHandler` (`@RestControllerAdvice`) — gère `ResponseStatusException`, `MethodArgumentNotValidException`, `AccessDeniedException`, `NoSuchElementException`, et exceptions génériques avec `trace_id`.
  - `AuthExceptionHandler` conservé sans modification — pas de régression sur le format `ProblemDetail` existant.
  - Configuration SpringDoc/OpenAPI dans `application.yml` — info API, version, contact, Swagger UI accessible publiquement (`/swagger-ui`).
  - Tests unitaires `GlobalExceptionHandlerTest` (7 cas : ResponseStatusException, BadRequest, validation, access denied, not found, generic, missing trace_id).

- **Module Notifications — Complétion (TICKET-0015)** :
  - `NotificationController` dédié avec endpoints REST : `GET /api/notifications/unread-count` (badge), `GET /api/notifications` (pagination Pageable), `DELETE /api/notifications/{id}` (suppression avec vérification propriétaire).
  - Méthodes `getUnreadCount`, `getNotificationsPaginated`, `deleteNotification` ajoutées à `NotificationService`.
  - Badge de notifications non lues dans la sidebar du portail patient (Angular) — consommation de `/api/notifications/unread-count` avec affichage numérique rouge sur l'icône.
  - Déclencheurs de notifications manquants : approbation/rejet de demande d'accès externe → notification SECURITY au patient ; téléversement de résultats labo → notification INFO/EMERGENCY au patient selon criticité.
  - Tests unitaires `NotificationServiceTest` (8 cas : send, getUnreadCount, paginated, markAsRead, markAsRead forbidden, markAllAsRead, delete, delete forbidden).
  - i18n FR/EN complet pour le badge et les messages de notification.

- **Vaccinations (Module 4 / DPU compliance)** : Conception, migration de base de données (V26), entité `PatientVaccinationEntity`, repository, services backend et endpoints API sous `/api/patients/{patientId}/vaccinations`. Ajout de l'interface d'enregistrement et de visualisation des vaccinations dans le dossier médical frontend.
- **Groupe Sanguin & Email (Module 3 / DPU compliance)** : Ajout des colonnes `blood_group` et `email` à la table `patients` (migration V25), mapping dans l'entité Java, DTOs backend/frontend, intégration dans le formulaire de création de patient, la fiche de profil d'administration et l'export PDF de synthèse médicale.
- **Lien Doublons Sidebar (TICKET-1304)** : Intégration du lien `/clinic/duplicates` dans le menu de navigation rétractable pour le rôle ADMIN_CLINIQUE.

- **Conformité Module Patient EPIC-0013 — Doublons, PDF Synthèse, Scopes Granulaires** :
  - **Détection de doublons (TICKET-1302)** : Nouveau service `PatientSimilarityService` implémentant un algorithme de similarité phonétique/Levenshtein. Détection automatique au moment de `createPatient()`. Entité `PatientDuplicateCandidateEntity` et `PatientDuplicateCandidateRepository` pour stocker les paires suspectes. Migration Flyway `V24__patient_duplicates_schema.sql`.
  - **Fusion transactionnelle de dossiers (TICKET-1303)** : Méthode `mergePatients()` dans `PatientService` réassignant l'intégralité des données du patient secondaire (Visites, Consentements, Accès d'urgence, Accès externes, Allergies, Antécédents, Ordres labo, Résultats labo, Hospitalisations, Notifications, Logs d'audit) vers le patient primaire, puis passant le secondaire à `MERGED`. Enregistrement de l'historique dans `PatientMergedHistoryEntity`.
  - **IHM Gestion des doublons (TICKET-1304)** : Nouveau composant Angular `DuplicatesPageComponent` (page + modale assistant de fusion côte à côte) exposant les candidats doublons triés par score, permettant à un ADMIN_CLINIQUE d'ignorer ou de fusionner interactivement. Ajout des traductions i18n FR/EN. Nouveau module de route `web/src/app/clinic/duplicates/`.
  - **Téléphone optionnel (TICKET-1301)** : La colonne `phone` de la table `patients` est rendue nullable (migration V24). Le DTO `CreatePatientRequest` ne valide plus le téléphone comme obligatoire.
  - **PDF Synthèse médicale (TICKET-1305)** : Endpoint sécurisé `GET /api/patients/{id}/summary-pdf` (rôles MEDECIN/INFIRMIER/ADMIN_CLINIQUE). Génération du PDF A4 via `PdfGeneratorService` avec QR Code d'authenticité (via `QrCodeGeneratorService`). Méthode `generatePatientSummaryPdf()` dans `PatientService`. Méthode `downloadSummaryPdf()` dans `PatientApiService` Angular. Bouton de téléchargement dans `patient-detail.component.ts`.
  - **Scopes granulaires de consentements (TICKET-1306)** : Ajout des colonnes `scopes` (VARCHAR 500, défaut `medical_records,prescriptions,lab_results,allergies_history`) et `validation_channel` (VARCHAR 50, défaut `PORTAL`) sur `patient_consents`. Nouveaux getters/setters sur `PatientConsentEntity`. Migration `V23__granular_scopes_and_validation_channel.sql`.
  - **Scopes granulaires accès externes (TICKET-1307)** : Ajout du champ `scopes` sur `ExternalAccessRequestEntity` et la table `external_access_requests`. Méthodes `validateAccess()` et `hasScope()` dans `PatientService` pour une validation fine par scope. Les contrôleurs `ConsultationController`, `ConsultationHistoryController`, `LabOrderController`, `LabResultService`, `PrescriptionController`, `PatientMedicalInfoController` utilisent désormais `validateAccess(patientId, "scope")`.
  - **Page clinique de demande d'accès externe (TICKET-CLINIC-EXTERNAL-ACCESS-REQUEST)** : Création du composant Angular `ClinicAccessRequestComponent` (route `/clinic/access-request`) permettant à un clinicien (MEDECIN, INFIRMIER, ADMIN_CLINIQUE) d'initier une demande d'accès externe au DPU d'un patient. Formulaire avec saisie du numéro DPU, motif, durée (15 min / 1h / 24h / 7 jours) et sélection granulaire des scopes (Dossier médical, Ordonnances, Résultats de labo, Allergies & ATCD). Service `ExternalAccessApiService` dédié. Internationalisation complète FR/EN. Design system Tailwind CSS v4 avec arrondis sobres. Lien ajouté dans le dashboard clinique. **Correction API** : le DTO `CreateExternalAccessRequest` accepte désormais un `String patientDpu` (numéro DPU textuel) au lieu d'un `UUID patientId`, résolu via `PatientRepository.findByGlobalPatientNumber()`. Les tests `ExternalAccessControllerTest` mis à jour en conséquence.
  - **Utilitaire convertToUuid (TICKET-1306/1307)** : Méthode statique `PatientService.convertToUuid()` pour convertir des objets en UUID (utilisée dans les contrôleurs pour résoudre le `patient_id` depuis un `visit_id`).

- **Refonte UI/UX Premium (EPIC-0012)** :
  - **Sélecteur de Thème (STORY-1801)** : Ajout d'un bouton d'action premium dans le header (`AppShellComponent`) relié à `ThemeService` pour permuter dynamiquement entre thèmes Clair (Light) et Sombre (Dark), avec mémorisation dans le `localStorage`.
  - **Barre Latérale Rétractable (STORY-1802)** : Remplacement du layout classique par un modèle back-office à sidebar rétractable filtrant les menus (Tableau de bord, Cliniques, Patients, Équipe, Ordonnances, Labo) selon le rôle de l'utilisateur actif.
  - **Fil d'Ariane Dynamique (STORY-1803)** : Ajout du composant `BreadcrumbComponent` autonome retraçant en temps réel le chemin actif et gérant la traduction (i18n).
  - **Modularisation du Dossier Patient (STORY-1804)** : Découpage de l'IHM monolithique de détails patient (`PatientDetailComponent`) en sous-routes enfants (/profile, /consultations, /hospitalizations, /lab-orders, /audit-trail) avec chargement fluide par onglet-route, ramenant la taille du composant principal sous la limite de 500 lignes.
  - **Navigation Contextuelle, Portail Dédié et Layout Fluide (TICKET-UX-GOOGLE-DESIGN)** : Intégration d'un service d'état réactif `ActivePatientService` pour ajouter dynamiquement la sub-navigation du patient sélectionné dans la Sidebar back-office. Retrait des onglets internes imbriqués pour un design épuré sans double-header. Éclatement du portail Patient en 5 pages routées indépendantes avec menus dédiés dans la Sidebar. Alignement du gabarit général sur un mode fluide (100% de largeur) en libérant les marges de `.app-container` et `.app-container-wide` pour que le logo se place à l'extrémité gauche et les menus de la Topbar à l'extrémité droite.
- **Interopérabilité HL7 FHIR - Mapping (STORY-1701)** : DTOs FHIR minimaux (`FhirPatientDto`, `FhirEncounterDto`, `FhirObservationDto` et leurs sous-structures) et mappers associés (`FhirPatientMapper`, `FhirEncounterMapper`, `FhirObservationMapper`) permettant de projeter à la volée les entités JPA existantes (`PatientEntity`, `VisitEntity`, `VitalsEntity`) au format de ressources standardisé HL7 FHIR R4. Ajout de tests unitaires couvrant l'ensemble de la logique de conversion et de validation des formats.
- **Interopérabilité HL7 FHIR - Endpoints REST (STORY-1702)** : Contrôleur `FhirController` et service `FhirService` exposant les endpoints REST sécurisés `GET /fhir/Patient/{id}`, `GET /fhir/Encounter/{id}` et `GET /fhir/Observation?patient={patientId}`. Application de l'isolation multi-tenant stricte, de la sécurisation par rôles Spring Security (MEDECIN, INFIRMIER, BIOLOGISTE), de l'audit log d'accès `READ_FHIR_RESOURCE` et d'une suite de tests d'intégration MockMvc complète.

### Changed

- **Alignement des Contrôles de Formulaire (TICKET-0108)** : Harmonisation visuelle des inputs (`.ui-input`), textareas (`.ui-textarea`), cases à cocher (`.ui-checkbox`), boutons radio (`.ui-radio`) et listes déroulantes (`.ui-select`) avec les variables CSS centralisées et le rayon de courbure sobre standard (`var(--radius-brand-sm)`). Ajout des états de survol (`hover`), d'accessibilité numérique de focus visible (`focus-visible` avec `box-shadow`) et d'opacité désactivée (`disabled`).
- **Harmonisation des templates frontaux** : Remplacement des classes ad-hoc de formulaire par les classes globales standardisées `.ui-*` dans `patient-medical-info.component.ts`, `patient-hospitalization.component.ts`, `patient-consents-list.component.ts`, `patient-requests-list.component.ts`, `staff-management.component.ts`, `lab-orders-page.component.ts`, `consultation.component.ts` et `pharmacy-dispensation-panel.component.ts`.

### Fixed

- **Téléchargement PDF de Sortie (BUG)** : Résolution de l'erreur 401 Unauthorized lors du téléchargement de la fiche de sortie d'hospitalisation en remplaçant le lien d'accès direct `<a>` par un téléchargement Angular via `HttpClient` (permettant d'injecter automatiquement l'en-tête de jeton d'authentification Bearer JWT).



## [0.9.0] - 2026-07-04

### Added

- **Télétransmission d'ordonnances (STORY-1601/1602)** : migration Flyway V22 d'ajout des colonnes de télétransmission, service d'intégration simulé `AlloPharmaClient`, service métier et contrôleurs Rest médecin (`POST /api/prescriptions/{id}/transmit`) et patient (`POST /api/patient/me/prescriptions/{id}/transmit`) avec contrôles IDOR et logs d'audit. Ajout de boutons "Télétransmettre" et badges de statut IHM i18n sur le dossier praticien et portail patient, avec tests unitaires et intégration complets.
- **Configuration Git (TICKET-1603)** : Ajout du dépôt distant officiel (remote `origin`) pointant vers `https://github.com/fomekong-jocelin/joprelys-connect.git`.
- **Demandes d'Accès Externes (STORY-1301)** : migration Flyway V20, entité JPA ExternalAccessRequestEntity et Repository, Service d'enregistrement des demandes et contrôleur API REST POST /api/external-access/requests avec validation d'entrée stricte, audit log automatique et suite complète de 5 tests d'intégration MockMvc.
- **Validation de demande d'accès externe (STORY-1302)** : contrôleur API REST pour les actions du patient (liste, approbation, rejet), sécurisation stricte anti-IDOR avec audit log automatique, et IHM Angular complète (onglet "Demandes d'accès", boutons d'action d'approbation et de rejet, gestion i18n FR/EN et styles Tailwind CSS v4) avec tests d'intégration backend et tests unitaires frontend passants à 100%.
- **Contrôle d'accès & Expiration des droits (STORY-1303)** : renforcement du filtrage de sécurité dans `PatientService` pour interdire l'accès aux dossiers par des cliniques externes sans autorisation valide. Ajout du planificateur automatique `ExternalAccessExpirationScheduler` (cron `@Scheduled` configurable) pour expirer les autorisations obsolètes. Mise en place de logs d'audit critiques `EMERGENCY_DPU_ACCESS` et rendu rouge/rose dans la timeline de sécurité patient.
- **Socle de notifications backend (STORY-1501)** : migration Flyway V21 de création de la table `notifications`, entité JPA, Repository, service `NotificationService` avec passerelle simulée (logs), intégration automatique aux processus d'urgence Brise-Glace et de demandes d'accès externes, et exposition des API REST sécurisées (`GET /api/patient/me/notifications`, `POST /api/patient/me/notifications/{id}/read`, `POST /api/patient/me/notifications/read-all`).
- **Centre de notifications IHM (STORY-1502)** : composant Angular autonome `PatientNotificationsComponent`, intégration d'un 5ème onglet interactif sur le tableau de bord patient avec badge rouge dynamique de décompte en temps réel, design premium Tailwind CSS v4, styles visuels distincts par gravité, actions individuelles/globales de marquage, support i18n FR/EN, thèmes clair/sombre et suite de tests unitaires passante.

## [0.8.0] - 2026-07-04

### Added

- **Hospitalisations & Notes d'Évolution (STORY-1202)** : migration Flyway V19, entités JPA HospitalizationEntity et HospitalizationNoteEntity avec concurrence optimiste (@Version) et multi-tenant, génération de la fiche de sortie PDF avec QR Code de vérification, et intégration dans l'IHM Angular (Tailwind CSS v4, I18nService).
- **Allergies & Antécédents Médicaux (STORY-1201)** : migration Flyway V18, entités JPA PatientAllergyEntity et PatientMedicalHistoryEntity avec concurrence optimiste (@Version) et multi-tenant, endpoints CRUD sécurisés avec audit logs, et intégration dans l'IHM Angular (Tailwind CSS v4, I18nService).
- **Préparation du SPRINT-0006** : initialisation du plan de sprint et écriture des tickets pour les modules cliniques d'Allergies/Antécédents (STORY-1201) et d'Hospitalisations (STORY-1202).

### Changed

- **Alignement IHM & Enveloppe de Layout (AppShell)** : Uniformisation de toutes les pages de l'application (incluant les stocks de pharmacie et la page d'accès refusé) pour utiliser systématiquement le composant structurel `AppShellComponent` afin de garantir la présence du même en-tête (profil utilisateur, sélecteur de langue) et du même pied de page (footer) partout.

### Fixed

## [0.7.0] - 2026-07-03

### Added

- **Gestion réelle des stocks de médicaments (STORY-1103)** : table `drug_stocks` (Flyway V17), entité JPA `DrugStockEntity` avec verrouillage optimiste (`@Version`) et isolation multi-tenant (`@TenantId`), `DrugStockService` avec upsert par nom, décrémentation transactionnelle et alertes de stock bas, API CRUD `GET/POST /api/pharmacy/stocks` et `GET /api/pharmacy/stocks/alerts` réservée aux pharmaciens.
- **Pipeline CI/CD GitHub Actions (STORY-1101)** : création du workflow .github/workflows/ci.yml pour valider build Maven backend (Java 21, H2) et build Angular (Node 22, Vitest) sur chaque PR et push main/develop.
- **Import FHIR DiagnosticReport (STORY-1104)** : FhirDiagnosticReportParser Jackson (sans HAPI), endpoint POST /api/public/lab-integration/fhir/diagnostic-report sécurisé par API KEY, tests unitaires purs.
### Changed

- **Nettoyage dette technique QA (STORY-1105)** : suppression des dépendances Maven test invalides (spring-boot-starter-*-test inexistants), ajout spring-security-test, création application-test.yml, repair-on-migrate Flyway activé.

### Fixed

### Security

- **Sécurisation IDOR Portail Patient (STORY-1102)** : `PatientAccessGuardService` centralise les contrôles OWASP A01. Tout accès cross-patient retourne 403. Tests de sécurité ajoutés.

## [0.6.0] - 2026-07-03

### Fixed

- **Bypass du filtre TenantId Hibernate pour les requêtes publiques de Pharmacie (STORY-1005 / T-SYS-01)** :
  - Modification de `PharmacyService.java` pour requêter les ordonnances via la nouvelle méthode SQL native `findByPrescriptionNumberGlobally` de `PrescriptionRepository`.
  - Résolution des exceptions 404 lors des appels anonymes/publics en bypassant l'isolation multi-tenant de session de Hibernate.
  - Utilisation de requêtes SQL brutes via `jdbcTemplate` pour charger les informations d'associations filtrées par tenant (`PatientEntity` et `ConsultationEntity`) sans provoquer de `LazyInitializationException` ou de filtrage erroné par le tenant par défaut.

- **Intégrité référentielle et isolation des tests de la base de données H2** :
  - Ajout de la contrainte `ON DELETE CASCADE` sur la clé étrangère `prescription_item_id` dans la table `dispensation_items` dans le script de migration `V14__add_pharmacy_fields_and_tables.sql`.
  - Résolution des erreurs d'intégrité de clé étrangère lors du nettoyage des tables (`DELETE FROM visits`, etc.) dans les configurations `@BeforeEach` des autres suites de tests.
  - Ajout de la méthode `resetLockouts()` dans `PharmacyService.java` et appel au début du test `setUp()` de `PharmacyControllerTest` pour éviter la pollution de l'état de verrouillage (erreur 429) entre les méthodes de test.

### Changed

- **Header topbar fixe (TICKET-UI-FIXED-TOPBAR)** :
  - Rendu de la barre de navigation supérieure (`.app-topbar`) sticky (`position: sticky`) au défilement sur l'ensemble de l'application web, avec une gestion du `z-index: 50` pour garantir la superposition.

- **Redesign des vues Consentement et Sécurité du portail patient (TICKET-UI-PATIENT-PORTAL-CONSENT-SECURITY-REDESIGN)** :
  - Restructuration visuelle de la gestion des consentements (`PatientConsentsListComponent`) et du journal d'accès (`PatientAuditListComponent`) avec le conteneur `ui-card-subtle` et des styles épurés et professionnels conformes à `DESIGN.md`.
  - Remplacement de l'interrupteur capsule (en forme de pilule `rounded-full` non conforme) par des boutons de statut explicites, esthétiques et accessibles avec des rayons de courbure standardisés de 4px (`rounded-[var(--radius-brand-sm)]`).
  - Simplification visuelle du journal de sécurité avec un tableau responsive et des cartes mobiles plus denses.
  - Extraction de tous les textes visibles des composants Consentement et Sécurité dans l'injectable `I18nService` avec traduction complète FR/EN pour respecter les standards du projet.
  - Ajout de tests unitaires complets pour le composant de consentement dans `patient-portal.spec.ts`.


- **Amélioration premium du portail patient (TICKET-UI-PATIENT-PORTAL-PREMIUM-REDESIGN)** :
  - Recomposition visuelle de `/patient/dashboard` avec conteneur desktop plus large, en-tête plus sobre, DPU plus lisible et onglets accessibles sous forme de boutons.
  - Simplification de l'historique des consultations : suppression des emojis, mesures médicales en grille, couleurs secondaires réduites et action PDF moins dominante.
  - Ajout de tokens de radius manquants et d'une carte subtile réutilisable dans le design system CSS global.

### Fixed

- **Double header et alignement du header pharmacie et laboratoire (TICKET-UI-PHARMACY-LAB-HEADER-FIX)** :
  - Résolution du problème de double header dans le portail laboratoire et pharmacie en remplaçant les en-têtes personnalisés avec logo dupliqué par le composant réutilisable standard `<app-page-header>`.
  - Intégration de `<app-shell>` sur la page de vérification des ordonnances de la pharmacie pour assurer une cohérence visuelle complète (topbar avec session utilisateur, langue et logout, et footer).
  - Sécurisation de la route `pharmacy/prescriptions` avec `roleGuard` réservé aux rôles `PHARMACIEN` et `ADMIN_JOPRELYS`.
  - Nettoyage des imports inutilisés (comme `RouterLink`) pour éviter les avertissements du compilateur Angular.

- **Lisibilité du tableau patients DPU (TICKET-UI-DPU-PATIENT-TABLE-READABILITY)** :
  - Stabilisation des colonnes desktop de la liste patients pour conserver les numeros DPU, numeros locaux et l'action de consultation sur une seule ligne.
  - Conservation du responsive par defilement horizontal desktop/tablette et sans impact sur la vue mobile en cartes.

- **Correction de validation Hibernate du schema laboratoire (TICKET-1011)** :
  - Ajout d'une migration Flyway additive `V16__repair_lab_results_schema_validation.sql` pour synchroniser la table `lab_results` avec `LabResultEntity`, notamment la colonne manquante `result_value`.
  - Objectif : eviter l'echec de demarrage Spring Boot lors de la creation de `entityManagerFactory` avec `ddl-auto: validate`.

- **Correction de validation Hibernate du schema pharmacie (TICKET-1010)** :
  - Ajout d'une migration Flyway additive `V15__repair_pharmacy_schema_validation.sql` pour synchroniser les colonnes pharmacie de `prescriptions`, les tables de dispensation et leurs index sur les bases locales/de developpement ayant deja applique une variante incomplete de `V14`.
  - Objectif : eviter l'echec de demarrage Spring Boot lors de la creation de `entityManagerFactory` avec `ddl-auto: validate`.

### Added

- **Portail pharmacie — vérification et détail ordonnance (STORY-1004)** :
  - Frontend : ajout de la route publique `/pharmacy/prescriptions` pour vérifier une ordonnance via numéro et PIN.
  - Ajout de `PharmacyApiService` utilisant l'endpoint relatif `/api/public/pharmacy/prescriptions/verify`.
  - Ajout d'un écran Angular responsive affichant uniquement les données nécessaires à la délivrance : statut, patient minimal, prescripteur, dates et lignes de médicaments.
  - Raccordement de la carte pharmacien du dashboard vers le nouvel écran.
  - Ajout des traductions FR/EN et de tests unitaires frontend couvrant succès, erreur, état vide et ordonnance expirée.

- **Portail pharmacie — délivrance et historique (STORY-1005)** :
  - Backend : ajout de `POST /api/public/pharmacy/prescriptions/history`, protégé par numéro d'ordonnance + PIN, pour restituer l'historique des délivrances sans exposer le PIN dans l'URL.
  - Backend : ajout des DTOs d'historique pharmacie avec date, pharmacie, licence, médicaments, quantités et substitutions.
  - Frontend : ajout d'un panneau de délivrance partielle/totale sur `/pharmacy/prescriptions`, avec quantités servies, substitutions autorisées et disponibilité déclarative sans module de stock.
  - Frontend : affichage de l'historique des délivrances après vérification et rafraîchissement après délivrance.
  - Ajout de traductions FR/EN et de tests Angular couvrant `/verify`, `/dispense`, `/history`, succès de délivrance et erreurs backend.

- **Portail laboratoire — demandes, statuts, résultats et PDF (STORY-0904 / STORY-0905 / STORY-0906)** :
  - Backend : ajout de `GET /api/lab-orders` pour lister les demandes d'examens du tenant courant.
  - Backend : ajout de `PATCH /api/lab-orders/{id}/status` avec liste blanche de statuts CDC et audit `UPDATE_LAB_ORDER_STATUS`.
  - Frontend : ajout de la route sécurisée `/clinic/lab-orders` pour consulter les demandes reçues, ouvrir le détail, changer le statut et saisir des résultats structurés.
  - Frontend : upload PDF optionnel via `/api/public/lab-integration/upload` avec `X-API-KEY`.
  - Frontend : affichage de l'historique des résultats filtré par numéro de demande.
  - Ajout de traductions FR/EN et de tests Angular couvrant liste, statut, upload, erreur backend et historique.

- **Écran praticien de visualisation des résultats (STORY-0903)** :
  - Frontend : Onglet "Analyses & Labo" sur la fiche patient affichant la liste des demandes et l'historique détaillé des examens biologiques.
  - Graphique SVG interactif et réactif traçant l'évolution temporelle des marqueurs cliniques (ex : Glucose).
  - Code couleur dynamique (Vert/Bleu/Orange/Rouge) selon l'interprétation clinique (`NORMAL`, `BAS`, `ELEVE`, `CRITIQUE`).
  - Validation de la logique et du tracé par l'ajout de nouveaux tests unitaires dans `patient-detail.component.spec.ts` (39/39 tests au vert).
- **Rembobinage & Réparation de base de données (Gouvernance/CI-CD)** :
  - Configuration de `spring.flyway.repair-on-migrate: true` dans `application.yml` pour réparer de manière automatisée les anomalies de checksum Flyway locales/de dev en cas de script modifié.


- **Téléversement de résultats d'analyses par les labos externes (STORY-0902)** :
  - Backend : endpoints publics `/api/public/lab-integration/upload` sécurisés par clé d'API (`X-API-KEY`) validée via la propriété configurable `joprelys.lab-integration.api-key`.
  - Intégration de la logique de décodage du PDF en Base64 et stockage physique des fichiers de résultats dans le répertoire de stockage (`joprelys.documents.storage-dir`).
  - Insertion structurée des constantes et des lignes d'analyses dans la table `lab_results` et mise à jour du statut de la demande `LabOrderEntity` vers `VALIDATED`.
  - Enregistrement automatique d'un log d'audit de succès `UPLOAD_LAB_RESULTS` pour garantir la traçabilité.
  - Couverture complète par des tests d'intégration automatisés avec MockMvc (100% de succès).


- **Demande d'examens biologiques par le médecin (STORY-0901)** :
  - Backend : table `lab_orders`, entité JPA `LabOrderEntity` isolée par `@TenantId` multi-tenant.
  - Endpoints REST `/api/lab-orders` (création) et `/api/lab-orders/patient/{patientId}` (liste) sécurisés par rôles (MEDECIN, ADMIN_CLINIQUE).
  - Génération automatique de numéro d'examen séquentiel quotidien unique formaté `EXAM-REQ-YYYYMMDD-XXXXXX`.
  - Frontend : Bloc IHM interactif intégré dans l'écran de consultation médecin (`ConsultationComponent`) avec suggestions d'analyses courantes (NFS, Glycémie, Bilan lipidique/rénal, CRP...) et formulaire d'ajout personnalisé.
  - Intégration et enchaînement asynchrones des appels d'enregistrement (consultation + prescription + examens) et chargement transparent des examens pré-existants.
  - Tests d'intégration et unitaires backend validés avec succès (107/107). Compilation Angular de production OK.

## [0.5.0] - 2026-07-03

### Added

- **Refonte de la gestion des cliniques pilotes (TASK-0901)** :
  - Backend : endpoint de mise à jour `PUT /api/organizations/{id}` et enrichissement de `OrganizationResponse` pour renvoyer le nom et l'email de l'administrateur de clinique.
  - Frontend : affichage en direct de l'administrateur affecté dans le tableau de bord (et tag d'alerte si absent).
  - Détail & Édition : implémentation d'un tiroir latéral (Drawer) interactif et animé pour visualiser les détails de la clinique, modifier directement ses informations en place et affecter/remplacer son administrateur clinique.
- **Récupération de mot de passe simplifiée (STORY-0103)** :
  - Backend : endpoints publics `/api/public/auth/password-recovery/request` et `/api/public/auth/password-recovery/reset` avec validation de formulaire.
  - Gestion des codes OTP de sécurité à 6 chiffres stockés en mémoire avec limite à 3 essais incorrects et expiration après 5 minutes.
  - Frontend : composant standalone `ForgotPasswordComponent` avec formulaire multi-étape dynamique (Email -> OTP & Nouveau mot de passe -> Succès), raccordement des routes et i18n FR/EN.
  - Sécurité : protection contre l'énumération d'adresses e-mail (renvoi de 200 OK pour tout email demandé, seul un utilisateur existant et actif de clinique active reçoit l'OTP), hashage BCrypt pour le stockage en base de données.
  - Tests unitaires et d'intégration validés à 100% (Backend MockMvc et Frontend Vitest).

- **Affichage détaillé des consultations (IHM Accordéon)** : 
  - Ajout d'un panneau dépliable (accordéon) sur les listes de consultations du médecin (`PatientDetailComponent`) et du patient (`PatientVisitsListComponent`).
  - Rendu responsive et esthétique (grille de capsules pour les constantes vitales, observations structurées et tableau de prescriptions).
  - Enrichissement de `ConsultationResponse` et `PatientPortalMeResponse.PatientPortalConsultation` pour mapper dynamiquement l'intégralité du dossier sans requêtes N+1.
- Unification de la page de connexion à la racine `/` : suppression de la page `/patient/login` au profit d'un sélecteur d'espace animé "Personnel de santé / Patient" embarquant directement le formulaire OTP double étape.
- Refonte professionnelle du sélecteur de langue sous forme statique `FR | EN` pour éviter l'échec de rendu des drapeaux émojis sur Windows.
- Mémorisation et restauration automatique de la langue choisie par l'utilisateur via le `localStorage`.
- Intégration de l'icône de déconnexion et alignement soigné des métadonnées utilisateur (avatar, nom, rôle) en topbar.

### Fixed

- **LazyInitializationException sur les lignes d'ordonnances** : Résolution du plantage Hibernate lors du dépliage ou de la génération de document en modifiant `PrescriptionRepository.findByConsultationId` pour charger immédiatement la collection `items` avec un `LEFT JOIN FETCH p.items`.
- Résolution de l'erreur 500 `LazyInitializationException` lors du téléchargement d'ordonnances (modules clinique et patient) par l'utilisation de requêtes JPQL avec `JOIN FETCH` (visite + patient) et la suppression de la transaction `readOnly` bloquant l'enregistrement des audits.

- Implémentation complète de la User Story [STORY-0801](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-0801-espace-patient.md) (Espace patient sécurisé et historique personnel) :
  - **Backend** : Création des endpoints d'authentification OTP patients (`/api/public/patient/auth/otp` et `verify`), du contrôleur sécurisé patient (`/api/patient/me`), de la méthode de génération de jeton JWT patient et gestion du multi-tenant avec Hibernate en mode natif.
  - **Frontend** : Création de `PatientLoginComponent` (formulaire double étape), `PatientDashboardComponent` (orchestration de l'espace patient), et des sous-composants réutilisables `PatientProfileCardComponent` et `PatientVisitsListComponent` dans un grid responsive respectant [DESIGN.md](file:///C:/MES-APPLICATIONS/joprelys-connect/DESIGN.md).
- Création du fichier de Design System centralisé [DESIGN.md](file:///C:/MES-APPLICATIONS/joprelys-connect/DESIGN.md) à la racine pour standardiser les tokens (couleurs, typographie, espacements, radius) et les règles UI (Tailwind CSS v4 CSS-first).
- Cadrage et raffinement de la première User Story [STORY-0801](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-0801-espace-patient.md) (Espace patient sécurisé et historique personnel) avec rédaction de sa spécification fonctionnelle et de son architecture technique.
- Mise à jour de `VerificationComponent` (page publique de vérification d'authenticité) pour prendre en compte et afficher correctement les statuts révoqués (`REVOQUE` / `REVOKED`) et annulés (`ANNULE` / `CANCELLED`) du document médical avec des styles et des libellés adaptés (STORY-0602 / STORY-0603).
- Cadrage initial de l'`EPIC-0008` (Portail Patient & Consentement) avec documentation de l'objectif, du périmètre et rédaction des 4 user stories associées (STORY-0603).
- Refactoring majeur de l'écran de détail du patient `PatientDetailComponent` avec un design à 3 onglets (Fiche Patient, Dossier Médical, Journal d'Audit) conforme à Material Design 3 pour éliminer la surcharge cognitive.
- Déplacement des boutons d'actions principales ("Démarrer la consultation", "Retour") en haut de la fiche dans un en-tête structuré et responsive.
- Intégration de l'affichage du journal d'audit et sécurité en timeline sous forme collapsible dans le détail du patient unique (STORY-0702 / EPIC-0007).
- Service d'API Angular `AuditApiService` et clés d'internationalisation FR/EN pour le support multilingue de la traçabilité (STORY-0702).
- Tests unitaires et d'autorisation d'affichage basés sur les rôles de l'utilisateur actif (STORY-0702).
- Implémentation du module de traçabilité et logs d'audit (backend) (STORY-0701 / EPIC-0007).
- Exposition des API REST sécurisées `GET /api/audit/patients/{patientId}` et `GET /api/audit/organizations/{organizationId}` avec isolation multi-tenant stricte (STORY-0701).
- Ajout d'écouteurs d'événements Spring Security pour journaliser automatiquement les connexions et échecs d'authentification (STORY-0701).
- Intégration d'audit logs automatiques lors de la création/consultation de patients et d'actions sur les documents médicaux (génération, téléchargement, révocation, annulation) (STORY-0701).
- Ajout de la table de données `audit_logs` indexée sous Flyway migration V10 (STORY-0701).
- Mise à niveau du kit de gouvernance IA, Scrum et Architecture vers la version v0.3.8 (TICKET-0108).
- Ajout de standards de design system Tailwind CSS v4 CSS-first et d'arrondis sobres (4px-6px) sous `docs/standards/DESIGN-SYSTEM-STANDARDS.md` et `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md` (TICKET-0108).
- Ajout de standards d'architecture SOLID et limitation stricte à 500 lignes par classe et 40 lignes par méthode sous `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md` (TICKET-0108).
- Préservation et isolation des fichiers de données projet (`VERSION`, `CHANGELOG.md`, `PROJECT-TRACKING.md`, `DELIVERY-DASHBOARD.md`, `VERSION-MATRIX.md`) (TICKET-0108).
- Implémentation frontend de l'écran de révocation et d'annulation de documents médicaux pour les cliniciens (STORY-0604 / EPIC-0006).
- Enrichissement de la réponse `ConsultationResponse` sur le backend pour propager les identifiants et statuts de documents sans requêtes N+1 (STORY-0604).
- Ajout du bouton d'action et d'une modale de confirmation interactive avec radio-boutons de type d'action et motif d'audit sur `PatientDetailComponent` (STORY-0604).
- Ajout de 4 tests unitaires frontend dans `patient-detail.component.spec.ts` validant le cycle de révocation/annulation (STORY-0604).
- Implémentation backend de la révocation et de l'annulation de documents médicaux (STORY-0603 / EPIC-0006).
- Ajout des endpoints sécurisés `PATCH /api/documents/{id}/revoke` (statut → `REVOQUE`) et `PATCH /api/documents/{id}/cancel` (statut → `ANNULE`), réservés aux rôles `MEDECIN` et `ADMIN_CLINIQUE` (STORY-0603).
- Ajout des champs de traçabilité `revoked_at`, `revoked_by_user_id`, `revocation_reason` dans la table `medical_documents` via migration Flyway `V9__add_revocation_to_medical_documents.sql` (STORY-0603).
- Ajout de la méthode métier `revoke()` dans `MedicalDocumentEntity` pour centraliser la logique de changement de statut avec audit (STORY-0603).
- Ajout des DTOs `RevokeDocumentRequest` (motif obligatoire ≤ 500 caractères) et `DocumentStatusResponse` (métadonnées d'audit sans données médicales) (STORY-0603).
- Ajout de 5 tests d'intégration dans `DocumentRevocationControllerTest` couvrant révocation, annulation, double révocation (409), rôle non habilité (403) et accès anonyme (401) (STORY-0603).
- Implémentation backend de la gestion du personnel clinique (STORY-0104 / EPIC-0001).
- Ajout de l'API sécurisée `ADMIN_CLINIQUE` `/api/staff` pour lister, inviter, modifier et activer/désactiver les collaborateurs d'une clinique (STORY-0104).
- Ajout de `StaffService` avec isolation multi-tenant par `organizationId`, validation des rôles cliniques, génération du mot de passe temporaire `Jop-XXXXXX` et hash BCrypt (STORY-0104).
- Ajout de `StaffControllerTest` couvrant liste tenant-aware, invitation, doublon email, RBAC, cross-tenant, modification et désactivation bloquant la connexion (STORY-0104).
- Implémentation Angular de la page `/clinic/staff` pour la gestion du personnel clinique, réservée à `ADMIN_CLINIQUE` (STORY-0104).
- Ajout de `StaffApiService`, `StaffManagementComponent` et `StaffManagementComponent` tests pour lister, inviter, modifier, copier le mot de passe temporaire et suspendre/réactiver les collaborateurs (STORY-0104).
- Ajout de l'accès "Équipe clinique" dans le dashboard et des traductions FR/EN associées (STORY-0104).
- Amélioration et refondation du modal de saisie des constantes vitales (design premium, groupements logiques, icônes, et validation client en temps réel avec désactivation du bouton d'enregistrement).
- Backend : Ajout de l'endpoint `GET /api/visits/{id}` requis lors du chargement initial de la saisie de consultation.
- Sécurité : Configuration de Spring Security pour autoriser `/error` publiquement, évitant de transformer les erreurs 404 en 401 Unauthorized.
- Implémentation de la page publique de vérification d'authenticité et intégration du téléchargement de PDF (STORY-0602 / EPIC-0006).
- Création du composant public `VerificationComponent` avec design premium, responsive, support du mode sombre et protection du secret médical (RGPD).
- Ajout de la route anonyme `/verify/:documentId` dans `app.routes.ts`.
- Intégration du bouton "Télécharger PDF" dans la section historique médical du composant `PatientDetailComponent` avec gestion d'erreurs en cas de visite non clôturée.
- Implémentation de la génération et du stockage des documents PDF médicaux lors de la clôture des visites (STORY-0601 / EPIC-0006).
- Ajout de l'entité JPA `MedicalDocumentEntity` et de son repository `MedicalDocumentRepository` avec support multi-tenant via `@TenantId`.
- Ajout du générateur séquentiel `DocumentNumberGenerator` (format `DOC-YYYYMMDD-XXXXXX`) contournant le tenant context via `JdbcTemplate`.
- Ajout du générateur de QR codes `QrCodeGeneratorService` via `zxing`.
- Ajout du générateur de PDF `PdfGeneratorService` via `openpdf` (conversion en format de tableau moderne `PdfPTable` et `PdfPCell`).
- Ajout de `DocumentService` pour l'orchestration de la génération du QR code et du PDF, le stockage sur le disque local, et la persistance en base.
- Intégration de la génération automatique dans `VisitService.closeVisit()`.
- Ajout de `DocumentController` exposant un endpoint sécurisé de téléchargement de PDF `/api/visits/{visitId}/document` (RBAC) et un endpoint public anonyme de vérification `/api/public/documents/{id}/verify`.
- Adaptation de `SecurityConfig` pour ouvrir l'accès public à `/api/public/**`.
- Ajout de tests d'intégration complets dans `DocumentControllerTest` (vérification de la clôture, du téléchargement sécurisé, du cross-tenant, et de la validation publique sans informations sensibles).
- Implémentation du module de consultation médicale backend (STORY-0501 / EPIC-0005).
- Ajout de la migration Flyway `V6__create_consultation_table.sql` créant la table `consultations` (OneToOne avec `visits`, FK vers `users`, `organization_id` multi-tenant, champs `symptoms`, `clinicalExam`, `diagnosis`, `advice`, `followUp`, `status`).
- Ajout de l'entité JPA `ConsultationEntity` avec annotation `@TenantId` Hibernate pour l'isolation multi-tenant et relation ManyToOne lazy vers `UserAccountEntity` (médecin) (STORY-0501).
- Ajout du `ConsultationRepository` avec requête JPQL `findByVisitId()` et `existsByVisitId()` (STORY-0501).
- Ajout du `ConsultationService` exposant un pattern upsert (création ou mise à jour) résolvant le médecin via `UserAccountRepository.findByEmail()` depuis le principal JWT (STORY-0501).
- Ajout du `ConsultationController` exposant `POST /api/visits/{id}/consultation` (rôles MEDECIN, ADMIN_CLINIQUE) et `GET /api/visits/{id}/consultation` (tous les rôles cliniques) (STORY-0501).
- Ajout du `ConsultationControllerTest` avec 11 cas de test couvrant création, upsert, lecture, RBAC (403), validation Bean (400), visite inexistante (404), sans token (401), isolation multi-tenant cross-tenant (404) et visite clôturée (400) (STORY-0501).
- Implémentation du module de prescription médicale backend (STORY-0502 / EPIC-0005).
- Ajout de la migration Flyway `V7__create_prescription_table.sql` créant les tables `prescriptions` (OneToOne avec `consultations`, @TenantId) et `prescription_items` (1-N médicaments par prescription) (STORY-0502).
- Ajout de `PrescriptionEntity`, `PrescriptionItemEntity`, `PrescriptionRepository` (pattern upsert avec `orphanRemoval`) (STORY-0502).
- Ajout du `PrescriptionService` et du `PrescriptionController` exposant `POST/GET /api/consultations/{id}/prescription` (rôles MEDECIN/ADMIN_CLINIQUE pour la saisie, tous les rôles cliniques + PHARMACIEN pour la lecture) (STORY-0502).
- Ajout du `PrescriptionControllerTest` avec 9 cas de test (création, upsert, lecture, RBAC 403, liste vide 400, 404, 401, isolation multi-tenant) (STORY-0502).
- Ajout de `ConsultationHistoryController` exposant `GET /api/patients/{id}/consultations` (liste paginée des consultations d'un patient, rôles MEDECIN/ADMIN_CLINIQUE/INFIRMIER) (STORY-0504 backend).
- Ajout de `findByPatientIdOrderByCreatedAtDesc()` dans `ConsultationRepository` et `getConsultationsByPatientId()` dans `ConsultationService` (STORY-0504 backend).
- Implémentation du module consultation Angular (STORY-0503 / EPIC-0005).
- Ajout de `consultation.models.ts` (interfaces Consultation, Prescription, PrescriptionItem, SaveConsultationRequest, SavePrescriptionRequest) (STORY-0503).
- Ajout de `ConsultationApiService` avec 5 méthodes HTTP (saveConsultation, getConsultation, savePrescription, getPrescription, getPatientConsultations) (STORY-0503).
- Ajout du `ConsultationComponent` standalone (route `/clinic/consultation/:visitId`) : formulaire réactif complet (symptoms, clinicalExam, diagnosis, advice, followUp) + FormArray prescription avec lignes dynamiques ajoutables/supprimables + affichage des constantes vitales en lecture seule (STORY-0503).
- Ajout du bouton "Démarrer la consultation" dans le drawer du tableau de bord, accessible aux rôles MEDECIN et ADMIN_CLINIQUE, naviguant vers l'écran de consultation (STORY-0503).
- Ajout de la section accordéon "Historique Médical" dans `PatientDetailComponent` : chargement lazy des consultations passées avec date, diagnostic, médecin ; spinner ; message vide (STORY-0504 frontend).

### Fixed

- Correction de la résolution du nom de l'acteur (`actorName`) dans l'API et la timeline du journal d'audit de sécurité (STORY-0702).
- Correction de la référence de clé étrangère dans `V6__create_consultation_table.sql` : `REFERENCES user_accounts(id)` → `REFERENCES users(id)` (nom réel de la table défini dans `V1__create_users_and_auth_audit.sql`) (STORY-0501).

- Implémentation du module de gestion des visites et de la file d'attente active (STORY-0401).
- Ajout du script de migration Flyway de création de la table SQL `visits` rattachée au patient et à l'organisation (STORY-0401).
- Ajout de l'entité JPA `VisitEntity` avec relation FetchType.LAZY pour la performance mémoire et liaison multi-tenant (STORY-0401).
- Ajout du service `VisitNumberGenerator` générant des numéros de visites uniques `VIS-YYYYMMDD-XXXXXX` sans collision inter-tenant (STORY-0401).
- Ajout du service métier `VisitService` et du contrôleur REST `VisitController` exposant les endpoints d'ouverture, de liste et de clôture de visites (STORY-0401).
- Ajout de l'Exception Handler global pour `ResponseStatusException` dans `AuthExceptionHandler` afin de propager proprement les détails d'erreurs d'API sous format ProblemDetail (STORY-0401).
- Ajout de l'intégration Angular `VisitApiService`, de la boîte de dialogue d'ouverture de visite sur `PatientDetailComponent` et du tableau/cartes de file d'attente active sur le tableau de bord clinique (STORY-0401).
- Implémentation du Dossier Patient Unique (DPU) et de l'enregistrement de patients (STORY-0301).
- Ajout de la table SQL `patients` avec contraintes et indexes pour optimiser la recherche (STORY-0301).
- Ajout des APIs d'enregistrement, de recherche et de détails des patients (/api/patients) (STORY-0301).
- Ajout des composants Angular `PatientListComponent`, `PatientFormComponent` et `PatientDetailComponent` intégrant la charte graphique, i18n, thème et mobile-first (STORY-0301).
- Implémentation de l'isolation logique multi-tenant via `@TenantId` d'Hibernate 6, `TenantContext` thread-local et `TenantIdentifierResolver` (STORY-0301).
- Ajout du service de génération d'identifiants séquentiels `PatientNumberGenerator` par jour pour le DPU et le numéro local (STORY-0301).
- Amélioration de la gestion globale des exceptions de validation d'API via `AuthExceptionHandler` pour renvoyer des messages d'erreurs de champs détaillés, et intégration côté frontend pour les formulaires patients et organisations.
- Implémentation de la structure de clinique pilote multi-tenant de base (STORY-0201).
- Ajout de la table SQL `organizations` et clé étrangère `organization_id` associée sur la table `users` (STORY-0201).
- Ajout des APIs d'administration des organisations (/api/organizations) réservées au rôle `ADMIN_JOPRELYS` (STORY-0201).
- Ajout du blocage d'authentification pour les utilisateurs de cliniques désactivées (statut `INACTIVE`) (STORY-0201).
- Ajout de l'écran d'administration Angular de gestion et liste de cliniques pilotes (/organizations) (STORY-0201).
- Implémentation du contrôle d'accès basé sur les rôles (RBAC) de bout en bout (STORY-0102).
- Ajout de l'autorisation d'accès par méthode Spring Security (@EnableMethodSecurity) et d'endpoints de test restrictifs (/api/clinic/*) (STORY-0102).
- Ajout du guard Angular de permissions (role.guard.ts) et de l'écran d'accès refusé (unauthorized.component.ts) (STORY-0102).
- Ajout du Tableau de Bord clinique dynamique affichant les menus selon le rôle (dashboard.component.ts) (STORY-0102).
- Mise en place d'une gouvernance IA centralisée.
- Ajout d'un workflow obligatoire pour Codex, Gemini, Claude Code et autres IA.
- Ajout d'un modèle de ticket actionnable avec cases à cocher.
- Ajout d'un fichier de suivi global `PROJECT-TRACKING.md`.
- Ajout d'un fichier de références techniques.
- Ajout d'une couche Chef de projet / Scrum / Delivery.
- Ajout d'un guide de capacité sprint.
- Ajout d'un guide d'estimation par story points et profils.
- Ajout d'un modèle de dashboard delivery.
- Ajout de templates Epic, User Story, Task, Sprint Plan, Timesheet et Weekly Report.
- Ajout de `TICKET-0101` pour la connexion/deconnexion securisees.
- Ajout de l'API backend `/api/auth/login` et `/api/auth/logout` avec JWT, BCrypt, revocation en memoire et audit des tentatives.
- Ajout des migrations Flyway `users` et `auth_audit_events`.
- Ajout de l'ecran Angular de connexion, du stockage `sessionStorage` et de l'intercepteur Bearer pour les requetes `/api/*`.
- Ajout de tests unitaires backend JWT/auth et de tests Angular login/intercepteur.

### Changed

- Reprise mobile-first de l'écran Angular de gestion des cliniques pilotes : shell applicatif commun, header/footer persistants, thème light par défaut, composants UI réutilisables et affichage liste ou formulaire exclusif (STORY-0201).
- Renforcement du `SKILL.md` avec une règle anti-ticket isolé.
- Extension explicite aux projets Spring Boot, Angular et Flutter.
- Extension du template de ticket avec estimation, profil recommandé, sprint, reviewer et suivi du temps.
- Extension du seeder d'utilisateurs (`AdminUserSeeder`) pour initialiser également les profils médecin (`medecin@...`) et pharmacien (`pharmacien@...`) de test.
- Migration de l'outil de build backend de Gradle vers Maven (TICKET-0104).
- Intégration de Tailwind CSS v4 dans le projet frontend Angular (TICKET-0104).
- Refonte de l'interface de connexion et de session active selon le style épuré "Innerly" (TICKET-0106).
- Intégration de la charte graphique et des polices Montserrat/Inter de Joprelys HealthTech (TICKET-0106).

### Fixed

- Correction du chargement de la file d'attente active du dashboard clinique et chargement des constantes avec les visites actives pour permettre la saisie fiable des constantes après ouverture de visite (STORY-0402).
- Conception et implémentation d'un volet latéral de détails (Drawer / Slide-over) pour la file d'attente active du tableau de bord : affichage complet et aéré des constantes de tri (icônes et cartes associées) et du motif de visite. Ce volet glisse de la droite sur grand écran et du bas vers le haut (Bottom Sheet scrollable) sur mobile (STORY-0402).
- Correction de l'erreur de DataSource PostgreSQL manquante au démarrage du backend, et initialisation de l'administrateur système conforme aux standards de production avec SLF4J et contrôle d'activation (TICKET-0102).
- Configuration du proxy de développement Angular pour rediriger les requêtes `/api/*` vers le backend Spring Boot (TICKET-0103).
- Amélioration des contrastes de couleurs des textes et liens d'action en mode sombre sur le tableau de bord pour la conformité WCAG AA (TICKET-0107).

### Security

- Ajout de références obligatoires OWASP Top 10, OWASP ASVS, OWASP MASVS et OWASP API Security.
- Secret JWT obligatoire via configuration, sans valeur secrete par defaut.
- Erreur de connexion generique pour reduire le risque d'enumeration de comptes.

## [0.3.0] - 2026-07-01

### Added

- Ajout de la gouvernance Semantic Versioning.
- Ajout du fichier racine `VERSION`.
- Ajout de `docs/release/SEMANTIC-VERSIONING.md`.
- Ajout de `docs/release/RELEASE-WORKFLOW.md`.
- Ajout de `docs/release/VERSION-MATRIX.md`.
- Ajout des templates de release note et décision de version.

### Changed

- Mise à jour des instructions IA pour imposer l'analyse PATCH / MINOR / MAJOR.
- Extension des templates ticket, epic, story, task et sprint avec l'impact version.

## [0.2.0] - 2026-07-01

### Added

- Version complète Engineering + Scrum Governance.

## [0.1.0] - 2026-06-29

### Added

- Première version du kit documentaire IA.
