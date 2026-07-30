# TICKET: MOB-2816 — Historique Médical & Chronologie des Visites du Patient

## Infos
- Mode: Engineering
- Date: 2026-07-30
- Priorité: P1 - Clinical Continuity
- Statut: DONE
- Target Release: MINOR (v1.1.0)

## Description
Implémentation de la vue synthétique et chronologique complète du dossier patient sur mobile : antécédents médicaux/chirurgicaux, allergies avec niveaux de gravité et chronologie des consultations passées.

## Check-list d'intervention
- [x] Documentation First (`FUNCTIONAL-SPEC.md`, `TECHNICAL-DESIGN.md`, `API-CONTRACT.md`)
- [x] Création du modèle de domaine `patient_history.dart`
- [x] Création de la gateway & API client `patient_history_api.dart`
- [x] Création du composant modal `PatientHistorySheet`
- [x] Raccordement de l'action `[Dossier & Historique]` sur les cartes du Dashboard
- [x] Support 100% i18n FR/EN et thèmes Light/Dark
- [x] Écriture des tests unitaires `patient_history_api_test.dart`
- [x] Exécution de la suite complète `flutter test` (68/68 tests verts)
- [x] Mise à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`

## Reste à faire
- Valider l'affichage sur appareil physique.
