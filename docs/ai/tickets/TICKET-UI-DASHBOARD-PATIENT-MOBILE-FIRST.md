# TICKET — UI : Dashboard patient — Mobile-first & Journal d'audit allégé

**Date** : 2026-07-02
**Mode** : Engineering / UI
**Statut** : DONE
**Version impact** : PATCH

---

## Contexte

Le dashboard patient était trop chargé visuellement.
Problèmes : journal d'audit trop volumineux sans pagination, tableau non adapté mobile, profil disparu sur petit écran.

---

## Actions réalisées

- [x] patient-audit-list.component.ts : en-tête compact, cards sur mobile, tableau compact desktop, pagination 5 entrées, bouton Voir plus
- [x] patient-dashboard.component.ts : bannière profil compact toujours visible, cartes scrollables mobile, profil détaillé masqué sur mobile avec bloc dépliable

---

## Fichiers modifiés

- web/src/app/patient/portal/components/patient-audit-list.component.ts
- web/src/app/patient/portal/patient-dashboard.component.ts

---

## Impact version / SemVer

PATCH : amélioration UI uniquement, aucun impact API ou backend.
