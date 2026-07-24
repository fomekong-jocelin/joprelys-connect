# FUNCTIONAL-SPEC — Éditeur d'affectations hospitalières collaborateur (HOS-STAFF)

## 1. Objectif Fonctionnel

L'éditeur d'affectations hospitalières structurées permet d'administrer les spécialités médicales et les affectations aux unités organisationnelles d'un collaborateur du personnel de santé.

## 2. Parcours Utilisateur & Ergonomie

1. **Consultation des affectations existantes (Vue par défaut)** :
   - Lorsque le gestionnaire ouvre la fiche de modification d'un collaborateur, la section « Affectations hospitalières structurées » affiche immédiatement les cartes des spécialités et unités déjà enregistrées pour ce collaborateur (actives et historiques).
   - Chaque carte indique : libellé de l'élément, période de validité (`début → fin` ou `sans fin`), badges (`Principale`, `Active` / `Historique`) et l'action « Clôturer maintenant » pour les affectations actives.
   - En l'absence d'élément enregistré, un message informatif clair s'affiche (« Aucune spécialité structurée. » / « Aucune affectation d’unité. »).

2. **Ajout d'une nouvelle affectation (Action à la demande)** :
   - Un bouton d'action secondaire (« + Ajouter une spécialité » / « + Ajouter une affectation ») permet d'ouvrir/replier le formulaire d'ajout.
   - Le formulaire d'ajout apparaît de façon fluide avec un espacement net (pas d'élément collé aux cartes).
   - Le formulaire comporte des boutons « Enregistrer la spécialité / l'affectation » et « Annuler ».
   - Une fois l'affectation enregistrée avec succès, le formulaire se réinitialise et se replie automatiquement, et la liste des cartes s'actualise.

3. **Internationalisation (i18n)** :
   - L'intégralité des titres, sous-titres, champs, placeholders, boutons, options et messages d'erreur bascule dynamiquement selon la langue choisite (Français `fr` / Anglais `en`).

## 3. Critères d'Acceptation

- [x] Les cartes d'affectations enregistrées apparaissent au-dessus du bouton d'ajout.
- [x] Le formulaire de création n'est pas affiché par défaut ; il s'ouvre sur clic du bouton d'ajout.
- [x] Les boutons d'action disposent d'un espacement vertical et horizontal conforme au design system (`DESIGN.md`).
- [x] Toutes les chaînes de caractères disposent d'une clé de traduction présente dans `fr.json` et `en.json`.
- [x] Aucune régression sur la sauvegarde ou la clôture d'affectations (contrats API préservés).
