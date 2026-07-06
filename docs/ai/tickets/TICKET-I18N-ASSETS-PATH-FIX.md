# TICKET-I18N-ASSETS-PATH-FIX — Correction du chemin des assets de traduction Angular

> Fichier obligatoire pour chaque ticket ou intervention IA.

## 1. Objectif

Corriger le problème de chargement des traductions dans l'application web Angular. Actuellement, les fichiers de traduction réels (54KB) de `web/src/assets/i18n/` sont copiés à la racine du build (`/i18n/`) en raison d'une mauvaise configuration dans `angular.json` (absence d'attribut `output: "assets"`). En parallèle, des fichiers de traduction partiels fictifs (175/192 octets) sous `web/public/assets/i18n` prenaient le dessus sur le chemin `/assets/i18n/`, bloquant le chargement des autres clés.

## 2. Critères d'acceptation

- [x] Supprimer les fichiers doublons et partiels obsolètes `web/public/assets/i18n/en.json` et `web/public/assets/i18n/fr.json`.
- [x] Mettre à jour la configuration de `web/angular.json` pour copier le contenu de `src/assets` dans le dossier `/assets` de destination (avec `"output": "assets"`).
- [x] Vérifier que les tests unitaires Angular et le build de production passent.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0008 |
| User story parent | TICKET-I18N-PATIENT-PORTAL-TRANSLATIONS-AUDIT |
| Sprint cible | SPRINT-0011 |
| Priorité business | P0 |
| Complexité | XS |
| Story points | 1 |
| Profil recommandé | Intermédiaire / Senior |
| Effort estimé senior | 0.05j |
| Effort estimé intermédiaire | 0.1j |
| Effort estimé junior | 0.2j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé
- [x] Tests existants analysés
- [x] Impacts sécurité analysés
- [x] Impacts données analysés
- [x] Impacts Angular analysés si applicable
- [x] Frontend Tailwind CSS v4 vérifié si applicable
- [x] Absence Angular Material vérifiée si applicable
- [x] Angular `proxy.conf.json` présent et référencé dans `angular.json` si applicable

## 5. Hypothèses

- Supprimer les fichiers doublons de `public/assets/i18n` et réorienter les vrais assets de `src/assets` vers `assets` résoudra définitivement l'anomalie de traduction sans casser les pages existantes.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Perte de clés définies uniquement dans `public/assets/i18n` | Faible | Les 3 clés de `public/assets/i18n` existent déjà dans les fichiers globaux de `src/assets/i18n`. |

## 7. Action plan

- [x] Supprimer les fichiers doublons dans `web/public/assets/i18n/`
- [x] Modifier `web/angular.json` pour ajouter `"output": "assets"` à la source `src/assets`
- [x] Lancer les vérifications (tests unitaires Angular et build)
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`

## 8. Implémentation réalisée

- [x] Suppression des fichiers partiels dans `web/public/assets/i18n`
- [x] Ajout de `"output": "assets"` dans `web/angular.json`
- [x] Lancement des tests unitaires et vérification de la compilation

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-06 | Antigravity | 0.05j | 100% | Aucun | Aucun | Résolution de la copie des assets de traduction et suppression des doublons. |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Angular tests
npm run test
npm run build
```

### Résultats

- [x] Tests unitaires OK
- [x] Build OK
- [x] Analyse statique OK

## 11. Documentation

- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire

- Aucun

## 13. Statut final

Statut : DONE

## 14. Notes finales

Le problème venait de l'absence de l'attribut `"output": "assets"` pour l'entrée `"input": "src/assets"` dans `angular.json`. Ceci copiait les traductions globales de `src/assets/i18n` sous `/i18n/` au lieu de `/assets/i18n/`. Par conséquent, l'application cherchait en vain dans `/assets/i18n/` et retombait sur les fichiers factices minimalistes présents dans le dossier `public/assets/i18n/` qui masquaient l'erreur de chemin tout en n'apportant aucune traduction réelle en dehors des organisations pilotes.

## 15. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Correction d'un bug de chargement des traductions sans modification de contrat API. |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Oui (chemin d'assets) |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |

## 16. Impact thème / i18n / branding

- [x] Impact Angular UI analysé
- [x] Textes `fr` / `en` prévus

## 17. Verification `.gitignore`

- [x] `.gitignore` présent à la racine
- [x] Aucun secret, cache ou artefact de build versionné
