# STORY-1603 — API Backend : Dossier d'Urgences & Traçabilité Réanimation

| Champ | Valeur |
|---|---|
| **ID** | STORY-1603 |
| **Type** | User Story |
| **Epic** | EPIC-0016 |
| **Titre** | API Backend : Dossier d'Urgences & Traçabilité Réanimation |
| **Statut** | DONE |
| **Priorité** | P0 |
| **Stack** | Backend |
| **Profil recommandé** | Senior |
| **Sprint** | SPRINT-0011 |
| **Assigné** | Antigravity |
| **Reviewer** | Lead Developer |
| **Temps passé** | 0.5j |
| **Dernière MAJ** | 2026-07-08 |

---

## 1. Contexte & Objectif
Implémenter la logique d'enregistrement, de soins et de stabilisation pour le dossier d'urgence (Module 4-bis).

## 2. Actions réalisées
- [x] Création des DTOs `CreateEmergencyRequest`, `AddResuscitationLogRequest`, `EmergencyResponse` et `ResuscitationLogResponse`.
- [x] Implémentation du service métier `EmergencyService` :
  - `createEmergency` (crée une urgence active avec triage et constantes initiales, empêche les doublons).
  - `addResuscitationLog` (ajoute un soin horodaté à un dossier d'urgence actif).
  - `stabilizeEmergency` (clôture le dossier d'urgence et définit l'orientation post-urgence).
  - `getActiveEmergencies` (lister les urgences non stabilisées).
- [x] Implémentation des endpoints dans `EmergencyController` avec sécurité `@PreAuthorize` par rôle (seuls les médecins et admins peuvent stabiliser un patient).
- [x] Validation de la suite de tests unitaires/d'intégration Spring Boot avec succès (`BUILD SUCCESS`).

## 3. Reste à faire
- Aucun. STORY-1603 est terminée.
