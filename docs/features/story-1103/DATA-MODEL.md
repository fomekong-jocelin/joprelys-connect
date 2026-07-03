# STORY-1103 — Modèle de données

## Table `drug_stocks`

### DDL (Flyway V17)

```sql
CREATE TABLE IF NOT EXISTS drug_stocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    drug_name VARCHAR(255) NOT NULL,
    generic_name VARCHAR(255),
    unit VARCHAR(50) NOT NULL DEFAULT 'comprime',
    quantity_available INTEGER NOT NULL DEFAULT 0 CHECK (quantity_available >= 0),
    minimum_threshold INTEGER NOT NULL DEFAULT 10,
    batch_number VARCHAR(100),
    expiry_date DATE,
    supplier VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

### Index

| Nom | Colonnes | Objectif |
|---|---|---|
| `idx_drug_stocks_organization_id` | `organization_id` | Filtrage multi-tenant rapide |
| `idx_drug_stocks_drug_name` | `(organization_id, lower(drug_name))` | Recherche insensible à la casse par tenant |

### Description des colonnes

| Colonne | Type | Nullable | Défaut | Description |
|---|---|---|---|---|
| `id` | UUID | NON | gen_random_uuid() | Clé primaire |
| `organization_id` | UUID | NON | — | Tenant ID (clinique) — géré par Hibernate `@TenantId` |
| `version` | BIGINT | NON | 0 | Compteur de verrouillage optimiste JPA (`@Version`) |
| `drug_name` | VARCHAR(255) | NON | — | Nom commercial du médicament |
| `generic_name` | VARCHAR(255) | OUI | — | Nom générique (DCI) |
| `unit` | VARCHAR(50) | NON | `comprime` | Unité de mesure (comprimé, flacon, ampoule…) |
| `quantity_available` | INTEGER | NON | 0 | Quantité physique disponible (≥ 0) |
| `minimum_threshold` | INTEGER | NON | 10 | Seuil d'alerte de stock bas |
| `batch_number` | VARCHAR(100) | OUI | — | Numéro de lot |
| `expiry_date` | DATE | OUI | — | Date de péremption |
| `supplier` | VARCHAR(255) | OUI | — | Nom du fournisseur |
| `created_at` | TIMESTAMPTZ | NON | NOW() | Date de création |
| `updated_at` | TIMESTAMPTZ | NON | NOW() | Date de dernière modification |

## Diagramme de relation

```
organizations (1) ──────────< drug_stocks (N)
  id                            organization_id [FK implicite via @TenantId]
```

> Pas de contrainte FK déclarée en DDL pour préserver la compatibilité avec le filtrage Hibernate multi-tenant par session. L'intégrité est garantie applicativement par le `TenantContext`.

## Valeurs d'unité recommandées

| Valeur | Label affiché |
|---|---|
| `comprime` | Comprimé |
| `gelule` | Gélule |
| `flacon` | Flacon |
| `ampoule` | Ampoule |
| `sachet` | Sachet |
| `tube` | Tube |
| `seringue` | Seringue |
