-- V55 : Migration des colonnes de montants billing de FLOAT8 → NUMERIC(19,4)
-- Aligne le schéma DB avec la migration Java Double → BigDecimal (PR #2)

-- -------------------------------------------------------
-- Table : invoices
-- -------------------------------------------------------
ALTER TABLE invoices
    ALTER COLUMN total_amount     TYPE NUMERIC(19,4) USING total_amount::NUMERIC(19,4),
    ALTER COLUMN patient_share    TYPE NUMERIC(19,4) USING patient_share::NUMERIC(19,4),
    ALTER COLUMN insurance_share  TYPE NUMERIC(19,4) USING insurance_share::NUMERIC(19,4),
    ALTER COLUMN discount_amount  TYPE NUMERIC(19,4) USING COALESCE(discount_amount, 0)::NUMERIC(19,4);

-- -------------------------------------------------------
-- Table : invoice_items
-- -------------------------------------------------------
ALTER TABLE invoice_items
    ALTER COLUMN unit_price        TYPE NUMERIC(19,4) USING unit_price::NUMERIC(19,4),
    ALTER COLUMN quantity          TYPE NUMERIC(19,4) USING quantity::NUMERIC(19,4),
    ALTER COLUMN coefficient       TYPE NUMERIC(19,4) USING COALESCE(coefficient, 1)::NUMERIC(19,4),
    ALTER COLUMN total_item_amount TYPE NUMERIC(19,4) USING total_item_amount::NUMERIC(19,4);

-- -------------------------------------------------------
-- Table : payments
-- -------------------------------------------------------
ALTER TABLE payments
    ALTER COLUMN amount TYPE NUMERIC(19,4) USING amount::NUMERIC(19,4);

-- -------------------------------------------------------
-- Table : receivables
-- -------------------------------------------------------
ALTER TABLE receivables
    ALTER COLUMN total_amount TYPE NUMERIC(19,4) USING total_amount::NUMERIC(19,4),
    ALTER COLUMN paid_amount  TYPE NUMERIC(19,4) USING paid_amount::NUMERIC(19,4);

-- -------------------------------------------------------
-- Table : tariff_grid
-- -------------------------------------------------------
ALTER TABLE tariff_grid
    ALTER COLUMN unit_value TYPE NUMERIC(19,4) USING unit_value::NUMERIC(19,4);

-- -------------------------------------------------------
-- Table : insurance_conventions
-- -------------------------------------------------------
ALTER TABLE insurance_conventions
    ALTER COLUMN coverage_percentage TYPE NUMERIC(5,4) USING coverage_percentage::NUMERIC(5,4);
