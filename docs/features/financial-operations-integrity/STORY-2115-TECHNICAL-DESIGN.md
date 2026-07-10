# Conception Technique — STORY-2115 (Pilotage DAF & Exports OHADA)

## 1. Architecture du Module

Le module s'appuie sur les tables opérationnelles existantes pour extraire, formater et projeter les écritures comptables sous forme de flux CSV.

```
[Client Angular DAF]
        │ (HTTP REST)
        ▼
[AccountingController] ──► [AccountingExportService]
                                   │
             ┌─────────────────────┼─────────────────────┐
             ▼                     ▼                     ▼
      [InvoiceRepo]          [PaymentRepo]       [CashSessionRepo]
```

## 2. API Contract

### A. Export comptable OHADA
- **Route** : `GET /api/accounting/export`
- **Paramètres Query** :
  - `startDate` (string, format `YYYY-MM-DD`, optionnel)
  - `endDate` (string, format `YYYY-MM-DD`, optionnel)
- **Habilitations** : Rôles `DAF` ou `ADMIN_CLINIQUE` uniquement (bloqué en 403 sinon).
- **Format de Réponse** : Fichier attaché au format CSV (`text/csv`).

### B. Supervision des sessions de caisse
- **Route** : `GET /api/cash-registers/sessions`
- **Habilitations** : Rôles `DAF` ou `ADMIN_CLINIQUE` (bloqué en 403 sinon).
- **Réponse** : Liste paginée ou liste complète de toutes les sessions de caisse du tenant.

### C. Résolution des écarts de caisse
- **Route** : `POST /api/cash-registers/sessions/{id}/resolve-discrepancy`
- **Request Body** :
  ```json
  {
    "resolutionNotes": "Explication et validation de l'écart après vérification du reçu N° 23."
  }
  ```
- **Habilitations** : Rôles `DAF` ou `ADMIN_CLINIQUE` uniquement.
- **Réponse** : La session mise à jour.

---

## 3. Modèle de données & Ratios

Pour stocker la note de résolution d'écart par le DAF sur la table `cash_register_sessions` :
Nous allons ajouter deux colonnes sur la table `cash_register_sessions` :
- `discrepancy_resolved` (boolean, default false)
- `resolution_notes` (text, nullable)
- `resolved_by_user_id` (uuid, nullable)
- `resolved_at` (timestamp, nullable)

### Script de migration Flyway cible (`V54__add_cash_session_resolution_fields.sql`) :
```sql
ALTER TABLE cash_register_sessions ADD COLUMN discrepancy_resolved BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE cash_register_sessions ADD COLUMN resolution_notes VARCHAR(1000);
ALTER TABLE cash_register_sessions ADD COLUMN resolved_by_user_id VARCHAR(36);
ALTER TABLE cash_register_sessions ADD COLUMN resolved_at TIMESTAMP WITH TIME ZONE;
```

---

## 4. Logique d'Exportation Comptable (`AccountingExportService`)

La méthode `exportToCsv(Instant start, Instant end)` réalise les étapes suivantes et projette le format d'import standard de **Sage 100 Comptabilité** :

### A. Structure des colonnes de l'export Sage 100
- **`Journal`** : Code journal sur 2 à 3 caractères (`VT` = Ventes, `CA` = Caisse, `BQ` = Banque).
- **`Date`** : Date au format `DDMMYY`.
- **`CompteGeneral`** : Compte général OHADA (ex: `41110000`, `70610000`).
- **`CompteTiers`** : Compte tiers (Patient ID ou Assurance Code).
- **`RefPiece`** : Numéro de la facture, reçu ou session.
- **`Libelle`** : Description de l'écriture (tronquée à 30 caractères max).
- **`Debit`** : Montant débité.
- **`Credit`** : Montant crédité.

### B. Schéma de projection des écritures

1. **Journal des Ventes (Factures validées)** :
   - Sélectionne toutes les factures validées entre `start` et `end`.
   - Pour chaque facture, si `patientShare > 0` :
     - Ligne Débit Compte `411100` (Client Patient) pour `patientShare`.
     - Ligne Crédit Compte `706100` (Prestations médicales) pour `patientShare`.
   - Si `insuranceShare > 0` :
     - Ligne Débit Compte `411200` (Client Assurance) pour `insuranceShare`.
     - Ligne Crédit Compte `706100` (Prestations médicales) pour `insuranceShare`.

2. **Journal de Caisse (Règlements & Écarts)** :
   - Sélectionne tous les paiements perçus en caisse.
   - Pour chaque paiement, si mode de règlement = `CASH` :
     - Ligne Débit Compte `571100` (Caisse Principale) / Crédit Compte `411100` (Client Patient) pour `amount`.
   - Sélectionne toutes les sessions clôturées présentant un écart :
     - Si $\text{écart} < 0$ (déficit) : Débit `656000` (Pertes sur écarts) / Crédit `571100` (Caisse Principale) pour le montant absolu.
     - Si $\text{écart} > 0$ (excédent) : Débit `571100` (Caisse Principale) / Crédit `756000` (Gains sur écarts) pour le montant de l'écart.

3. **Journal de Banque (Virements & Bordereaux)** :
   - Sélectionne tous les mouvements de type `TRANSFER_TO_BANK` validés :
     - Débit `585000` (Virements internes) / Crédit `571100` (Caisse Principale).
     - Lors du dépôt effectif en banque : Débit `521100` (Banque) / Crédit `585000` (Virements internes).
   - Sélectionne tous les règlements de bordereaux d'assurance :
     - Débit `521100` (Banque) / Crédit `411200` (Client Assurance) pour le montant réglé par l'assureur.

4. **Gestion des annulations et chèques rejetés (Contre-passation)** :
   - Lors de l'annulation d'un paiement (ex: rejet de chèque) : le système génère une écriture de contre-passation (Crédit Caisse `571100` / Débit Client Patient `411100`) dans le journal de caisse pour annuler le règlement initial, et remet la créance du patient en `UNPAID` ou `PARTIALLY_PAID` de manière transparente et conforme aux principes d'audit.
