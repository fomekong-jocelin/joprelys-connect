# TICKET-UI-DPU-PATIENT-TABLE-READABILITY — Lisibilité du tableau patients DPU

## 1. Objectif

Corriger l'affichage desktop du tableau des patients afin d'eviter les retours a la ligne disgracieux sur les numeros DPU, numeros locaux et actions.

## 2. Mode d'intervention

| Champ | Valeur |
|---|---|
| Mode | Engineering |
| Type | UI/UX |
| Stack | Angular |
| Epic parent | EPIC-0003 |
| Story rattachee | STORY-0301 |
| Priorite | P2 |
| Sprint | SPRINT-0004 |
| Profil recommande | Intermediaire |
| Estimation | 0.05j |
| Reviewer | Lead Developer |

## 3. Criteres d'acceptation

- [x] Les numeros DPU nationaux restent sur une seule ligne en vue desktop.
- [x] Les numeros locaux restent sur une seule ligne en vue desktop.
- [x] L'action de consultation du dossier reste sur une seule ligne.
- [x] Le tableau conserve un comportement responsive avec defilement horizontal si la largeur disponible est insuffisante.
- [x] Aucun contrat API, modele de donnees ou regle metier n'est modifie.

## 4. Analyse d'impact

| Axe | Impact |
|---|---|
| Backend | Aucun |
| API | Aucun |
| Base de donnees | Aucun |
| Angular | Template du tableau patient uniquement |
| Flutter | Aucun |
| Securite | Aucun changement AuthN/AuthZ ou donnees |
| Configuration | Aucun |
| SemVer | PATCH si livre |

## 5. Action plan

- [x] Lire les consignes IA, standards UI, design system, configuration, architecture et SemVer.
- [x] Identifier le composant Angular impacte.
- [x] Comparer avec le tableau des cliniques pilotes.
- [x] Modifier le tableau desktop patient pour stabiliser les colonnes et eviter les retours ligne.
- [x] Executer les verifications Angular pertinentes.
- [x] Mettre a jour la documentation de suivi.

## 6. Tests attendus

- `npm run build` dans `web`.
- `npm test` dans `web` si le temps d'execution local le permet.

## 7. Risques

| Risque | Niveau | Mitigation |
|---|---|---|
| Debordement sur ecrans moyens | Faible | Conserver `overflow-x-auto` et ajouter une largeur minimale coherente. |
| Regression mobile | Faible | Ne pas modifier la vue cartes mobile. |

## 8. Suivi d'execution

| Date | Intervenant | Temps passe | Avancement | Reste a faire | Blocage |
|---|---|---:|---:|---|---|
| 2026-07-03 | Codex | 0.00j | 20% | Implementation et verification | Aucun |
| 2026-07-03 | Codex | 0.05j | 100% | Aucun | Build production bloque par acces reseau aux polices Google |

## 9. Statut

Statut : DONE

## 10. Implementation realisee

- Ajout d'une largeur minimale `min-w-[1060px]` sur le tableau desktop patient.
- Ajout d'un `colgroup` pour stabiliser les largeurs de colonnes.
- Ajout de `whitespace-nowrap` sur les identifiants DPU/local, le sexe, le telephone et l'action.
- Reduction visuelle des identifiants avec une typographie monospace `text-sm` pour une lecture plus dense.
- Conservation de la vue mobile en cartes sans modification.

## 11. Tests et verifications

- `npm test -- --watch=false` dans `web` : OK, 11 fichiers de tests, 39 tests passes.
- `npm run build -- --configuration development` dans `web` : OK.
- `npm run build` dans `web` : KO environnemental, echec d'inlining de `https://fonts.googleapis.com/...` par acces reseau restreint (`connect EACCES`). Aucune erreur de template Angular remontee avant ce blocage.

## 12. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Correction UI retrocompatible sans changement fonctionnel, API ou donnees. |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Oui |
| Impact Flutter | Non |
