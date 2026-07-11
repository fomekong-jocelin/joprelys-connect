# Conception technique — Administration RBAC clinique

## Modèle retenu

La colonne historique `users.role` reste utilisée pour préserver la compatibilité. Elle contient une liste ordonnée de codes séparés par des virgules. `ClinicRoleCatalog` devient l’unique source backend pour :

- les rôles attribuables par une clinique ;
- leur ordre canonique ;
- leur catégorie et leur caractère sensible ;
- la normalisation et le rejet des rôles inconnus.

Aucune migration de données n’est nécessaire pour cette version.

## Backend

### Administration

- `GET /api/staff/roles` retourne le catalogue au seul `ADMIN_CLINIQUE` ;
- `POST /api/staff` et `PUT /api/staff/{id}` acceptent une chaîne multi-rôles ;
- le service normalise, déduplique et valide les codes ;
- les recherches et modifications restent filtrées par `organizationId` ;
- le répertoire consulté par les rôles non administrateurs masque les comptes financiers et de gouvernance.

### Application immédiate

Le JWT conserve les rôles présents au moment de sa création pour la compatibilité, mais `JwtAuthenticationFilter` recharge l’utilisateur à chaque requête staff :

- un utilisateur désactivé reçoit immédiatement `401` ;
- une nouvelle affectation est immédiatement transformée en autorités `ROLE_*` ;
- une suppression de rôle retire immédiatement l’autorité correspondante ;
- le tenant effectif provient de l’utilisateur courant en base.

Les jetons patient restent autonomes et ne nécessitent pas de compte dans `users`.

### Garde-fous

- interdiction de s’administrer soi-même ;
- interdiction des rôles plateforme/patient ;
- protection du dernier administrateur actif ;
- OTP étendu aux rôles financiers et d’audit sensibles.

## Frontend

- le formulaire charge le catalogue via `/api/staff/roles` ;
- les boutons radio deviennent des cases à cocher descriptives ;
- le `roleGuard` synchronise `/api/auth/me` avant chaque navigation staff ;
- le stockage de session met à jour le nom et les rôles sans remplacer le jeton ;
- les menus sont l’union dédupliquée des espaces autorisés par les rôles ;
- DAF et secrétaire comptable accèdent au workspace facturation ;
- le caissier conserve son poste dédié.

## Limitation

Les contrôles `@PreAuthorize` restent adossés à des rôles système audités. Une future matrice permission par permission nécessitera des tables `roles`, `permissions`, `role_permissions` et `user_roles`, ainsi qu’une migration coordonnée de tous les contrôles backend.
