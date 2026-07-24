# BUG-20260724-STAFF-ASSIGNMENT-EDITOR-UI-I18N — Ergonome et i18n de l'éditeur d'affectations collaborateur

## 1. Contexte & Problématique

Lors de la modification d'un collaborateur dans le module d'administration du personnel, la section « Affectations hospitalières structurées » présentait les défauts UX/UI et i18n suivants :
1. Le formulaire d'ajout d'une nouvelle spécialité ou unité était affiché par défaut au-dessus des cartes enregistrées, et son bouton d'action (« Ajouter la spécialité » / « Ajouter l'affectation ») était collé directement sans espacement au-dessus de la carte d'affectation déjà saisie.
2. L'ordre d'affichage n'était pas ergonomique : l'utilisateur doit voir d'abord les affectations existantes (spécialités et unités) et n'ouvrir le formulaire de création que sur demande (bouton « + / Ajouter »).
3. L'internationalisation (i18n) n'était pas entièrement prise en compte : les libellés de la section `staff.assignments.*` utilisaient des textes en dur comme fallbacks et manquaient dans les dictionnaires FR et EN (`web/src/assets/i18n/features/staff-onboarding/fr.json` et `en.json`).

## 2. Période & Branche dédiée

- **Date** : 24 juillet 2026
- **Branche dédiée** : `fix/staff-assignment-editor-ui-i18n`
- **Impact** : Frontend Angular (`StaffAssignmentEditorComponent`, assets i18n `fr.json` et `en.json`)

## 3. Actions prévues

- [x] Créer la branche dédiée `fix/staff-assignment-editor-ui-i18n`
- [ ] Créer la documentation fonctionnelle & technique dans `docs/features/hos-staff-assignments-editor/`
- [ ] Refondre `StaffAssignmentEditorComponent` :
  - Afficher la liste des affectations enregistrées (cartes) en premier.
  - Masquer le formulaire d'ajout par défaut sous un bouton/toggle d'action « Ajouter une spécialité » / « Ajouter une affectation ».
  - Assurer un espacement aéré et conforme au design system entre les boutons, formulaires et cartes.
  - Conserver un état propre lorsque le formulaire est soumis ou annulé.
- [ ] Compléter l'i18n FR/EN pour toutes les clés `staff.assignments.*`.
- [ ] Exécuter les tests Angular (`npm run test` / lint / build prod) et vérifier zéro régression.
- [ ] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`.

## 4. Statut

En cours (Branche : `fix/staff-assignment-editor-ui-i18n`).
