# HOS-BED-001-D — Conception technique

## Architecture retenue

```text
V79 SQL portable
  └── métadonnées VALID / QUARANTINED
      └── préflight PostgreSQL en lecture seule
          ├── zéro conflit → V80
          └── conflits → arbitrage + quarantaine manuelle → V80

V80 Java Flyway
  ├── H2 : no-op versionné
  └── PostgreSQL :
      ├── compte les chevauchements VALID
      ├── échoue si le compteur > 0
      ├── CREATE EXTENSION btree_gist
      └── ADD EXCLUDE USING gist
```

## V79 — Métadonnées de quarantaine

Colonnes ajoutées à `bed_assignments` :

| Colonne | Type | Rôle |
|---|---|---|
| `integrity_status` | `VARCHAR(20)` | `VALID` par défaut ou `QUARANTINED` |
| `quarantined_at` | `TIMESTAMP` | date d'arbitrage |
| `quarantine_reason` | `VARCHAR(500)` | justification traçable |
| `quarantined_by` | `VARCHAR(255)` | acteur ou référence d'autorisation |

La contrainte `ck_bed_assignments_integrity_quarantine` impose :

- aucune métadonnée de quarantaine pour une ligne `VALID` ;
- une période clôturée, une date, un motif et un acteur pour une ligne `QUARANTINED`.

Cette migration reste compatible avec H2 et PostgreSQL. Les nouvelles affectations applicatives utilisent le défaut `VALID`, sans changement de contrat JPA.

## V80 — Migration Java Flyway

La migration Java est placée sous `db.migration`, dans le même emplacement de classe que `classpath:db/migration`.

### Détection du moteur

- `PostgreSQL` : exécution complète ;
- autre moteur, notamment H2 : retour immédiat sans DDL spécifique.

Le no-op H2 est volontaire : H2 reste une suite rapide, mais n'est pas une preuve de l'intégrité temporelle critique.

### Préflight embarqué

V80 compte les paires qui satisfont simultanément :

- même `organization_id` ;
- même `bed_id` ;
- deux statuts `VALID` ;
- plages `tsrange(..., '[)')` qui se chevauchent avec `&&`.

La jointure ordonne les UUID (`left.id < right.id`) pour compter une paire une seule fois.

Si le compteur est non nul, V80 lève une `FlywayException` avant l'installation de l'extension ou de la contrainte.

## Contrainte PostgreSQL

```sql
ALTER TABLE bed_assignments
    ADD CONSTRAINT ex_bed_assignments_valid_period_no_overlap
    EXCLUDE USING gist (
        organization_id WITH =,
        bed_id WITH =,
        tsrange(
            assigned_at,
            COALESCE(released_at, 'infinity'::timestamp),
            '[)') WITH &&
    )
    WHERE (integrity_status = 'VALID');
```

### Propriétés

- contrainte immédiate, non différable ;
- index GiST créé automatiquement ;
- portée partielle limitée aux lignes `VALID` ;
- `released_at IS NULL` devient une borne infinie ;
- l'adjacence n'est pas un chevauchement avec `[)` ;
- toute mise à jour de `assigned_at`, `released_at`, `bed_id`, `organization_id` ou `integrity_status` réévalue la contrainte.

## Extension `btree_gist`

GiST gère nativement les plages, mais l'égalité des UUID dans un index GiST multicolonne nécessite les classes d'opérateurs fournies par `btree_gist`.

La migration exécute :

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;
```

Précondition d'exploitation : l'utilisateur Flyway doit être propriétaire de la base ou disposer du privilège `CREATE`, sinon le DBA doit préinstaller l'extension.

## Concurrence

La contrainte est évaluée par PostgreSQL au niveau de la base et protège :

- plusieurs instances backend ;
- imports ;
- scripts SQL ;
- corrections rétroactives ;
- écritures concurrentes.

La collision est remontée comme violation d'intégrité. Les use cases applicatifs devront continuer à traduire ces collisions en conflit métier HTTP `409` lorsqu'elles passent par une API.

## Quarantaine

Le script `QUARANTINE-APPROVED-ASSIGNMENTS.sql` :

1. exige une table temporaire d'IDs explicitement approuvés ;
2. refuse un ID absent, actif ou déjà traité ;
3. marque les lignes sans les déplacer ni les supprimer ;
4. revérifie tous les chevauchements `VALID` ;
5. annule la transaction si un conflit subsiste.

Aucun algorithme ne choisit automatiquement la période correcte.

## Tests

`BedAssignmentOverlapPostgresqlMigrationTest` couvre :

- migration limitée à V79 ;
- insertion de données historiques incompatibles ;
- échec attendu de V80 ;
- détection d'une paire ;
- quarantaine justifiée ;
- relance et succès de V80 ;
- présence de `btree_gist` et de la contrainte ;
- adjacence acceptée ;
- même période sur un autre lit acceptée ;
- nouvel overlap refusé ;
- correction rétroactive refusée ;
- conservation de la ligne quarantinée.

Les suites Spring/H2 valident la portabilité de V79 et la découverte sans erreur de V80.

## Observabilité

Avant production, l'exploitation doit disposer de contrôles sur :

- échec Flyway avec préfixe `HOS-BED-001-D` ;
- nombre de lignes `QUARANTINED` par établissement ;
- nouvelles violations de la contrainte ;
- âge des arbitrages non finalisés ;
- preuve du rapport de préflight associé à chaque déploiement.

## Déploiement et rollback

Le plan détaillé est dans ADR-0003. Le rollback technique minimal supprime uniquement la contrainte d'exclusion. Les métadonnées, lignes quarantinées et l'extension sont conservées afin de ne pas perdre la preuve d'intégrité.
