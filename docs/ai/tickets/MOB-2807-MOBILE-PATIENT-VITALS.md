# TICKET: MOB-2807 — Saisie mobile des constantes patient & raccordement file active

## Infos
- Mode: Engineering
- Date: 2026-07-30
- Priorité: P1 - Clinical Flow
- Statut: DONE
- Target Release: MINOR (v1.1.0)

## Description
Raccordement de la carte patient de la file active mobile (`_ActiveVisitCard`) avec une modale/feuille de saisie et de consultation des constantes vitales (`PatientVitalsSheet`), connectée aux endpoints `GET/POST /api/visits/{id}/vitals`.

## Check-list d'intervention
- [x] Documentation First (`FUNCTIONAL-SPEC.md`, `TECHNICAL-DESIGN.md`, `API-CONTRACT.md`)
- [x] Création du modèle domain `PatientVitals`
- [x] Gateway `VitalsApi` branchée sur `ApiClient`
- [x] Contrôleur et calcul dynamique d'IMC
- [x] Interface `PatientVitalsSheet` dans `features/dashboard/presentation/widgets`
- [x] Raccordement du tap sur `_ActiveVisitCard` dans `active_queue_section.dart`
- [x] Validation des tests unitaires (`vitals_api_test.dart`) et d'intégration Flutter
- [x] Mise à jour du suivi `PROJECT-TRACKING.md` et `CHANGELOG.md`

## Reste à faire
- Valider le flux réel lors du prochain test d'intégration backend/mobile.
