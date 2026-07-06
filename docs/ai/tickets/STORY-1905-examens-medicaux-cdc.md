# STORY-1905 — Alignement Module 8 : Examens médicaux

| Champ | Valeur |
|---|---|
| **ID** | STORY-1905 |
| **Epic** | EPIC-0014 |
| **Type** | User Story |
| **Titre** | Alignement Module 8 — Examens médicaux conformes CDC |
| **Statut** | COMPLETED |
| **Priorité** | P0 |
| **Stack** | Full-stack |
| **Profil recommandé** | Intermédiaire Backend + Frontend |
| **Estimation Senior** | 1.0j |
| **Estimation Intermédiaire** | 1.5j |
| **Estimation Junior** | 2.5j |
| **Sprint cible** | SPRINT-0011 |
| **Assigné** | Antigravity |
| **Reviewer** | Lead Developer |
| **Dernière MAJ** | 2026-07-05 |

---

## 1. Contexte

Le CDC définit un cycle de vie complet des demandes d’examens avec des types contrôlés, des statuts incluant le paiement, et une ségrégation des droits entre établissement demandeur et laboratoire cible. Le modèle actuel est un squelette fonctionnel mais incomplet.

---

## 2. Critères d’acceptation

### Backend

- [x] `exam_type` est une enum contrainte (`LABORATOIRE`, `IMAGERIE`, `CARDIOLOGIE`, `ORL`, `OPHTALMOLOGIE`, `AUTRE`).
- [x] Les statuts incluent `AWAITING_PAYMENT` et `PAID`.
- [x] `source_organization_id` est une colonne dédiée (distincte de `organization_id` qui reste le tenant).
- [x] La liste des examens demandés est stockée dans une table fille `lab_order_items` (pas de CSV).
- [x] Seul le laboratoire cible (`target_organization_id`) peut modifier le statut d’une demande qui lui est destinée.
- [x] Le patient peut voir qu’un résultat est disponible (notification + statut `RESULT_AVAILABLE`).

### Frontend

- [x] Formulaire de demande d’examen avec type contrôlé et liste dynamique d’examens.
- [x] Affichage du statut avec workflow de paiement (si applicable).
- [x] Internationalisation FR/EN.

---

## 3. Tâches techniques

### Backend

1. [x] Migration V34 (Java) : créer `lab_order_items`, ajouter `source_organization_id`, ajouter les nouveaux statuts, migrer les anciennes données.
2. [x] Créer `LabOrderItemEntity`.
3. [x] Remplacer le champ CSV `exams` par la relation 1-N.
4. [x] Créer enum `LabOrderStatus` et `ExamType`.
5. [x] Sécuriser `LabOrderService.updateStatus()` avec vérification du `target_organization_id`.
6. [x] Tests unitaires et d'intégration MockMvc.

### Frontend

1. [x] Modifier le formulaire de demande d’examen dans `consultation.component.ts`.
2. [x] Mettre à jour `lab.models.ts`.
3. [x] Mettre à jour `lab-orders-page.component.ts` pour afficher les items et inclure les statuts de paiement.
4. [x] Tests unitaires Vitest.

---

## 4. Fichiers impactés

### Backend

- `lab/infrastructure/persistence/LabOrderEntity.java`
- `lab/infrastructure/persistence/LabOrderItemEntity.java` (nouveau)
- `lab/infrastructure/persistence/LabOrderRepository.java`
- `lab/application/LabOrderService.java`
- `lab/api/LabOrderController.java`
- `lab/api/CreateLabOrderRequest.java`
- `lab/api/LabOrderResponse.java`
- `db/migration/V34__lab_orders_cdc_alignment.java` (nouveau, Java-based)

### Frontend

- `web/src/app/consultation/consultation.component.ts`
- `web/src/app/clinic/lab/lab.models.ts`
- `web/src/app/clinic/lab/lab-orders-page.component.ts`
- `web/src/app/core/i18n/i18n.service.ts`

---

## 5. Tests attendus

- [x] Backend : test de création avec items.
- [x] Backend : test de sécurité — biologiste d’un autre labo ne peut pas modifier le statut.
- [x] Backend : test du workflow de paiement.
- [x] Frontend : test du formulaire de demande.

---

## 6. Dépendances

- Aucune directe.

---

## 7. Risques

- Migration des données CSV existantes vers la table fille (sécurisée par la migration Java robustement implémentée).

---

## 8. Impact version / SemVer

- Bump : **MINOR** (0.10.0).
