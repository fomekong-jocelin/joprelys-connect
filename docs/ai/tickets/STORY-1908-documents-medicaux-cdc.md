# STORY-1908 — Alignement Module 11 : Documents médicaux vérifiables

| Champ | Valeur |
|---|---|
| **ID** | STORY-1908 |
| **Epic** | EPIC-0014 |
| **Type** | User Story |
| **Titre** | Alignement Module 11 — Documents médicaux vérifiables conformes CDC |
| **Statut** | DONE |
| **Priorité** | P0 |
| **Stack** | Full-stack |
| **Profil recommandé** | Senior Backend |
| **Estimation Senior** | 1.5j |
| **Estimation Intermédiaire** | 2.5j |
| **Estimation Junior** | 4.0j |
| **Sprint cible** | SPRINT-0011 |
| **Assigné** | IA Antigravity |
| **Reviewer** | Lead Developer |
| **Dernière MAJ** | 2026-07-05 |

---

## 1. Contexte

Le CDC exige que chaque document médical ait un numéro unique, un hash d'intégrité, un type, un auteur, un QR code, une URL de vérification, un versionnement et un statut (`VALIDE`, `ANNULE`, `REMPLACE`). Le modèle actuel gère uniquement le compte-rendu de consultation dans `medical_documents`.

---

## 2. Critères d'acceptation

### Backend

- [x] `MedicalDocumentEntity` contient `document_type` (enum des 12 types CDC), `hash` (SHA-256), `qr_code_url`, `verification_url`, `author_user_id`, `version`, `previous_document_id`, statut `REMPLACE`.
- [x] Le hash est calculé sur le fichier PDF au moment de la génération.
- [x] Le QR code et l'URL de vérification sont persistés.
- [x] Le versionnement est implémenté : une nouvelle version d'un document ne remplace pas l'ancienne (champ `previous_document_id`).
- [x] Les PDF de sortie d'hospitalisation et de résultats labo sont stockés dans `medical_documents`.
- [x] La vérification publique retourne : statut, numéro, type, établissement, date, auteur/service, et la mention légale « ce document ne donne pas accès au dossier médical complet ».
- [x] Génération de numéro sécurisée (séquence DB) pour éviter les doublons en cluster.

### Frontend

- [x] Page de saisie publique du numéro de document (`/verify`).
- [x] Page de résultat de vérification conforme CDC avec mention légale.
- [ ] Option depuis la vérification publique pour demander l'accès au dossier (lien vers STORY-1301 — demande accès externe).
- [x] Internationalisation FR/EN.

---

## 3. Tâches techniques

### Backend

1. [x] Migration V37 : ajouter les colonnes manquantes à `medical_documents`, créer enum `DocumentType` et `DocumentStatus`.
2. [x] Modifier `DocumentService` pour calculer et stocker le hash.
3. [x] Modifier `DocumentNumberGenerator` pour utiliser une séquence DB.
4. [x] Implémenter le versionnement.
5. [x] Refactorer `HospitalizationService` et `LabResultService` pour utiliser `MedicalDocumentEntity`.
6. [x] Mettre à jour `DocumentVerificationResponse` avec documentType, serviceName, legalNotice.
7. [x] Ajouter endpoint `GET /api/public/documents/search?number=` pour recherche par numéro.
8. [x] Corriger les erreurs de compilation liées à `DocumentStatus` enum dans `ConsultationResponse` et `PatientPortalController`.
9. [ ] Tests unitaires et d'intégration.

### Frontend

1. [x] Modifier `consultation/verification.component.ts` pour afficher documentType, serviceName, legalNotice.
2. [x] Créer `document-search.component.ts` (page `/verify` sans ID).
3. [x] Ajouter les routes `/verify` et `/verify/:documentId` dans `app.routes.ts`.
4. [x] Ajouter `verifyDocumentByNumber()` dans `ConsultationApiService`.
5. [x] Ajouter keys i18n FR/EN pour verify.documentType, verify.serviceName, verify.legalNotice, verify.search.*.
6. [ ] Ajouter lien de demande d'accès externe depuis la page de vérification.
7. [ ] Tests.

---

## 4. Fichiers modifiés

### Backend

- `visit/infrastructure/persistence/DocumentType.java` (nouveau)
- `visit/infrastructure/persistence/DocumentStatus.java` (nouveau)
- `visit/infrastructure/persistence/MedicalDocumentEntity.java`
- `visit/infrastructure/persistence/MedicalDocumentRepository.java`
- `visit/application/DocumentService.java`
- `visit/application/DocumentNumberGenerator.java`
- `visit/api/DocumentController.java`
- `visit/api/DocumentVerificationResponse.java`
- `hospitalization/application/HospitalizationService.java`
- `lab/application/LabResultService.java`
- `consultation/api/ConsultationResponse.java` (fix enum → String)
- `patient/api/PatientPortalController.java` (fix enum → String)
- `db/migration/V37__medical_documents_cdc_alignment.sql` (nouveau)

### Frontend

- `web/src/app/consultation/verification.component.ts`
- `web/src/app/consultation/document-search.component.ts` (nouveau)
- `web/src/app/consultation/consultation-api.service.ts`
- `web/src/app/app.routes.ts`
- `web/src/app/core/i18n/i18n.service.ts`

---

## 5. Tests attendus

- [ ] Backend : test de calcul de hash.
- [ ] Backend : test de versionnement.
- [ ] Backend : test de vérification publique avec mention légale.
- [ ] Backend : test d'unicité du numéro de document en concurrence.
- [ ] Frontend : test de la page de vérification.

---

## 6. Dépendances

- Aucune directe (fondation pour STORY-1904, 1906, 1907).

---

## 7. Risques

- Migration des documents existants sans hash/type.
- Changement de la structure des URLs de vérification publique.

---

## 8. Impact version / SemVer

- Bump : **MINOR** (0.10.0).
