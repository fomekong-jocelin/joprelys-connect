# STORY-1912 — Gestion Spatiale (Lits & Chambres)

## 1. Description et contexte

**Epic** : EPIC-0014 — Alignement des modules 4 à 12 du CDC
**Titre** : Gestion Spatiale (Lits & Chambres)
**Statut** : **DONE**
**Assigné** : **Antigravity**
**Priorité** : P1
**Sprint** : SPRINT-0012
**SP** : 5
**Profil recommandé** : Intermédiaire Full-stack
**Estimation** : 2.5j (Senior: 1.8j, Junior: 4.5j)

## 2. Objectifs

Permettre de cartographier la clinique par services, chambres et lits, d'attribuer transactionnellement un lit libre à un patient hospitalisé, et de gérer les transferts de lits en prévenant les accès concurrents.

## 3. Critères d'acceptation (DoD)

- [x] Script de migration SQL Flyway `V44__create_spatial_tables.sql` pour les tables `wards`, `rooms`, `beds` et `bed_assignments`.
- [x] Entités JPA avec verrouillage optimiste (`@Version` sur `BedEntity`) et multi-tenancy (`@TenantId`).
- [x] Endpoints REST CRUD sécurisés dans `SpatialController`.
- [x] Implémentation du service Angular `SpatialApiService`.
- [x] Interface frontend réactive (grille d'occupation) sous Tailwind CSS v4, affichant l'occupation des lits en temps réel par service.
- [x] Intégration de la sélection de lit lors de l'admission dans le Drawer d'hospitalisation.
- [x] Gestion du cycle de vie du lit (`FREE`, `OCCUPIED`, `CLEANING`, `MAINTENANCE`) avec changement rapide d'état.
- [x] Traductions FR/EN complètes.
- [x] Tests de concurrence et d'intégration backend au vert.

## 4. Plan de découpage en sous-tâches

### Subtasks Techniques

- [x] **SUB-1912-01** : Script SQL de migration et configuration des entités JPA.
- [x] **SUB-1912-02** : Implémentation du service métier `SpatialService` et des repositories avec verrouillage optimiste.
- [x] **SUB-1912-03** : Création du controller REST `/api/spatial/*` et des tests d'intégration (MockMvc).
- [x] **SUB-1912-04** : Création des composants et services Angular (grille réactive par service).
- [x] **SUB-1912-05** : Intégration dans le flux d'admission et de transfert d'hospitalisation.
- [x] **SUB-1912-06** : Intégration i18n et tests de non-régression.

## 5. Définition de Prêt (DoR)

- [x] Spécifications fonctionnelles rédigées dans `docs/features/spatial-management/FUNCTIONAL-SPEC.md`.
- [x] Conception technique rédigée dans `docs/features/spatial-management/TECHNICAL-DESIGN.md`.
- [x] Modèle de données SQL validé.

## 6. Définition de Fini (DoD)

- [x] Critères d'acceptation validés.
- [x] Tous les tests passent avec succès (backend Maven et frontend Vitest).
- [x] Documentation mise à jour et changelog complété.
- [x] Review de code faite et approuvée par le Reviewer.

## 7. Notes de correction UI (2026-07-08)

- Remplacement des emojis par le composant d'icônes `app-ui-icon`.
- Utilisation des tokens CSS du design system pour les cartes, badges et actions de lits.
- Harmonisation des couleurs des KPI avec les variables sémantiques (`--brand-success`, `--brand-danger`, `--brand-info`).
- Correction des ombres et des arrondis selon `DESIGN.md`.
- Complétion des traductions FR/EN.

**Reviewer** : Lead Developer / Architecte
