# TECHNICAL-DESIGN — Facturation Médicale (Actes K & Conventions)

## 1. Objectif technique

Concevoir l'architecture de données et de services pour gérer les conventions d'assurances, la grille des tarifs, la génération et le calcul sécurisé des factures basées sur les coefficients K et les nuitées, ainsi que l'enregistrement des règlements de caisse.

## 2. Stack concernée

- [x] Spring Boot (REST API, JPA, Spring Security)
- [x] Angular (Tailwind CSS v4, billing table, modal de paiement)
- [x] Base de données (Flyway migrations, PostgreSQL/H2)
- [x] PDF Generation (PdfGeneratorService / iText)

## 3. Architecture cible

Création d'un module indépendant `com.joprelys.backend.billing` :

```text
InvoiceController (REST API)
  ➔ BillingService (Calcul et génération de factures)
    ➔ InvoiceEntity / InvoiceItemEntity (Modèle de données)
    ➔ InsuranceConventionEntity (Gestion des assureurs)
    ➔ TariffGridEntity (Tarifs K / AMI)
  ➔ PaymentController (REST API)
    ➔ PaymentService (Gestion de caisse et recettes)
```

## 4. Modèle de données / migrations

### Migration SQL Flyway (`V45__create_billing_tables.sql`)

```sql
CREATE TABLE insurance_conventions (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    coverage_percentage DOUBLE PRECISION NOT NULL DEFAULT 0.8, -- ex: 0.8 pour 80%
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE tariff_grid (
    id UUID PRIMARY KEY,
    key_letter VARCHAR(10) NOT NULL, -- K, AMI, CS
    unit_value DOUBLE PRECISION NOT NULL, -- ex: 1000 FCFA
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_key_letter_org UNIQUE (key_letter, organization_id)
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    visit_id UUID,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    insurance_convention_id UUID REFERENCES insurance_conventions(id),
    total_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    patient_share DOUBLE PRECISION NOT NULL DEFAULT 0.0, -- part ticket modérateur
    insurance_share DOUBLE PRECISION NOT NULL DEFAULT 0.0, -- part tiers payant
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PARTIALLY_PAID, PAID
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE invoice_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    label VARCHAR(250) NOT NULL,
    item_type VARCHAR(50) NOT NULL, -- CONSULTATION, K_SURGEON, K_ANESTHESIST, K_BLOC, AMI_CARE, STAY_FEE, MEDICATION
    unit_price DOUBLE PRECISION NOT NULL,
    quantity DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    coefficient DOUBLE PRECISION, -- valeur du K ou AMI si applicable
    total_item_amount DOUBLE PRECISION NOT NULL,
    organization_id UUID NOT NULL
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    amount DOUBLE PRECISION NOT NULL,
    payment_method VARCHAR(50) NOT NULL, -- CASH, CHECK, BANK_TRANSFER
    reference_number VARCHAR(100),
    received_by_user_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

## 5. Contrats API

| Méthode | Endpoint | Request | Response | Erreurs / Statuts |
|---|---|---|---|---|
| `POST` | `/api/invoices` | `{ "patientId": "...", "visitId": "...", "insuranceId": "..." }` | `InvoiceResponse` | `201 Created` |
| `POST` | `/api/invoices/{id}/payments` | `{ "amount": 15000, "method": "CASH", "reference": null }` | `PaymentResponse` | `200 OK`, `400 Bad Request` |
| `GET` | `/api/invoices/{id}/pdf` | N/A | Fichier PDF octet-stream | `200 OK`, `404 Not Found` |

## 6. Algorithme de calcul de facture

Pour une hospitalisation avec chirurgie :
1. Récupérer les jours d'hospitalisation : `Séjour = (DateSortie - DateEntree)`.
2. Calculer le tarif lit : `StayFee = Séjour * Tarif_Chambre`.
3. Récupérer les coefficients K saisis (Chirurgien, Anesthésiste, Bloc) et appliquer la valeur unitaire de la lettre clé K de la grille tarifaire.
4. Récupérer les ordonnances et médicaments dispensés liés à la visite.
5. Calculer le total `T`.
6. Si une convention d'assurance est sélectionnée (ex: AXA - 80%):
   - `InsuranceShare = T * 0.8`
   - `PatientShare = T * 0.2`
7. Générer l'entité `InvoiceEntity` et ses sous-items associés.

## 7. Versioning & SemVer

- **MINOR bump** : Ajout d'une nouvelle fonctionnalité majeure rétrocompatible.

## 8. Responsive navigation

La barre d'onglets de `BillingManagementPageComponent` utilise un conteneur horizontal défilable (`overflow-x-auto`). Les boutons ont une largeur intrinsèque (`shrink-0`, `whitespace-nowrap`) et une hauteur minimale adaptée au tactile. Cette stratégie conserve tous les libellés sans imposer une navigation secondaire ou modifier le contrat de la page.
