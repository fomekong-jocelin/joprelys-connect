# Services hospitaliers typés — Modèle de données

## Table `wards`

Nouvelle colonne :

| Colonne | Type | Null | Défaut | Règle |
|---|---|---:|---|---|
| `service_type` | `VARCHAR(40)` | Non | Aucun | valeur fermée de `HospitalServiceType` |

Valeurs autorisées :

```text
HOSPITALIZATION
EMERGENCY
OUTPATIENT
MEDICO_TECHNICAL
PHARMACY
ADMINISTRATIVE
```

## Invariants

- `wards.service_type` est toujours renseigné ;
- `rooms.ward_id` référence un service autorisant les chambres ;
- les contraintes d'unicité existantes par organisation sont conservées ;
- aucun type n'est déduit du nom du service ;
- aucune valeur par défaut n'est posée en base.

## Migration V74

### Étape 1 — ajouter la colonne nullable

```sql
ALTER TABLE wards ADD COLUMN service_type VARCHAR(40);
```

### Étape 2 — qualifier les services prouvés spatiaux

Un service ayant déjà au moins une chambre est nécessairement une unité spatiale dans le modèle existant :

```sql
UPDATE wards w
SET service_type = 'HOSPITALIZATION'
WHERE EXISTS (
    SELECT 1 FROM rooms r WHERE r.ward_id = w.id
);
```

### Étape 3 — rendre le modèle strict

```sql
ALTER TABLE wards ALTER COLUMN service_type SET NOT NULL;
```

Cette étape échoue volontairement s'il reste un service historique sans chambre et sans qualification explicite.

### Étape 4 — fermer le domaine de valeurs

Une contrainte `CHECK` empêche l'écriture d'une valeur étrangère à l'énumération.

## Préflight obligatoire

Avant déploiement sur une base contenant des données :

```sql
SELECT id, organization_id, name
FROM wards
WHERE id NOT IN (SELECT DISTINCT ward_id FROM rooms)
ORDER BY organization_id, name;
```

Chaque ligne doit être :

- classée explicitement par un script de préparation métier ; ou
- supprimée si elle correspond à une mauvaise configuration.

Aucun script automatique basé sur les mots « caisse », « labo », « pharmacie » ou « urgence » n'est autorisé.

## Rollback

La restauration s'effectue par restauration de sauvegarde de base et redéploiement de la version précédente. La suppression automatique de la colonne n'est pas utilisée comme stratégie de rollback, car le changement modifie le contrat fonctionnel et l'admission.

## Données de référence

La création automatique de services/chambres/lits dans `AdminUserSeeder` est supprimée. Les données de démonstration doivent être créées dans des fixtures ou scripts dédiés aux environnements de test, jamais dans le démarrage applicatif de production.
