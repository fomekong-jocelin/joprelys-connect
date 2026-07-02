# Modèle de Données — Gestion du Personnel Clinique (STORY-0104)

## 1. Tables utilisées

La story réutilise la table existante :

```text
users
```

Champs utilisés :

| Champ | Usage |
|---|---|
| `id` | Identifiant du collaborateur |
| `email` | Identifiant de connexion, unique globalement |
| `display_name` | Nom affiché dans l'équipe |
| `role` | Rôle clinique (`MEDECIN`, `INFIRMIER`, `AGENT_ACCUEIL`, `PHARMACIEN`) |
| `password_hash` | Hash BCrypt du mot de passe temporaire |
| `enabled` | Activation ou suspension du compte |
| `organization_id` | Isolation multi-tenant par clinique |
| `created_at` / `updated_at` | Traçabilité technique |

## 2. Migration

Aucune migration Flyway n'est ajoutée pour STORY-0104.

Justification :
- la table `users` existe déjà depuis `V1__create_users_and_auth_audit.sql` ;
- la colonne `organization_id` existe déjà depuis `V2__create_organizations_table.sql` ;
- l'index `idx_users_organization_id` existe déjà pour les listes par clinique.

## 3. Contraintes métier

- `email` reste unique globalement.
- `organization_id` est obligatoire fonctionnellement pour les comptes gérés par un `ADMIN_CLINIQUE`.
- Les comptes administrateurs de clinique ne sont pas exposés comme collaborateurs gérables via `/api/staff`.
