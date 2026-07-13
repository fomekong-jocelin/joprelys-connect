# Changelog — 2026-07-13 — STORY-2302

## Added

- **Triage ABCDE et réévaluations d’urgence (STORY-2302 / #42)** : ajout d’un journal clinique append-only pour l’évaluation initiale et les réévaluations successives, avec les cinq axes ABCDE, les constantes, le niveau de triage, l’état hémodynamique, l’orientation recommandée, l’auteur et l’heure clinique.
- **API de triage tenantée** : ajout des endpoints de lecture de l’historique, lecture unitaire et création d’une réévaluation sous `/api/emergencies/{emergencyId}/triage-assessments`.
- **Migration V64** : création de `emergency_triage_assessments` et reprise non destructive des urgences historiques avec les axes absents marqués `NOT_ASSESSED`.
- **Workspace d’urgence** : ajout d’un panneau mobile-first dans l’onglet Soins, compatible FR/EN, light/dark et clavier, avec affichage lecture seule lorsque l’utilisateur ne possède pas `EMERGENCY_WRITE`.
- **Sécurité et traçabilité** : verrouillage pessimiste des séquences concurrentes, refus après stabilisation, isolation tenant, RBAC et audit des lectures/écritures sans contenu clinique sensible.
- **Tests** : couverture de la création atomique URG-TEMP, du triage initial, des réévaluations, de la concurrence, du cross-tenant, des permissions, des migrations H2/PostgreSQL 16 et du composant Angular.

## Compatibility

- Évolution rétrocompatible ; les payloads historiques de création d’urgence restent valides.
- Aucun ancien champ, endpoint ou numéro métier n’est supprimé ou renommé.
- Impact SemVer recommandé : `MINOR`.