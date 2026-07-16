# BUG-20260716-I18N-SHELL-MISSING-KEYS — Clés brutes dans le shell

**Mode** : Engineering + QA Review

**Date** : 2026-07-16

**Statut** : DONE

**Priorité** : P1

**Estimation** : 0,25 j senior

**Profil recommandé** : Frontend Angular / i18n

**Reviewer** : Lead Frontend + QA

**Impact version** : PATCH

## Objectif

Empêcher l'affichage de clés i18n ou de codes de rôles bruts dans le shell Angular, notamment `menu.rbac`.

## Critères d'acceptation

- [x] Le menu RBAC est traduit en français et en anglais.
- [x] Les libellés du menu mobile utilisent le service i18n.
- [x] Les rôles principaux visibles dans l'en-tête sont traduits.
- [x] Toutes les clés de navigation sont présentes dans les deux langues.
- [x] Un contrôle automatisé bloque une nouvelle clé manquante dans le shell.
- [x] Build Angular validé.

## Actions

- [x] Auditer les clés utilisées par le shell et sa navigation.
- [x] Compléter les dictionnaires FR/EN.
- [x] Remplacer les libellés codés en dur.
- [x] Ajouter le contrôle de complétude.
- [x] Exécuter les vérifications et mettre à jour le suivi.

## Reste à faire

- Déployer le frontend corrigé en production.
