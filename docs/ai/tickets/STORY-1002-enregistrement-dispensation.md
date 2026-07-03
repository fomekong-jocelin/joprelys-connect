# STORY-1002 — Enregistrement de dispensation

> Fichier obligatoire pour le ticket de la User Story STORY-1002.

## 1. Objectif

Permettre au pharmacien d'enregistrer la dispensation d'une ordonnance en indiquant les quantités de médicaments délivrées et d'éventuelles substitutions par des génériques.

## 2. Critères d'acceptation

- [ ] **Validation de la délivrance** :
  - Endpoint `POST /api/public/pharmacy/prescriptions/dispense` accessible publiquement (authentifié par couple code prescription et code PIN).
  - La requête doit contenir `prescriptionNumber`, `pinCode`, `pharmacyName`, `pharmacistLicense`, et `dispensedItems` (liste contenant `prescriptionItemId`, `quantityDispensed` et `substitutedWith`).
  - Le système doit vérifier que pour chaque médicament de la requête, la quantité dispensée cumulée (dispensations passées + nouvelle dispensation) ne dépasse pas la quantité prescrite initiale.
  - Mettre à jour l'ordonnance en fonction du statut calculé :
    - `FULLY_DISPENSED` : Si toutes les lignes de médicaments prescrites ont été totalement dispensées.
    - `PARTIALLY_DISPENSED` : Si au moins une ligne de médicament a été partiellement ou totalement dispensée mais qu'il reste des quantités non servies.
  - Enregistrer la dispensation dans les tables `prescription_dispensations` et `dispensation_items`.
- [ ] **Interdiction de double dispensation** :
  - Si le statut de l'ordonnance est déjà `FULLY_DISPENSED`, renvoyer une erreur `400 Bad Request` et bloquer l'action.
- [ ] **Audit et Traçabilité** :
  - Générer un log d'audit `PHARMACY_DISPENSED` dans la table `audit_logs`.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0010 — Dispensation en Pharmacie & Gestion des Prescriptions |
| User story parent | STORY-1002 |
| Sprint cible | SPRINT-0004 |
| Priorité business | P1 |
| Complexité | L |
| Story points | 3 |
| Profil recommandé | Senior |
| Effort estimé senior | 0.8j |
| Effort estimé intermédiaire | 1.1j |
| Effort estimé junior | 1.8j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead |
| Risque fonctionnel | Fort |
| Risque technique | Moyen |
| Dépendances | STORY-1001 |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu

## 5. Hypothèses

- La dispensation peut se faire en plusieurs fois (dispensation partielle) tant que l'ordonnance est active et non expirée.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Sur-délivrance de médicaments | Fort | Contrôle strict de la somme des quantités dispensées par ligne. |

## 7. Action plan

- [ ] **Modèle et Logique métier** :
  - Créer `PrescriptionDispensationEntity` et `DispensationItemEntity`.
  - Implémenter la méthode métier `dispense(...)` dans `PharmacyService`.
- [ ] **Controller** : Ajouter l'endpoint `/dispense` dans `PharmacyController`.
- [ ] **Tests** :
  - Écrire des cas de tests d'intégration vérifiant la dispensation totale, la dispensation partielle, le passage de statut `ACTIVE` -> `PARTIALLY_DISPENSED` -> `FULLY_DISPENSED`, et le blocage si déjà `FULLY_DISPENSED`.
- [ ] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`.

## 8. Implémentation réalisée

*(À remplir lors de la réalisation)*

## 9. Suivi d'exécution

*(À remplir lors de la réalisation)*

## 10. Tests et vérifications

*(À remplir lors de la réalisation)*

## 11. Documentation

*(À remplir lors de la réalisation)*

## 12. Reste à faire

*(À remplir lors de la réalisation)*

## 13. Statut final

Statut : TODO

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout d'une fonctionnalité métier majeure de dispensation. |
| Breaking change | Non |
| Migration DB | Oui |
| Changement API | Oui |
| Impact Angular | Non |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Oui |
