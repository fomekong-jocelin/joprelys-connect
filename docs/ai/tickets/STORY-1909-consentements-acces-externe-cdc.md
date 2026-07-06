# STORY-1909 — Alignement Module 12 : Consentements patient et accès externe

| Champ | Valeur |
|---|---|
| **ID** | STORY-1909 |
| **Epic** | EPIC-0014 |
| **Type** | User Story |
| **Titre** | Alignement Module 12 — Consentements patient et accès externe conformes CDC |
| **Statut** | IN_PROGRESS |
| **Priorité** | P0 |
| **Stack** | Full-stack |
| **Profil recommandé** | Senior Backend + Frontend Intermédiaire |
| **Estimation Senior** | 1.5j |
| **Estimation Intermédiaire** | 2.5j |
| **Estimation Junior** | 4.0j |
| **Sprint cible** | SPRINT-0011 |
| **Assigné** | Antigravity |
| **Reviewer** | Lead Developer |
| **Dernière MAJ** | 2026-07-05 |

---

## 1. Contexte

Le CDC définit plusieurs types de consentement (ponctuel, temporaire, par établissement, par professionnel, limité, urgence) avec des statuts complets (`DEMANDE`, `ACCEPTE`, `REFUSE`, `EXPIRE`, `REVOQUE`) et des canaux de validation (`OTP`, `APP`, `AGENT_HABILITE`). L’implémentation actuelle est limitée au consentement par établissement binaire.

---

## 2. Critères d’acceptation

### Backend

- [x] `PatientConsentEntity` contient `requester_user_id`, `requester_organization_id`, `reason`, `requested_at`, `approved_at`, `expires_at`, `consent_type` (enum), `validation_channel` (enum).
- [x] Statuts : `REQUESTED`, `APPROVED`, `REJECTED`, `EXPIRED`, `REVOKED`.
- [x] Types de consentement : `PONCTUEL`, `TEMPORAIRE`, `ETABLISSEMENT`, `PROFESSIONNEL`, `LIMITE`, `URGENCE`.
- [x] FR-CONSENT-003 : chaque consentement a une durée (`expires_at`).
- [x] FR-CONSENT-004 : le patient peut révoquer un consentement.
- [x] FR-CONSENT-005 : tout accès basé sur consentement est journalisé.
- [ ] Une demande d’accès externe approuvée génère un `PatientConsent` temporaire.
- [x] Le patient peut révoquer une demande d’accès externe déjà approuvée.
- [x] OTP d’approbation disponible comme canal de validation.

### Frontend

- [x] Interface de gestion des consentements avec types et durées.
- [x] Interface de révocation des accès approuvés.
- [x] Affichage de l’historique des consentements.
- [x] Internationalisation FR/EN.

---

## 3. Tâches techniques

### Backend

1. [x] Migration V38 : remodeler `patient_consents` avec les nouveaux champs.
2. [x] Créer enums `ConsentType`, `ConsentStatus`, `ValidationChannel`.
3. [x] Modifier `PatientService.validateAccess()` pour prendre en compte `expires_at` et les types.
4. [ ] Modifier `ExternalAccessService` pour créer un `PatientConsent` temporaire à l’approbation.
5. [x] Ajouter endpoint de révocation côté patient.
6. [x] Implémenter OTP d’approbation via `PatientAuthService`.
7. [ ] Tests.

### Frontend

1. [x] Modifier `patient-consents-list.component.ts` et `patient-consents-page.component.ts`.
2. [x] Modifier `patient-requests-list.component.ts` pour la révocation.
3. [x] Mettre à jour `patient-portal.service.ts`.
4. [ ] Tests.

---

## 4. Fichiers impactés

### Backend

- `patient/infrastructure/persistence/PatientConsentEntity.java`
- `patient/infrastructure/persistence/ExternalAccessRequestEntity.java`
- `patient/application/PatientService.java`
- `patient/application/ExternalAccessService.java`
- `patient/api/PatientPortalController.java`
- `patient/api/ExternalAccessController.java`
- `db/migration/V38__consents_external_access_cdc_alignment.sql` (nouveau)

### Frontend

- `web/src/app/patient/portal/components/patient-consents-list.component.ts`
- `web/src/app/patient/portal/pages/patient-consents-page.component.ts`
- `web/src/app/patient/portal/components/patient-requests-list.component.ts`
- `web/src/app/patient/portal/services/patient-portal.service.ts`
- `web/src/app/core/i18n/i18n.service.ts`

---

## 5. Tests attendus

- [ ] Backend : test de cycle de vie complet d’un consentement.
- [ ] Backend : test d’expiration automatique.
- [ ] Backend : test de révocation d’un accès externe approuvé.
- [ ] Backend : test d’OTP d’approbation.
- [ ] Frontend : test de l’interface de consentements.

## 6. Dépendances

- STORY-1901 pour la validation des scopes dans la synthèse.

---

## 7. Risques

- Remodelage important de `patient_consents` : migration des données existantes.
- Impact sur tous les contrôleurs utilisant `validateAccess()`.

---

## 8. Impact version / SemVer

- Bump : **MINOR** (0.10.0).

---

## 9. Reste à faire

1. **Backend** : générer un `PatientConsent` de type `TEMPORAIRE` dans `ExternalAccessService.approveRequest()` lors de l’approbation d’une demande d’accès externe (critère d’acceptation non satisfait).
2. **Backend** : ajouter les tests unitaires/d’intégration manquants (cycle de vie, expiration, révocation, OTP).
3. **Frontend** : ajouter les tests unitaires sur `patient-consents-list` et `patient-requests-list`.
4. **Review** : valider la cohérence entre les statuts `APPROVED`/`ACTIVE` et `APPROUVEE`/`REFUSEE` côté `external_access_requests`.
5. **Documentation** : mettre à jour `CHANGELOG.md` à la clôture du ticket.
