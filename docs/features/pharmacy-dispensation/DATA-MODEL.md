# MODELE DE DONNEES — Dispensation en Pharmacie

## Objectif

Garantir que le schema PostgreSQL reste aligne avec les entites JPA du module pharmacie, y compris sur les bases locales ayant deja applique une premiere variante incomplete de `V14__add_pharmacy_fields_and_tables.sql`.

## Tables et colonnes concernees

### `prescriptions`

- `prescription_number VARCHAR(50)` : identifiant metier unique de l'ordonnance.
- `pin_code VARCHAR(4)` : code de verification pharmacie.
- `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'` : cycle de vie de l'ordonnance.
- `expires_at TIMESTAMP WITH TIME ZONE` : expiration de validite.

Index attendu :

- `uk_prescriptions_prescription_number` : unicite de `prescription_number`.

### `prescription_dispensations`

Table de suivi des actes de dispensation :

- `id UUID PRIMARY KEY`
- `prescription_id UUID NOT NULL`
- `dispensed_at TIMESTAMP WITH TIME ZONE NOT NULL`
- `pharmacy_name VARCHAR(200) NOT NULL`
- `pharmacist_license VARCHAR(50) NOT NULL`
- `created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP`

### `dispensation_items`

Table de detail des lignes dispensees :

- `id UUID PRIMARY KEY`
- `dispensation_id UUID NOT NULL`
- `prescription_item_id UUID NOT NULL`
- `quantity_dispensed INT NOT NULL`
- `substituted_with VARCHAR(200)`

## Migrations

- `V14__add_pharmacy_fields_and_tables.sql` introduit les champs et tables pharmacie pour les bases neuves.
- `V15__repair_pharmacy_schema_validation.sql` est une migration additive et idempotente qui repare les bases locales/de developpement ayant deja applique une variante incomplete de `V14`.

## Impact SemVer

- Type : PATCH.
- Justification : correction retrocompatible de schema pour permettre le demarrage Hibernate avec `spring.jpa.hibernate.ddl-auto=validate`.
- Breaking change : non.
