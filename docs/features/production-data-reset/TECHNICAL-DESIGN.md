# Réinitialisation des données de production — Conception technique

Le script `scripts/reset_production_data.py` utilise Paramiko avec `RejectPolicy` et le fichier `known_hosts`. Il exige deux secrets distincts par variables d'environnement : mot de passe SSH et mot de passe du nouvel administrateur.

Le service est arrêté, un dump PostgreSQL au format custom est produit, puis un SQL transactionnel tronque toutes les tables `public` sauf `flyway_schema_history`. Le compte administrateur est inséré avec un UUID aléatoire et un hash BCrypt coût 12. Au redémarrage, `RbacBootstrap` régénère `roles`, `permissions`, `role_permissions` et synchronise `user_roles`.

Garde-fous : confirmation `PURGE_PRODUCTION`, sauvegarde non vide, transaction `ON_ERROR_STOP`, redémarrage même en cas d'échec, aucune valeur secrète dans les sorties.
