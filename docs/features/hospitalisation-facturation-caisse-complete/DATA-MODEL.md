# DATA-MODEL — Hospitalisation, Facturation et Caisse complètes

## 1. Vue d'ensemble

Le modèle cible complète les tables existantes `hospitalizations`, `hospitalization_notes`, `invoices`, `invoice_items`, `payments`, `insurance_conventions`, `tariff_grid`, `wards`, `rooms`, `beds` et `bed_assignments`.

Les nouvelles tables doivent rester multi-tenant via `organization_id` et compatibles avec Flyway.

## 2. Tables / collections

| Table | Description | Nouvelle / existante |
|---|---|---|
| `hospitalizations` | Séjours hospitaliers | Existante à enrichir |
| `hospitalization_daily_care` | Feuille de soins journalière structurée | Nouvelle |
| `medication_administrations` | Administration horodatée des médicaments | Nouvelle |
| `patient_consumptions` | Consommables, médicaments, repas liés au patient | Nouvelle |
| `consultative_opinions` | Demandes et réponses d'avis consultatif | Nouvelle |
| `operating_reports` | Compte rendu opératoire / anesthésie | Nouvelle |
| `operating_report_implants` | Implants et matériel utilisé au bloc | Nouvelle |
| `surgical_consents` | Consentements opération/anesthésie | Nouvelle |
| `estimates` | Devis/proforma | Nouvelle |
| `estimate_items` | Lignes de devis | Nouvelle |
| `invoices` | Factures patient | Existante à enrichir |
| `invoice_status_history` | Historique des statuts de facture | Nouvelle |
| `credit_notes` | Avoirs et annulations financières | Nouvelle |
| `receivables` | Créances patient / assurance | Nouvelle |
| `payments` | Paiements | Existante à rattacher aux sessions caisse |
| `payment_receipts` | Reçus et tickets de caisse | Nouvelle |
| `cash_registers` | Caisses physiques/logiques | Nouvelle |
| `cash_register_sessions` | Sessions d'ouverture/clôture | Nouvelle |
| `cash_movements` | Recettes, dépenses, transferts | Nouvelle |
| `accounting_journals` | Journaux comptables OHADA | Nouvelle |
| `accounting_entries` | Écritures comptables | Nouvelle |
| `accounting_lines` | Lignes débit/crédit | Nouvelle |

## 3. Colonnes / champs principaux

| Table | Colonne | Type | Nullable | Index | Description |
|---|---|---|---|---|---|
| `hospitalization_daily_care` | `hospitalization_id` | UUID | Non | Oui | Séjour rattaché |
| `hospitalization_daily_care` | `care_type` | VARCHAR(50) | Non | Oui | Type de soin |
| `hospitalization_daily_care` | `billable` | BOOLEAN | Non | Non | Génère ligne facturable |
| `medication_administrations` | `prescription_item_id` | UUID | Oui | Oui | Prescription source |
| `medication_administrations` | `administered_at` | TIMESTAMP | Non | Oui | Date d'administration |
| `patient_consumptions` | `source_type` | VARCHAR(50) | Non | Oui | Médicament, consommable, repas |
| `operating_reports` | `status` | VARCHAR(30) | Non | Oui | DRAFT, VALIDATED, CANCELLED |
| `operating_reports` | `k_surgeon` | DOUBLE | Oui | Non | Coefficient chirurgien |
| `operating_reports` | `k_anesthetist` | DOUBLE | Oui | Non | Coefficient anesthésiste |
| `operating_reports` | `k_bloc` | DOUBLE | Oui | Non | Coefficient bloc |
| `estimates` | `estimate_number` | VARCHAR(50) | Non | Unique | Numéro proforma |
| `invoices` | `validated_at` | TIMESTAMP | Oui | Oui | Date validation immuable |
| `invoices` | `validated_by_user_id` | UUID | Oui | Oui | Validateur |
| `payments` | `cash_session_id` | UUID | Oui | Oui | Session de caisse |
| `payment_receipts` | `receipt_number` | VARCHAR(50) | Non | Unique | Numéro reçu |
| `cash_register_sessions` | `status` | VARCHAR(30) | Non | Oui | OPEN, CLOSED, CANCELLED |
| `cash_register_sessions` | `opening_float` | DOUBLE | Non | Non | Fond de caisse |
| `cash_register_sessions` | `declared_cash_amount` | DOUBLE | Oui | Non | Montant déclaré |
| `cash_register_sessions` | `difference_amount` | DOUBLE | Oui | Non | Écart |
| `accounting_lines` | `account_number` | VARCHAR(20) | Non | Oui | Compte OHADA |
| `accounting_lines` | `debit_amount` | DOUBLE | Non | Non | Débit |
| `accounting_lines` | `credit_amount` | DOUBLE | Non | Non | Crédit |

## 4. Migrations

| Migration | Type | Backward compatible | Rollback |
|---|---|---|---|
| `V46__hospitalization_complete_stay_tables.sql` | Ajout tables séjour/soins/bloc | Oui | Drop tables si non utilisées |
| `V47__billing_estimates_receivables.sql` | Ajout devis, créances, statuts facture | Oui | Drop tables et colonnes ajoutées |
| `V48__cash_register_tables.sql` | Ajout caisse et reçus | Oui | Drop tables si aucune donnée prod |
| `V49__accounting_ohada_minimal_tables.sql` | Ajout journaux et écritures | Oui | Drop tables si aucune donnée prod |

## 5. Contraintes et index

- Indexer tous les `organization_id`, `patient_id`, `hospitalization_id`, `invoice_id`, `cash_session_id`.
- Contrainte unique par tenant sur les numéros métier : facture, reçu, devis, session.
- Interdire plusieurs sessions de caisse ouvertes pour le même caissier et la même caisse.
- Interdire le paiement hors session ouverte dès que le module caisse est activé.
- Interdire la modification des lignes d'une facture validée.

## 6. Données sensibles

- [x] Données personnelles identifiées.
- [x] Données financières sensibles.
- [x] Masquage logs prévu.
- [x] Rétention prévue à définir avec conformité locale.
- [ ] Chiffrement applicatif spécifique à arbitrer.

## 7. Historique

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-08 | Codex | Création du modèle de données cible |
