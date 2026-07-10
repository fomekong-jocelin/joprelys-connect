-- V55 : Migration des colonnes de montants billing de FLOAT8 vers NUMERIC
-- Aligne le schéma DB avec la migration Java Double vers BigDecimal.
--
-- La syntaxe reste volontairement portable entre PostgreSQL et H2 :
-- - aucune clause PostgreSQL USING ;
-- - aucun cast PostgreSQL ::NUMERIC ;
-- - une modification de colonne par instruction.

-- Préserver la normalisation prévue par la version initiale de V55.
UPDATE invoices
SET discount_amount = 0
WHERE discount_amount IS NULL;

UPDATE invoice_items
SET coefficient = 1
WHERE coefficient IS NULL;

-- -------------------------------------------------------
-- Table : invoices
-- -------------------------------------------------------
ALTER TABLE invoices ALTER COLUMN total_amount SET DATA TYPE NUMERIC(19,4);
ALTER TABLE invoices ALTER COLUMN patient_share SET DATA TYPE NUMERIC(19,4);
ALTER TABLE invoices ALTER COLUMN insurance_share SET DATA TYPE NUMERIC(19,4);
ALTER TABLE invoices ALTER COLUMN discount_amount SET DATA TYPE NUMERIC(19,4);

-- -------------------------------------------------------
-- Table : invoice_items
-- -------------------------------------------------------
ALTER TABLE invoice_items ALTER COLUMN unit_price SET DATA TYPE NUMERIC(19,4);
ALTER TABLE invoice_items ALTER COLUMN quantity SET DATA TYPE NUMERIC(19,4);
ALTER TABLE invoice_items ALTER COLUMN coefficient SET DATA TYPE NUMERIC(19,4);
ALTER TABLE invoice_items ALTER COLUMN total_item_amount SET DATA TYPE NUMERIC(19,4);

-- -------------------------------------------------------
-- Table : payments
-- -------------------------------------------------------
ALTER TABLE payments ALTER COLUMN amount SET DATA TYPE NUMERIC(19,4);

-- -------------------------------------------------------
-- Table : receivables
-- -------------------------------------------------------
ALTER TABLE receivables ALTER COLUMN total_amount SET DATA TYPE NUMERIC(19,4);
ALTER TABLE receivables ALTER COLUMN paid_amount SET DATA TYPE NUMERIC(19,4);

-- -------------------------------------------------------
-- Table : tariff_grid
-- -------------------------------------------------------
ALTER TABLE tariff_grid ALTER COLUMN unit_value SET DATA TYPE NUMERIC(19,4);

-- -------------------------------------------------------
-- Table : insurance_conventions
-- -------------------------------------------------------
ALTER TABLE insurance_conventions ALTER COLUMN coverage_percentage SET DATA TYPE NUMERIC(5,4);
