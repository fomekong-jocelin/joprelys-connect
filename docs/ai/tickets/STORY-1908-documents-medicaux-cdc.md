# STORY-1908 — Alignement Module 11 : Documents médicaux vérifiables

| Champ | Valeur |
|---|---|
| **ID** | STORY-1908 |
| **Epic** | EPIC-0014 |
| **Type** | User Story |
| **Titre** | Alignement Module 11 — Documents médicaux vérifiables conformes CDC |
| **Statut** | READY |
| **Priorité** | P0 |
| **Stack** | Full-stack |
| **Profil recommandé** | Senior Backend |
| **Estimation Senior** | 1.5j |
| **Estimation Intermédiaire** | 2.5j |
| **Estimation Junior** | 4.0j |
| **Sprint cible** | SPRINT-0011 |
| **Assigné** | À assigner |
| **Reviewer** | Lead Developer |
| **Dernière MAJ** | 2026-07-05 |

---

## 1. Contexte

Le CDC exige que chaque document médical ait un numéro unique, un hash d’intégrité, un type, un auteur, un QR code, une URL de vérification, un versionnement et un statut (`VALIDE`, `ANNULE`, `REMPLACE`). Le modèle actuel gère uniquement le compte-rendu de consultation dans `medical_documents`.

---

## 2. Critères d’acceptation

### Backend

- [ ] `MedicalDocumentEntity` contient `document_type` (enum des 12 types CDC), `hash` (SHA-256), `qr_code_url`, `verification_url`, `author_user_id`, `version`, `previous_document_id`, statut `REMPLACE`.
- [ ] Le hash est calculé sur le fichier PDF au moment de la génération.
- [ ] Le QR code et l’URL de vérification sont persistés.
- [ ] Le versionnement est implémenté : une nouvelle version d’un document ne remplace pas l’ancienne (champ `previous_document_id`).
- [ ] Les PDF de sortie d’hospitalisation et de résultats labo sont stockés dans `medical_documents`.
- [ ] La vérification publique retourne : statut, numéro, type, établissement, date, auteur/service, et la mention légale « ce document ne donne pas accès au dossier médical complet ».
- [ ] Génération de numéro sécurisée (séquence DB ou UUID) pour éviter les doublons en cluster.

### Frontend

- [ ] Page de saisie publique du numéro de document (`/verify`).
- [ ] Page de résultat de vérification conforme CDC avec mention légale.
- [ ] Option depuis la vérification publique pour demander l’accès au dossier.
- [ ] Internationalisation FR/EN.

---

## 3. Tâches techniques

### Backend

1. Migration V37 : ajouter les colonnes manquantes à `medical_documents`, créer enum `DocumentType` et `DocumentStatus`.
2. Modifier `DocumentService` pour calculer et stocker le hash.
3. Modifier `DocumentNumberGenerator` pour utiliser une séquence DB.
4. Implémenter le versionnement.
5. Refactorer `HospitalizationService` et `LabResultService` pour utiliser `MedicalDocumentEntity`.
6. Mettre à jour `DocumentVerificationResponse`.
7. Tests.

### Frontend

1. Modifier `consultation/verification.component.ts` pour afficher le type et la mention légale.
2. Créer une page de saisie du numéro document.
3. Ajouter le lien vers la demande d’accès.
4. Tests.

---

## 4. Fichiers impactés

### Backend

- `visit/infrastructure/persistence/MedicalDocumentEntity.java`
- `visit/application/DocumentService.java`
- `visit/application/DocumentNumberGenerator.java`
- `visit/api/DocumentController.java`
- `visit/api/DocumentVerificationResponse.java`
- `hospitalization/application/HospitalizationService.java`
- `lab/application/LabResultService.java`
- `db/migration/V37__medical_documents_cdc_alignment.sql` (nouveau)

### Frontend

- `web/src/app/consultation/verification.component.ts`
- `web/src/app/consultation/consultation-api.service.ts`
- `web/src/app/app.routes.ts`
- `web/src/app/core/i18n/i18n.service.ts`

---

## 5. Tests attendus

- [ ] Backend : test de calcul de hash.
- [ ] Backend : test de versionnement.
- [ ] Backend : test de vérification publique avec mention légale.
- [ ] Backend : test d’unicité du numéro de document en concurrence.
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
