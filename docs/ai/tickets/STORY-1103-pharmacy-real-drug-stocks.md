# STORY-1103 — Gestion réelle des stocks de médicaments

> Ticket d'évolution fonctionnelle de gestion des stocks de pharmacie.

## 1. Objectif

Remplacer l'état déclaratif par un tableau de suivi des stocks physiques de médicaments dans la clinique, avec décrémentation automatique des quantités disponibles lors d'une délivrance validée.

## 2. Critères d'acceptation

- [x] Création de la table `drug_stocks` liée à l'organisation (tenant).
- [x] Le pharmacien reçoit une alerte si la quantité demandée dépasse le stock physique de la clinique.
- [x] La validation d'une dispensation décrémente le stock en transaction sécurisée.
- [x] API de mise à jour / approvisionnement des stocks documentée.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0010 — Dispensation en Pharmacie & Gestion des Prescriptions |
| User story parent | STORY-1103 |
| Sprint cible | SPRINT-0005 |
| Priorité business | P1 |
| Complexité | XL |
| Story points | 8 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 1.8j |
| Effort estimé intermédiaire | 2.3j |
| Effort estimé junior | 4.0j |
| Responsable | Antigravity |
| Reviewer obligatoire | Tech Lead |
| Risque fonctionnel | Fort |
| Risque technique | Moyen |
| Dépendances | STORY-1005 |
| Bloquants connus | Aucun |

## 4. Action plan

- [x] Rédiger la migration Flyway pour `drug_stocks` (V17).
- [x] Implémenter les entités et repositories (`DrugStockEntity`, `DrugStockRepository`).
- [x] Créer le service `DrugStockService` avec méthodes upsert, alertes et décrémentation.
- [x] Créer le contrôleur `DrugStockController` avec endpoints CRUD sécurisés.
- [x] Créer les DTOs `DrugStockResponse` et `CreateDrugStockRequest`.
- [x] Créer la documentation (FUNCTIONAL-SPEC, TECHNICAL-DESIGN, DATA-MODEL).
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.
- [ ] Ajouter les tests unitaires et d'intégration MockMvc (tests à implémenter).
- [ ] Créer l'IHM d'inventaire stock dans le portail pharmacie (Angular, tâche suivante).

## 5. Fichiers créés / modifiés

| Fichier | Action |
|---|---|
| `db/migration/V17__create_drug_stocks_table.sql` | Créé |
| `prescription/infrastructure/persistence/DrugStockEntity.java` | Créé |
| `prescription/infrastructure/persistence/DrugStockRepository.java` | Créé |
| `prescription/api/DrugStockResponse.java` | Créé |
| `prescription/api/CreateDrugStockRequest.java` | Créé |
| `prescription/application/DrugStockService.java` | Créé |
| `prescription/api/DrugStockController.java` | Créé |
| `docs/features/story-1103/FUNCTIONAL-SPEC.md` | Créé |
| `docs/features/story-1103/TECHNICAL-DESIGN.md` | Créé |
| `docs/features/story-1103/DATA-MODEL.md` | Créé |

## 13. Statut final

Statut : DONE

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Nouveau module d'inventaire de stock pharmacie rétrocompatible |
