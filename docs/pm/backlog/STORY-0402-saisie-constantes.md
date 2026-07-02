# STORY-0402 — Saisie des Constantes Vitales & Calcul IMC

## 1. User story

En tant qu'**infirmier ou médecin**, je veux **saisir les constantes vitales d'un patient en visite active**, afin de **préparer la consultation et disposer d'un IMC calculé automatiquement**.

## 2. Critères d'acceptation

- [x] La file d'attente active affiche les patients après ouverture d'une visite depuis la fiche patient.
- [x] L'utilisateur peut saisir les constantes depuis l'action `Saisir constantes` d'une visite `EN_COURS`.
- [x] Les bornes de validation des constantes sont appliquées côté API.
- [x] L'IMC est calculé automatiquement côté frontend et côté backend.
- [x] Les constantes sauvegardées s'affichent immédiatement dans la file d'attente après rechargement.
- [x] L'isolation multi-tenant est respectée via la visite rattachée à l'organisation.

## 3. Tâches

| ID | Titre | Stack | Profil recommandé | SP | Estimation | Statut |
|---|---|---|---|---:|---:|---|
| TASK-0402-01 | Migration SQL `vitals` | DB / SQL | Intermédiaire | 1 | 0.15j | DONE |
| TASK-0402-02 | API REST constantes et calcul IMC | Backend | Intermédiaire | 1 | 0.35j | DONE |
| TASK-0402-03 | Formulaire Angular de saisie des constantes | Frontend | Intermédiaire | 1 | 0.25j | DONE |
| TASK-0402-04 | Correction file d'attente active et fetch des constantes | Full-stack | Intermédiaire | 1 | 0.15j | DONE |

## 4. Definition of Done

- [x] Tests backend `VisitControllerTest` OK.
- [x] Tests frontend dashboard OK.
- [x] Build Angular production OK.
- [x] Documentation fonctionnelle et technique mise à jour.
- [x] Changelog et tracking mis à jour.

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout d'un flux fonctionnel de constantes vitales dans le module visite |
| Breaking change | Non |
| Release cible | v0.4.0 |
