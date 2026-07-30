# TICKET: BUG-20260730-MOBILE-DASHBOARD-UI-REDESIGN

## Infos
- Mode: Engineering
- Date: 2026-07-30
- Priorité: P2 - UX/UI Quality
- Statut: DONE
- Target Release: PATCH (v1.0.1)

## Description
Refonte visuelle du dashboard mobile Flutter (en-tête, marqueur d'espace clinique, cartes métriques d'attente, suppression du trait vert à gauche de la carte patient et assainissement complet de l'affichage DPU avec tirets insecables) afin de fournir un rendu haut de gamme, sobre et professionnel conforme au Design System.

## Check-list d'intervention
- [x] Analyse des captures d'écran et détection des retours UX (duplication `DPU DPU-`, saut de ligne maladroit `DPU-` sur 2 lignes, trait vert gauche à supprimer)
- [x] Suppression complète de la bande latérale verte sur `_ActiveVisitCard`
- [x] Assainissement Regex de la chaîne DPU (`RegExp(r'^(DPU[\s\-]*)+', caseSensitive: false)`) empêchant toute duplication `DPU DPU-`
- [x] Remplacement des tirets standards par des tirets insecables (`\u2011`) dans l'en-tête de la modale de constantes pour bannir le saut de ligne sur `DPU-`
- [x] Refonte des cartes métriques (`En attente`, `Prêts`, `À évaluer`) avec micro-icônes, bordures fines sémantiques et ombres sobres
- [x] Validation des 61 tests unitaires, widgets et goldens Flutter (`flutter test`)
- [x] Mise à jour des fichiers `PROJECT-TRACKING.md` et `CHANGELOG.md`

## Reste à faire
- Valider le rendu final sur un appareil mobile physique ou émulateur Android/iOS.
