# TICKET: MOB-2814 — Saisie & consultation mobile des notes cliniques

## Infos
- Mode: Engineering
- Date: 2026-07-30
- Priorité: P1 - Clinical Flow
- Statut: DONE
- Target Release: MINOR (v1.1.0)

## Description
Implémentation de l'interface mobile de consultation et saisie des notes cliniques SOAP (Subjectif, Objectif, Évaluation, Plan) connectée à l'API backend REST `GET/POST /api/visits/{id}/consultation-notes`.

## Check-list d'intervention
- [x] Documentation First (`FUNCTIONAL-SPEC.md`, `TECHNICAL-DESIGN.md`, `API-CONTRACT.md`)
- [x] Création du modèle domain `ConsultationNote`
- [x] Création de la gateway API `ConsultationApi`
- [x] Création du composant modal UI `ConsultationNotesSheet`
- [x] Raccordement i18n FR/EN et thèmes Light/Dark
- [x] Raccordement de l'action `_ActionChip` sur la carte patient du Dashboard
- [x] Écriture des tests unitaires `consultation_api_test.dart`
- [x] Verification de la suite complète `flutter test` (63/63 tests verts)
- [x] Mise à jour du suivi `PROJECT-TRACKING.md` et `CHANGELOG.md`

## Reste à faire
- Valider le flux complet lors des essais de recette backend Spring Boot.
