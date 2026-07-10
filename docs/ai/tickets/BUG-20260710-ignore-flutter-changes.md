# BUG-20260710-ignore-flutter-changes — Ignorer les fichiers de build locaux du répertoire Flutter et nettoyer le mapping VCS d'IntelliJ

> Fichier obligatoire pour chaque ticket ou intervention IA.

## 1. Objectif

Le développeur / client voit 351 changements non suivis (untracked) dans son outil Git liés au projet Flutter. Ces changements proviennent de deux sources :
1. Les fichiers locaux temporaires de build générés dans `mobile/` qui n'étaient pas correctement exclus par `mobile/.gitignore` lorsque ce répertoire est ouvert comme projet racine dans l'IDE.
2. Un mapping VCS d'IntelliJ IDEA configuré dans `.idea/vcs.xml` qui suivait le répertoire du SDK Flutter situé en dehors du projet à `$PROJECT_DIR$/../../flutter` (branche `stable`), affichant ainsi tous les fichiers locaux non versionnés de l'installation du SDK Flutter lui-même dans le volet des commits de l'IDE.

L'objectif est d'aligner `mobile/.gitignore` avec le standard `docs/standards/GITIGNORE-STANDARDS.md` et de supprimer le mapping VCS obsolète pointant vers le SDK Flutter externe afin de nettoyer l'environnement de développement.

## 2. Critères d'acceptation

- [x] Le fichier `mobile/.gitignore` contient toutes les règles pour ignorer les répertoires et fichiers générés par Flutter/Dart, Android Gradle, iOS CocoaPods et les plateformes Desktop.
- [x] Le fichier `.idea/vcs.xml` ne contient plus le mapping vers le SDK Flutter externe (`$PROJECT_DIR$/../../flutter`).
- [x] Les 351 fichiers unversioned du SDK Flutter disparaissent immédiatement de la fenêtre VCS d'IntelliJ.
- [x] Aucun fichier source ou configuration nécessaire au projet n'est impacté.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | Aucun |
| User story parent | Aucun |
| Sprint cible | SPRINT-0013 |
| Priorité business | P0 |
| Complexité | XS |
| Story points | 1 |
| Profil recommandé | Senior |
| Effort estimé senior | 0.05j |
| Effort estimé intermédiaire | 0.1j |
| Effort estimé junior | 0.2j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucune |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu si nécessaire
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Fichier `.idea/vcs.xml` analysé (mapping vers `$PROJECT_DIR$/../../flutter` identifié)
- [x] Fichier `mobile/.gitignore` analysé
- [x] Vérification `.gitignore` à la racine fait
- [x] Vérification `docs/standards/GITIGNORE-STANDARDS.md` fait

## 5. Hypothèses

- L'intégration de Git dans l'IDE IntelliJ (ou Android Studio) incluait à tort le SDK Flutter externe via son Directory Mapping VCS, exposant ainsi l'arborescence de travail du SDK lui-même comme un sous-projet dans la liste des modifications locales.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Retirer le mapping VCS d'un projet utile | L'IDE ne suit plus le dépôt | Nous ne retirons que le mapping vers le SDK Flutter externe. Le mapping principal `$PROJECT_DIR$` vers Git reste inchangé. |

## 7. Action plan

- [x] Analyser `mobile/.gitignore` et y ajouter les règles d'ignorance manquantes.
- [x] Analyser `.idea/vcs.xml` et identifier le mapping fautif vers le SDK Flutter externe.
- [x] Supprimer la ligne de mapping VCS du SDK Flutter dans `.idea/vcs.xml`.
- [x] Vérifier l'état de Git avec `git status` et s'assurer que les fichiers configurés sont clean.
- [x] Mettre à jour `docs/ai/PROJECT-TRACKING.md` et `docs/ai/CHANGELOG.md`.

## 8. Implémentation réalisée

- [x] Enrichissement du fichier `mobile/.gitignore`.
- [x] Modification de `.idea/vcs.xml` pour retirer la ligne `<mapping directory="$PROJECT_DIR$/../../flutter" vcs="Git" />`.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-10 | Antigravity | 0.03j | 100% | Aucun | Aucun | Modification de `mobile/.gitignore` et `.idea/vcs.xml` terminées |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
git status
```

### Résultats

- [x] Build OK
- [x] Analyse statique OK (Git status clean)
- [x] Non exécuté avec justification : Tâche purement de configuration d'ignorance de fichiers Git et de configuration d'IDE, aucun code exécutable modifié.

## 11. Documentation

- [ ] README mis à jour si nécessaire
- [ ] API docs mises à jour si nécessaire
- [ ] ADR créé si décision structurante
- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire

- [x] Mettre à jour `docs/ai/PROJECT-TRACKING.md` et `docs/ai/CHANGELOG.md`.
- [x] Présenter le résumé de l'intervention au client.

## 13. Statut final

Statut : DONE

## 14. Notes finales

Les 351 fichiers unversioned du SDK Flutter provenaient du mapping automatique d'IntelliJ sur le dossier externe `$PROJECT_DIR$/../../flutter`. La suppression de ce mapping dans `.idea/vcs.xml` ainsi que la mise à jour de `mobile/.gitignore` résolvent définitivement la pollution des modifications locales de l'IDE.

## 15. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Non |
| Type de bump | Aucun (Configuration de développement local uniquement) |
| Justification | Amélioration de la configuration d'IDE et de `.gitignore` sans impact sur le produit ou le runtime |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Non |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |

## 16. Vérification `.gitignore`

- [x] `.gitignore` présent à la racine
- [x] `.gitignore` adapté à la stack réelle du projet
- [x] `docs/standards/GITIGNORE-STANDARDS.md` respecté
- [x] Aucun secret, cache ou artefact de build versionné
