# 2026-07-24 — Configuration hospitalière mobile & seed Trauma Center

## UI

- `PageHeader` réellement responsive : titre puis actions empilées en mobile, alignement horizontal seulement lorsque la largeur est suffisante ;
- correction du sélecteur d'établissement qui pouvait être compressé jusqu'à n'afficher que son chevron ;
- breadcrumbs métier FR/EN pour Organisation hospitalière et Structure hospitalière ;
- sous-titre de Structure hospitalière aligné sur HOS-LOC : géographie, espaces, rattachements unité↔espace et lits ;
- description des spécialités HOS-ORG actualisée après fusion HOS-STAFF.

## Démonstration

Ajout de `scripts/demo/seed-trauma-center-hospital-config.sql`, script hors Flyway destiné à préparer rapidement TRAUMA CENTER pour la répétition générale et la présentation client : organisation, géographie, espaces, profils d'hébergement, lits et rattachements structurés.

Le script est tenant-safe par sélection explicite de `TRAUMA CENTER`, refuse une sélection ambiguë et ne modifie aucune donnée clinique ou utilisateur.
