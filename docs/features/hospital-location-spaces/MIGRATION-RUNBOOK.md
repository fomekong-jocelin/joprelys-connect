# MIGRATION-RUNBOOK — HOS-LOC-001-A

## 1. Nature de la migration

HOS-LOC-001-A est un breaking cleanup volontaire pendant la phase de développement.

Il remplace :

```text
Ward → Room → Bed
```

par :

```text
LocationNode? → Space → InpatientProfile → Bed
            ↘
              dated OrganizationalUnit ↔ Space
```

et remplace les clés texte d'admission par des UUID.

## 2. Pourquoi aucun auto-mapping

Les données legacy ne prouvent pas :

- qu'un `Ward.name` correspond à une unité V87 donnée ;
- qu'un `Room.roomNumber` correspond à une salle physique unique ;
- qu'un `service_name` d'hospitalisation identifie un service catalogué ;
- qu'un même numéro de chambre/lit garde le même sens dans le temps.

Un mapping par `LOWER(name)` ou heuristique serait une corruption silencieuse de données.

## 3. Stratégie Flyway

### V88 — preflight sans mutation

Migration Java qui :

1. détecte la présence des tables legacy ;
2. compte `wards`, `rooms`, `beds`, `bed_assignments`, `hospitalizations` ;
3. échoue si l'un de ces compteurs est non nul ;
4. n'exécute aucun DDL/DML métier ;
5. fournit un message clair avec les compteurs et la procédure attendue.

Exemple de message :

```text
HOS-LOC V88 blocked: legacy spatial/hospitalization data detected
wards=2 rooms=5 beds=10 bedAssignments=4 hospitalizations=3.
No automatic mapping is allowed. Reset or execute an approved remapping procedure before retrying.
```

Cette migration garantit qu'aucune suppression de table/colonne ne survient avant la décision humaine.

### V89 — nouveau schéma

Exécutée uniquement après V88 verte.

Ordre :

1. catalogues location/space ;
2. location nodes ;
3. spaces ;
4. inpatient profiles ;
5. unit-space assignments ;
6. refactor `beds` vers `space_id` ;
7. références structurées `hospitalizations` ;
8. renommage snapshots ;
9. suppression `rooms` puis `wards` ;
10. index/FK composites.

### V90 — contraintes temporelles PostgreSQL

Migration Java, sur le pattern déjà utilisé par V80 :

- PostgreSQL : exclusion GIST du chevauchement unit-space ;
- H2 test : règle contrôlée par le service et tests dédiés, sans simuler une syntaxe PostgreSQL incorrecte.

## 4. Greenfield

Sur une base vide :

```text
V1 → … → V87 → V88 preflight → V89 schema → V90 temporal constraint
```

Attendus :

- 0 migration failed ;
- Hibernate validate OK ;
- `wards` absent ;
- `rooms` absent ;
- `beds.space_id` présent et NOT NULL ;
- catalogues seedés ;
- hospitalizations structurées ;
- application démarre.

## 5. Environnement avec données legacy

La migration **doit s'arrêter à V88**.

Aucune action automatique n'est permise.

Deux stratégies seulement :

### A. Environnement de développement / démonstration réinitialisable

1. sauvegarde si nécessaire ;
2. reset de la base selon processus autorisé ;
3. création greenfield depuis le SHA validé ;
4. création des données structurées via API/UI.

### B. Environnement à données à conserver

Créer un ticket de remapping dédié avec :

- inventaire des wards/rooms/beds/hospitalizations ;
- mapping manuel approuvé vers organizational units/spaces ;
- script versionné et testé ;
- dry-run ;
- preuve des comptes avant/après ;
- rollback.

Ce script n'appartient pas à HOS-LOC-001-A tant qu'aucun environnement à conserver n'en démontre le besoin.

## 6. Interdictions

- pas de déduction `Ward.name == OrganizationalUnit.name` ;
- pas de `Room.roomNumber == Space.code` implicite ;
- pas de service/room/bed créé par défaut pour faire passer Flyway ;
- pas de `NULL` temporaire conservé dans les références courantes ;
- pas d'alias DB legacy ;
- pas de migration exécutée manuellement sur serveur comme source de vérité.

## 7. Rollback en développement

Tant que le lot n'est pas déployé sur un environnement partagé :

- rollback = revenir au commit `main` précédent et recréer la DB de développement depuis ses migrations ;
- aucune migration Flyway déjà appliquée n'est modifiée après merge.

Pour un environnement partagé, le rollback doit être préparé comme opération séparée avant déploiement : dump validé + ancien artefact + ancienne DB restaurable.

## 8. Préflight avant merge

- V1→nouvelle tête sur PostgreSQL 16 ;
- V87→V88 avec fixture legacy : échec attendu et vérification qu'aucun objet V89 n'existe ;
- V87 vide→nouvelle tête : succès ;
- `flyway_schema_history` sans failed ;
- Hibernate validate ;
- Maven global ;
- Angular tests/build.

## 9. Déploiement

Ce document ne déclenche **aucun déploiement**.

Après merge, tout déploiement suit les scripts versionnés et le processus GitHub/CI. PROD/RECETTE ne sont jamais des environnements de développement.
