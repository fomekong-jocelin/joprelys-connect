# Spécification fonctionnelle — Complétude i18n du shell

Le shell doit afficher un libellé humain dans la langue active pour chaque entrée de navigation, contrôle mobile et rôle utilisateur connu. Aucune clé technique telle que `menu.rbac` ne doit être visible.

## Critères

- Français et anglais sont couverts.
- Une clé inconnue conserve un repli lisible uniquement pour les rôles personnalisés.
- Le changement de langue actualise les libellés sans rechargement métier.
