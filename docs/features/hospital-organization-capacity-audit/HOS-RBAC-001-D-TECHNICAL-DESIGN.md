# HOS-RBAC-001-D — Conception technique

## Décision

Retirer définitivement `HOSPITALIZATION_MANAGE` du catalogue applicatif et du référentiel persistant.

La stratégie retenue est **fail closed** : aucune traduction automatique vers les permissions HOS-RBAC-001-B/C n'est réalisée.

## État source

Dans `main@2584117e789d46b5187f90b5c2f4a3d2b0cfc904` :

- aucun contrôleur métier ne demande `HOSPITALIZATION_MANAGE` ;
- le frontend hospitalisation utilise uniquement les permissions dédiées et ses tests vérifient l'absence de fallback ;
- les rôles système `MEDECIN`, `INFIRMIER` et `RESPONSABLE_HOSPITALISATION` ne reçoivent plus la permission ;
- la permission reste encore déclarée dans `RbacCatalog.permissions()` uniquement pour rétro-compatibilité ;
- `RbacStore.seedCatalog()` ajoute/met à jour les permissions connues mais ne supprime pas les permissions retirées du code ;
- la FK `role_permissions.permission_code -> permissions.code` est `ON DELETE CASCADE` depuis V57.

## Changements techniques

### Catalogue Java

Supprimer la définition :

```text
HOSPITALIZATION_MANAGE
```

`RbacCatalog.permissionCodes()` ne doit plus l'exposer. Comme `ADMIN_CLINIQUE` est construit à partir de `all = permissionCodes()`, il cessera également de la recevoir sans traitement spécial.

### Migration V86

Ajouter une migration forward-only :

```sql
DELETE FROM permissions
WHERE code = 'HOSPITALIZATION_MANAGE';
```

La FK V57 en `ON DELETE CASCADE` supprime automatiquement les lignes `role_permissions` correspondantes, y compris pour les rôles personnalisés.

Aucun `UPDATE` de remapping n'est autorisé.

### Pourquoi une migration est nécessaire

Retirer uniquement la permission du Java serait insuffisant : `seedCatalog()` ne supprime pas les codes absents du catalogue. Une base ayant déjà connu la permission continuerait donc à l'exposer et à l'appliquer aux rôles personnalisés. V86 garantit que l'état DB converge vers l'état du code.

## Tests

### Catalogue

Étendre `RbacCatalogHospitalizationClinicalPermissionTest` :

- `permissionCodes()` ne contient pas `HOSPITALIZATION_MANAGE` ;
- les permissions spécialisées restent présentes ;
- les matrices système restent inchangées.

### Migration upgrade V85 → V86

Créer un test de migration qui :

1. migre une base jusqu'à V85 ;
2. insère une permission `HOSPITALIZATION_MANAGE` si nécessaire ;
3. crée un rôle personnalisé de test et l'association dans `role_permissions` ;
4. applique V86 ;
5. vérifie : permission absente, association supprimée, rôle personnalisé conservé ;
6. vérifie qu'aucune nouvelle permission hospitalière n'a été ajoutée au rôle.

Le test doit fonctionner sur PostgreSQL 16 ; une variante H2 peut couvrir la portabilité si le socle de test existant le permet.

### Greenfield

Le test PostgreSQL de migration globale doit vérifier :

- version courante >= 86 ;
- `HOSPITALIZATION_MANAGE` absent après `seedCatalog()` ;
- permissions dédiées présentes.

## API / Frontend

Aucune URL, payload ou logique UI à modifier. Les tests Angular existants qui vérifient l'absence de fallback sont conservés comme garde de non-régression.

## Sécurité

- moindre privilège renforcé ;
- suppression d'une autorité ambiguë ;
- aucun mapping automatique ;
- les rôles personnalisés perdent uniquement la permission obsolète ;
- toute réattribution est explicite et auditable via le RBAC existant.

## Rollback

La migration est volontairement destructive sur l'association obsolète. Un rollback applicatif vers un code qui attend `HOSPITALIZATION_MANAGE` n'est pas une stratégie acceptable après V86. En phase de développement, la direction est forward-only : corriger les permissions explicites plutôt que réintroduire la permission large.

## Impact SemVer

Le modèle d'autorisation change de façon incompatible pour les rôles personnalisés qui utilisent encore la permission supprimée. Impact prévu : **MAJOR** à la prochaine release incluant V86.
