# STORY-0803 — Gestion des consentements d'accès du DPU

## 1. Objectif

Permettre au patient de contrôler qui peut accéder à ses données médicales (DPU) en accordant ou révoquant le consentement d'accès pour chaque établissement du réseau. Mettre en place un blocage d'accès côté clinique en l'absence de consentement, tout en offrant une procédure d'urgence "Brise-Glace" (Break-Glass) tracée en audit.

## 2. Critères d'acceptation

### Côté Patient (Portail Patient)
- [x] Un onglet ou une section "Consentements" est accessible sur le tableau de bord patient.
- [x] Le patient y voit la liste des cliniques du réseau Joprelys Connect.
- [x] Chaque clinique dispose d'un interrupteur (toggle) ou bouton permettant d'activer ("Accorder l'accès") ou désactiver ("Révoquer l'accès") le consentement d'accès.
- [x] Par défaut, l'établissement créateur du dossier dispose d'un consentement accordé (`ACTIVE`).

### Côté Clinique (Portail Professionnel)
- [x] Si une clinique n'a pas le consentement actif pour un patient, toute tentative de lire ses données (détail patient, constantes, consultations) doit renvoyer une erreur `403 FORBIDDEN` avec le code d'erreur `CONSENT_REQUIRED`.
- [x] L'interface clinique affiche alors un écran de blocage expliquant que le consentement est manquant.
- [x] Un bouton d'urgence **"Brise-Glace"** (Break-Glass) est disponible sur cet écran de blocage pour les rôles cliniques (`MEDECIN`, `INFIRMIER`, `ADMIN_CLINIQUE`).
- [x] Cliquer sur "Brise-Glace" ouvre une modale exigeant la saisie d'une justification médicale d'urgence.
- [x] Après validation, l'accès au DPU est débloqué pour la clinique pour la session courante (ou via une autorisation d'urgence persistante) et un log d'audit critique de type `EMERGENCY_ACCESS` est enregistré avec la justification.
- [x] Un bandeau d'avertissement rouge est affiché sur la fiche patient pour rappeler que l'accès a été forcé pour motif d'urgence.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0008 (Portail Patient & Consentement) |
| User story parent | STORY-0803 |
| Sprint cible | SPRINT-0003 (en cours) |
| Priorité business | P0 |
| Complexité | L |
| Story points | 8 |
| Profil recommandé | Senior |
| Effort estimé senior | 2.5j |
| Effort estimé intermédiaire | 4j |
| Effort estimé junior | 6j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Fort |
| Risque technique | Fort |
| Dépendances | STORY-0801 (Espace patient), EPIC-0007 (Audit Logs) |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu
- [x] `DESIGN.md` (Design System centralisé)
- [x] `docs/features/patient-portal/FUNCTIONAL-SPEC.md`
- [x] `docs/features/patient-portal/TECHNICAL-DESIGN.md`

## 5. Hypothèses

- La table `consents` ou `patient_consents` stockera les paires `(patient_id, organization_id, status, granted_at, revoked_at)`.
- Un accès Brise-Glace crée une entrée d'urgence temporaire en base ou s'appuie sur une table d'autorisations d'urgence `emergency_access_authorizations` (contenant `patient_id`, `organization_id`, `doctor_id`, `reason`, `expires_at`).

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Blocage abusif en cas d'urgence vitale | Très Fort | Le bouton "Brise-Glace" doit être disponible immédiatement sur l'écran de blocage sans exiger de validation externe complexe. |
| Contournement des règles de consentement | Fort | Sécurisation stricte au niveau du backend (filtre de sécurité, validation dans `PatientService`). |

## 7. Action plan

### Phase 1 : Spécifications et Modèle de Données (Backend)
- [x] Mettre à jour `FUNCTIONAL-SPEC.md` et `TECHNICAL-DESIGN.md` pour y inclure la modélisation de `PatientConsentEntity` et `EmergencyAccessEntity`.
- [x] Créer les entités et tables de base de données associées.
- [x] Créer les repositories associés.

### Phase 2 : Logique Métier & Sécurisation API (Backend)
- [x] Implémenter la vérification du consentement dans `PatientService.getPatientById` et lever `403 FORBIDDEN` avec le code `CONSENT_REQUIRED` si absent.
- [x] Créer un endpoint `POST /api/patient/consents` pour permettre au patient d'activer/désactiver le consentement pour une clinique.
- [x] Créer un endpoint `POST /api/patients/{patientId}/emergency-access` pour déclencher la procédure Brise-Glace et enregistrer l'audit log.
- [x] Ajouter les tests d'intégration dans `PatientConsentTest.java`.

### Phase 3 : Interfaces Utilisateur (Frontend)
- [x] Créer le composant de gestion de consentement patient `PatientConsentListComponent` sur le portail patient.
- [x] Mettre à jour `PatientDetailComponent` côté clinique pour capturer l'erreur `403 CONSENT_REQUIRED` et afficher l'écran de blocage.
- [x] Créer la modale de justification d'accès d'urgence et le bouton "Brise-Glace".
- [x] Afficher le bandeau rouge d'alerte d'accès d'urgence sur la fiche d'identité du patient.
- [x] Écrire les tests unitaires et d'intégration frontend.

## 8. Implémentation réalisée
Implémentation complète backend (entités JPA, repositories, service de contrôle de consentement, endpoints d'urgence, Flyway migration V11) et frontend (gestion des onglets sur le dashboard patient, composant de liste des consentements par établissement, intercepteur d'accès DPU et écran Brise-Glace avec saisie de justification d'urgence côté clinique).

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-02 | Antigravity | 0.1j | 10% | Phase 1, 2 & 3 | Aucun | Cadrage initial et ticket créé |
| 2026-07-02 | Antigravity | 0.8j | 100% | Aucun | Aucun | Développement backend et frontend terminé et testé |

## 10. Tests et vérifications
- **Backend** : 94 tests unitaires et d'intégration Spring Boot (incluant `PatientPortalControllerTest` et `PatientServiceTest`) passent à 100%.
- **Frontend** : 31 tests unitaires et d'intégration (incluant `PatientPortalService` et `PatientListComponent`) passent à 100%.

## 11. Documentation
- [x] Spécification fonctionnelle complétée dans `docs/features/patient-portal/FUNCTIONAL-SPEC.md`
- [x] Spécification technique complétée dans `docs/features/patient-portal/TECHNICAL-DESIGN.md`

## 12. Reste à faire
Aucun.

## 13. Statut final
Statut : **DONE**

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Gestion des consentements d'accès du DPU et procédure d'urgence Brise-Glace |
| Breaking change | Non |

## 15. Impact thème / i18n / branding
- [x] Nouveaux libellés i18n FR/EN.
- [x] Bandeau rouge d'alerte et design système respectés.
