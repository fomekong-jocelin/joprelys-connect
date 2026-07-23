# TICKET-PREPROD-ADMIN-CLEANUP — Durcissement du bootstrap administrateur avant préproduction

> Source de vérité : code courant + configuration versionnée. Aucun credential de bootstrap ne doit être documenté ou fourni par défaut.

## 1. Objectif

Préparer Joprelys Connect pour la préproduction et le Go-Live en supprimant les comportements de bootstrap dangereux ou ambigus :

1. le bootstrap `ADMIN_JOPRELYS` est **désactivé par défaut** ;
2. son activation est une action explicite via `JOPRELYS_SEED_ADMIN_ENABLED=true` ;
3. email, nom et mot de passe sont obligatoires lorsque le bootstrap est activé ;
4. aucun email, mot de passe ou credential de secours n'est codé en dur dans `AdminUserSeeder` ;
5. une configuration activée mais incomplète provoque un échec explicite au démarrage ;
6. le mot de passe n'est jamais journalisé et reste encodé via `PasswordEncoder` ;
7. le seeder ne crée aucun établissement, médecin, pharmacien ou autre donnée de démonstration.

## 2. Contexte et réconciliation

Une ancienne PR de durcissement (#118) a été fermée sans fusion. Une partie de ses intentions restait pertinente, mais aucun cherry-pick global n'est effectué : le correctif est repris proprement depuis le `main` courant afin d'éviter la réintroduction de code obsolète.

L'audit a également identifié une contradiction RBAC :

- `RbacCatalog` déclare `ADMIN_JOPRELYS` et `SUPER_ADMIN` non attribuables depuis une clinique ;
- `RbacPlatformRolePolicyInitializer` les remettait `assignable = TRUE` au démarrage.

La politique canonique est désormais le catalogue RBAC. L'initializer contradictoire est supprimé et l'API conserve un refus explicite `403` pour toute tentative clinique d'attribution d'un rôle plateforme.

## 3. Critères d'acceptation

- [x] `joprelys.seed.admin.enabled` vaut `false` par défaut dans `application.yml`.
- [x] `AdminUserSeeder` n'est instancié que lorsque `joprelys.seed.admin.enabled=true`.
- [x] aucune valeur de repli pour l'email, le nom ou le mot de passe n'existe dans le seeder.
- [x] `SeedAdminProperties.isComplete()` exige `enabled + email + name + password`.
- [x] une configuration activée mais incomplète échoue avant toute lecture/écriture en base et avant tout hash.
- [x] la création reste idempotente si le compte existe déjà.
- [x] l'email est normalisé, le nom est trimé et le mot de passe est hashé.
- [x] `RbacPlatformRolePolicyInitializer` est supprimé.
- [x] les rôles plateforme restent non attribuables dans `RbacCatalog`.
- [x] une tentative clinique vers `ADMIN_JOPRELYS` ou `SUPER_ADMIN` conserve le contrat `403`.
- [x] un rôle clinique normal désactivé/non attribuable conserve le contrat générique `400` par l'ordre des contrôles.
- [x] aucun credential réel ou de démonstration n'est documenté dans ce ticket.
- [ ] Maven strict `clean verify` vert sur le SHA de la PR.
- [ ] revue Tech Lead / sécurité terminée avant fusion.

## 4. Implémentation

### Configuration

```yaml
joprelys:
  seed:
    admin:
      enabled: ${JOPRELYS_SEED_ADMIN_ENABLED:false}
      email: ${JOPRELYS_ADMIN_EMAIL:}
      name: ${JOPRELYS_ADMIN_NAME:}
      password: ${JOPRELYS_ADMIN_PASSWORD:}
```

Aucun secret n'est fourni par le dépôt. Les valeurs doivent venir du mécanisme de secrets de l'environnement cible.

### Backend

- `SeedAdminProperties` vérifie que tous les paramètres requis sont renseignés.
- `AdminUserSeeder` est opt-in et fail-fast en cas de configuration incomplète.
- aucun fallback de credential n'est conservé.
- `RbacAdministrationService` refuse les rôles plateforme avant le contrôle générique d'assignabilité.
- `RbacPlatformRolePolicyInitializer` est supprimé.

### Tests

- `AdminUserSeederTest` : fail-fast, idempotence, normalisation et hash ;
- `SeedAdminPropertiesTest` : activation et champs obligatoires ;
- `RbacCatalogPermissionIsolationTest` : rôles plateforme non attribuables ;
- `RbacControllerTest` existant : attribution `SUPER_ADMIN` / `ADMIN_JOPRELYS` refusée en `403`.

## 5. Procédure d'exploitation

Le bootstrap ne doit être activé que pour une initialisation contrôlée :

1. injecter les quatre variables `JOPRELYS_SEED_ADMIN_ENABLED`, `JOPRELYS_ADMIN_EMAIL`, `JOPRELYS_ADMIN_NAME`, `JOPRELYS_ADMIN_PASSWORD` via le gestionnaire de secrets ;
2. démarrer l'application et confirmer la création idempotente du compte plateforme ;
3. désactiver le bootstrap après l'initialisation ;
4. ne jamais conserver le mot de passe dans Git, un ticket, une capture ou un runbook.

## 6. Risques / garde-fous

- aucune migration Flyway n'est nécessaire ;
- aucun contrat REST métier n'est modifié ;
- aucune donnée PROD/RECETTE n'est modifiée par cette intervention ;
- ne pas réutiliser les anciennes valeurs documentées dans l'historique Git ; elles doivent être considérées comme compromises si elles ont déjà été utilisées ;
- toute réintroduction d'un credential par défaut bloque la fusion.

## 7. SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH sécurité/configuration |
| Breaking change API | Non |
| Migration DB | Non |
| Impact Angular | Non |
| Impact Flutter | Non |
| Changelog requis | Oui |
