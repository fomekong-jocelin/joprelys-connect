# STORY-1904 — Alignement Module 7 : Prescriptions et ordonnances

| Champ | Valeur |
|---|---|
| **ID** | STORY-1904 |
| **Epic** | EPIC-0014 |
| **Type** | User Story |
| **Titre** | Alignement Module 7 — Prescriptions et ordonnances conformes CDC |
| **Statut** | DONE |
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

Le CDC définit un modèle complet d’ordonnance : numéro unique, statuts (`DRAFT`, `ACTIVE`, `PARTIALLY_DISPENSED`, `FULLY_DISPENSED`, `EXPIRED`, `CANCELLED`), champs détaillés du médicament (`form`, `route`, `frequency`, `duration`, `quantity`, `instructions`, `substitution_allowed`), QR code vérifiable, et transmission AllôPharma. L’implémentation actuelle est fonctionnelle mais incomplète.

---

## 2. Critères d’acceptation

### Backend

- [x] `PrescriptionItemEntity` contient `form`, `route`, `frequency`, `substitution_allowed`.
- [x] `PrescriptionEntity` contient `issued_at`, `visit_id`, `document_id` (lien vers `medical_documents`).
- [x] Statut `DRAFT` supporté ; endpoint prescripteur pour basculer de `DRAFT` à `ACTIVE`.
- [x] Endpoint prescripteur `PATCH /api/prescriptions/{id}/cancel` pour annuler une ordonnance.
- [x] Job planifié pour passer les prescriptions `expires_at` au statut `EXPIRED`.
- [x] Génération d’un PDF d’ordonnance dédié avec QR code et hash (via `MedicalDocumentEntity` avec `document_type = ORDONNANCE`).
- [x] Vérification publique par QR code ou numéro + PIN.
- [x] `substitution_allowed` configurable par ligne.

### Frontend

- [x] Formulaire de prescription avec les nouveaux champs médicament.
- [x] Bouton "Enregistrer comme brouillon" et "Valider l’ordonnance".
- [x] Bouton "Annuler l’ordonnance" pour le prescripteur.
- [x] Affichage du QR code / numéro d’ordonnance.
- [x] Internationalisation FR/EN.

---

## 3. Tâches techniques

### Backend

1. Migration V33 : ajouter les colonnes manquantes à `prescriptions` et `prescription_items`.
2. Créer `PrescriptionDocumentService` pour générer le PDF ordonnance.
3. Ajouter `document_type` dans `MedicalDocumentEntity`.
4. Implémenter cycle de vie DRAFT → ACTIVE → EXPIRED/CANCELLED.
5. Scheduler Spring Boot pour l’expiration.
6. Mettre à jour `AlloPharmaClient` derrière une interface configurable (URL, credentials, retry).
7. Tests.

### Frontend

1. Modifier `consultation.component.ts` pour extraire le formulaire de prescription dans un sous-composant.
2. Ajouter les champs médicament.
3. Ajouter les boutons brouillon / valider / annuler.
4. Afficher QR code / numéro.
5. Tests.

---

## 4. Fichiers impactés

### Backend

- `prescription/infrastructure/persistence/PrescriptionEntity.java`
- `prescription/infrastructure/persistence/PrescriptionItemEntity.java`
- `prescription/application/PrescriptionService.java`
- `prescription/api/PrescriptionController.java`
- `visit/infrastructure/persistence/MedicalDocumentEntity.java`
- `visit/application/DocumentService.java`
- `prescription/application/AlloPharmaClient.java`
- `db/migration/V33__prescriptions_cdc_alignment.sql` (nouveau)

### Frontend

- `web/src/app/consultation/consultation.component.ts`
- `web/src/app/consultation/consultation.models.ts`
- `web/src/app/consultation/consultation-api.service.ts`
- `web/src/app/pharmacy/pharmacy.models.ts`
- `web/src/app/core/i18n/i18n.service.ts`

---

## 5. Tests attendus

- [x] Backend : création ordonnance en DRAFT puis activation.
- [x] Backend : annulation prescripteur.
- [x] Backend : expiration automatique.
- [x] Backend : génération PDF ordonnance avec hash.
- [x] Frontend : test du formulaire de prescription.

---

## 6. Dépendances

- STORY-1908 (Documents médicaux) pour le `document_type` et le hash.

---

## 7. Risques

- Changement du modèle de prescription : impact sur la pharmacie.
- AllôPharma : si l’intégration réelle n’est pas disponible, rester sur simulation documentée.

---

## 8. Impact version / SemVer

- Bump : **MINOR** (0.10.0).
