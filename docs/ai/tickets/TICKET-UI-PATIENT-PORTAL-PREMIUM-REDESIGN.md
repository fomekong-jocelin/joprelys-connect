# TICKET-UI-PATIENT-PORTAL-PREMIUM-REDESIGN — Amelioration premium du portail patient

## 1. Objectif

Ameliorer l'interface `/patient/dashboard` pour atteindre un rendu plus premium, plus lisible et plus coherent avec `DESIGN.md`, sans modifier la logique metier, les services Angular, les contrats API ni les donnees.

## 2. Mode d'intervention

| Champ | Valeur |
|---|---|
| Mode | Engineering |
| Type | UI/UX |
| Stack | Angular |
| Epic parent | EPIC-0008 |
| Story rattachee | STORY-0801 |
| Priorite | P1 |
| Sprint | SPRINT-0004 |
| Profil recommande | Intermediaire |
| Estimation | 0.15j |
| Reviewer | Lead Developer |

## 3. Criteres d'acceptation

- [x] L'ecran utilise mieux la largeur desktop tout en conservant le responsive mobile.
- [x] Le DPU patient reste lisible et ne casse pas la mise en page.
- [x] Les consultations sont plus compactes, mieux hierarchisees et plus faciles a scanner.
- [x] Les emojis visibles dans les donnees medicales sont retires au profit d'un style sobre.
- [x] Le nombre de couleurs secondaires est reduit pour limiter la fatigue visuelle.
- [x] Les arrondis restent sobres, avec un maximum de 8px hors avatar.
- [x] Aucune logique metier, service Angular, endpoint REST ou modele de donnees n'est modifie.

## 4. Analyse d'impact

| Axe | Impact |
|---|---|
| Backend | Aucun |
| API | Aucun |
| Base de donnees | Aucun |
| Angular | Templates des composants du portail patient et style global mineur |
| Flutter | Aucun |
| Securite | Aucun changement AuthN/AuthZ ou stockage |
| Configuration | Aucun |
| SemVer | PATCH si livre |

## 5. Action plan

- [x] Lire les standards projet, `DESIGN.md`, Documentation First, SemVer et regles UI.
- [x] Analyser les composants patient portal existants.
- [x] Creer le ticket et mettre a jour la documentation fonctionnelle/technique.
- [x] Ajouter un conteneur large reutilisable conforme au design system.
- [x] Ameliorer le layout du dashboard patient sans changer la logique.
- [x] Ameliorer la fiche patient et la lisibilite du DPU.
- [x] Ameliorer l'accordeon des consultations, retirer les emojis et reduire les couleurs.
- [x] Executer les tests Angular et le build de validation possible.
- [x] Mettre a jour le changelog et le suivi projet.

## 6. Tests attendus

- `npm test -- --watch=false` dans `web`.
- `npm run build -- --configuration development` dans `web`.
- `npm run build` si l'environnement autorise l'acces aux polices Google.

## 7. Risques

| Risque | Niveau | Mitigation |
|---|---|---|
| Regression visuelle mobile | Moyen | Conserver le layout mobile en pile et tester la compilation Angular. |
| Depassement taille composant | Faible | Limiter l'intervention aux templates et garder les fichiers sous 300 lignes. |
| Accessibilite des onglets | Faible | Convertir les cartes d'onglets en boutons. |

## 8. Statut

Statut : DONE

## 8.1 Implementation realisee

- Ajout de `app-container-wide`, tokens `--radius-brand-*` et `ui-card-subtle` dans le design system CSS global.
- Recomposition de `PatientDashboardComponent` avec conteneur large, en-tete plus sobre, bannière patient plus lisible et onglets transformes en boutons accessibles.
- Reprise de `PatientProfileCardComponent` pour rendre le DPU lisible sur une ligne scrollable et clarifier les informations d'identite.
- Reprise de `PatientVisitsListComponent` pour retirer les emojis, compacter les consultations, limiter les couleurs et rendre le bouton PDF moins dominant.
- Ajout des cles i18n FR/EN necessaires aux nouveaux libelles du portail patient.
- Mise a jour du test frontend pour cibler explicitement le bouton de telechargement PDF.

## 8.2 Tests et verifications

- `npm test -- --watch=false` dans `web` : OK, 11 fichiers de tests, 39 tests passes.
- `npm run build -- --configuration development` dans `web` : OK.
- `npm run build` dans `web` : KO environnemental, inlining impossible de `https://fonts.googleapis.com/...` par acces reseau restreint (`connect EACCES`). Aucune erreur Angular propre aux changements n'est remontee avant ce blocage.

## 9. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Amelioration UI retrocompatible sans changement fonctionnel, API, securite ou donnees. |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Oui |
| Impact Flutter | Non |
