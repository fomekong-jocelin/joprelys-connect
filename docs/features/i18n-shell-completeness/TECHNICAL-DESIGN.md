# Conception technique — Complétude i18n du shell

Les traductions restent centralisées dans les dictionnaires JSON chargés par `I18nService`. Le shell traduit les libellés statiques via `i18n.t()`. Les codes de rôles sont résolus avec le préfixe `role.` et conservent le code comme repli pour les rôles personnalisés.

Le script `web/scripts/check-shell-i18n.mjs` extrait les clés littérales du shell et les clés `menu.*` du composant de navigation, fusionne les dictionnaires modulaires et échoue si une clé manque en français ou en anglais.

## Impacts

- API et base de données : aucun.
- Sécurité/RBAC : affichage uniquement, codes d'autorisation inchangés.
- Angular : shell partagé et ressources i18n.
