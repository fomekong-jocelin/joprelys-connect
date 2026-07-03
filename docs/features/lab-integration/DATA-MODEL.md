# MODELE DE DONNEES — Integration Laboratoire

## Objectif

Maintenir l'alignement entre la table `lab_results` et `LabResultEntity`, y compris sur les bases locales/de developpement ayant deja applique une variante incomplete de `V13__create_lab_results_table.sql`.

## Table `lab_results`

Colonnes attendues par l'entite JPA :

- `id UUID PRIMARY KEY`
- `result_number VARCHAR(50) UNIQUE NOT NULL`
- `lab_order_id UUID NOT NULL`
- `patient_id UUID NOT NULL`
- `organization_id UUID NOT NULL`
- `validator_name VARCHAR(150) NOT NULL`
- `analyte_name VARCHAR(100) NOT NULL`
- `result_value VARCHAR(50) NOT NULL`
- `unit VARCHAR(20)`
- `reference_range VARCHAR(50)`
- `interpretation VARCHAR(20) NOT NULL DEFAULT 'NORMAL'`
- `comment VARCHAR(255)`
- `pdf_file_path VARCHAR(500)`
- `sample_collected_at TIMESTAMP WITH TIME ZONE`
- `result_at TIMESTAMP WITH TIME ZONE`
- `validated_at TIMESTAMP WITH TIME ZONE`
- `created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP`

## Migrations

- `V13__create_lab_results_table.sql` cree la table complete pour les bases neuves.
- `V16__repair_lab_results_schema_validation.sql` repare les bases ou `lab_results` existe deja mais ne contient pas encore toutes les colonnes attendues, notamment `result_value`, ainsi que les colonnes de liaison necessaires aux index.

## Impact SemVer

- Type : PATCH.
- Justification : correction retrocompatible de schema pour permettre le demarrage Hibernate avec `spring.jpa.hibernate.ddl-auto=validate`.
- Breaking change : non.
