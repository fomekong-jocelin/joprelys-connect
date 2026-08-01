# TICKET: MOB-2810 — Historique Médical & Chronologie des Visites du Patient

## Infos
- Mode: Engineering
- Date: 2026-07-30
- Priorité: P0 - Clinical Continuity
- Statut: REVIEW_BLOCKED — ID canonique réconcilié et livraison fusionnée par
  la PR #258 ; widget au-dessus de 500 lignes et recette backend/appareil absente
- Target Release: candidat MINOR `0.11.0` ; aucune release préparée

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
- [x] Réattribution de l'ancien ticket MOB-2816 au dossier patient canonique
  MOB-2810, sans perte de l'historique de livraison de la PR #258

## Reste à faire
- Découper `patient_history_sheet.dart` sous 500 lignes.
- Valider l'affichage sur appareil physique.
- Obtenir les gates Flutter, APK et intégration backend sur le HEAD exact.
