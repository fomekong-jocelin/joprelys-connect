# TECHNICAL-DESIGN — Hospitalisation, Facturation et Caisse complètes

## 1. Objectif technique

Compléter les modules existants sans casser le socle actuel, en ajoutant des agrégats métier dédiés pour le séjour hospitalier, les actes facturables, la caisse, les créances et les écritures comptables.

La priorité technique est de sortir les règles critiques du frontend, découper les composants Angular trop volumineux, et rendre les flux financiers auditables.

## 2. Stack concernée

- [x] Spring Boot
- [x] Angular
- [ ] Flutter
- [x] Base de données
- [x] CI/CD
- [x] Documentation

## 3. Contraintes projet obligatoires

- Backend Spring Boot : Maven uniquement (`pom.xml`, `mvnw`).
- Backend Spring Boot : `application.yml` obligatoire, pas de `application.properties`.
- Angular : Tailwind CSS v4 obligatoire, Angular Material interdit sauf ADR.
- Angular : `proxy.conf.json` obligatoire et URLs API relatives.
- Angular : composants sous 500 lignes, alerte dès 300 lignes.
- Documentation fonctionnelle et technique maintenue dès le démarrage.

## 4. Architecture cible

```text
HospitalizationController
  -> HospitalStayService
  -> DailyCareService
  -> OperatingRoomService
  -> DischargeDocumentService
  <- hospitalization / operating_room / care repositories

InvoiceController
  -> BillingService
  -> InvoiceValidationService
  -> ReceivableService
  <- billing repositories

CashRegisterController
  -> CashRegisterService
  -> PaymentReceiptService
  -> CashClosingService
  <- cash repositories

AccountingController
  -> AccountingEntryService
  -> OhadaPostingPolicy
  <- accounting repositories
```

Le backend porte les calculs, validations, statuts et autorisations. Angular affiche, collecte et orchestre uniquement l'expérience utilisateur.

## 5. Fichiers ou modules impactés

| Module | Fichier | Type d'impact |
|---|---|---|
| Backend hospitalisation | `backend/src/main/java/com/joprelys/backend/hospitalization` | Extension séjour, soins, documents |
| Backend billing | `backend/src/main/java/com/joprelys/backend/billing` | Validation facture, devis, créances |
| Backend cash | `backend/src/main/java/com/joprelys/backend/cash` | Nouveau module caisse |
| Backend accounting | `backend/src/main/java/com/joprelys/backend/accounting` | Nouveau socle OHADA |
| Migrations | `backend/src/main/resources/db/migration` | Nouvelles tables |
| Angular hospitalisation | `web/src/app/patient` | Refactor composants et écrans séjour |
| Angular billing | `web/src/app/clinic/billing` | Refactor + écrans caisse/créances |
| i18n | `web/src/assets/i18n/fr.json`, `en.json` | Nouvelles clés |

## 6. Contrats API

| Méthode | Endpoint | Request | Response | Erreurs |
|---|---|---|---|---|
| POST | `/api/hospitalizations/{id}/entry-documents` | Document d'entrée | DocumentResponse | 400, 403, 404 |
| POST | `/api/hospitalizations/{id}/daily-care` | DailyCareRequest | DailyCareResponse | 400, 403, 404, 409 |
| POST | `/api/hospitalizations/{id}/medication-administrations` | MedicationAdministrationRequest | MedicationAdministrationResponse | 400, 403, 404 |
| POST | `/api/operating-room/reports` | OperatingReportRequest | OperatingReportResponse | 400, 403, 404, 409 |
| POST | `/api/billing/estimates` | EstimateRequest | EstimateResponse | 400, 403, 404 |
| POST | `/api/invoices/{id}/validate` | ValidateInvoiceRequest | InvoiceResponse | 400, 403, 404, 409 |
| POST | `/api/invoices/{id}/credit-notes` | CreditNoteRequest | CreditNoteResponse | 400, 403, 404, 409 |
| POST | `/api/cash-registers/{id}/sessions/open` | OpenSessionRequest | CashSessionResponse | 400, 403, 409 |
| POST | `/api/cash-sessions/{id}/payments` | CashPaymentRequest | PaymentReceiptResponse | 400, 403, 404, 409 |
| POST | `/api/cash-sessions/{id}/close` | CloseSessionRequest | CashClosingResponse | 400, 403, 409 |
| GET | `/api/receivables` | Query params | ReceivableResponse[] | 403 |
| GET | `/api/accounting/entries` | Query params | AccountingEntryResponse[] | 403 |

## 7. Modèle de données / migrations

| Élément | Description | Migration requise |
|---|---|---|
| `hospitalization_daily_care` | Soins journaliers structurés | Oui |
| `medication_administrations` | Administration médicaments patient | Oui |
| `patient_consumptions` | Consommables, médicaments, repas rattachés au patient | Oui |
| `consultative_opinions` | Avis consultatifs demandés pendant séjour | Oui |
| `operating_reports` | CRO, anesthésie, équipe, actes K | Oui |
| `surgical_consents` | Consentement opération/anesthésie | Oui |
| `invoice_status_history` | Historique validation, annulation, avoir | Oui |
| `estimates` / `estimate_items` | Devis / proforma | Oui |
| `credit_notes` | Avoirs financiers | Oui |
| `receivables` | Créances patient / assurance | Oui |
| `cash_registers` | Caisses physiques/logiques | Oui |
| `cash_register_sessions` | Ouverture, clôture, fond, écart | Oui |
| `cash_movements` | Recettes, dépenses, transfert caisse-banque | Oui |
| `payment_receipts` | Reçus/tickets numérotés | Oui |
| `accounting_journals` | Journaux OHADA | Oui |
| `accounting_entries` / `accounting_lines` | Écritures et lignes comptables | Oui |

## 8. Configuration

| Paramètre | Fichier | Valeur / source | Environnement |
|---|---|---|---|
| `joprelys.billing.invoice-number-format` | `application.yml` | Format facture clinique | all |
| `joprelys.cash.expense-ceiling` | `application.yml` | `100000` FCFA | all |
| `joprelys.accounting.enabled` | `application.yml` | `true/false` | all |
| API URLs Angular | `proxy.conf.json` + services | Chemins relatifs `/api/...` | dev |

## 9. Sécurité

- [x] Authentification requise.
- [x] Autorisation par rôle requise.
- [x] Inputs validés.
- [x] Requêtes paramétrées.
- [x] Pas de secret dans le code.
- [x] PII masquée dans logs.

Rôles à confirmer ou ajouter : `CAISSIER`, `SECRETAIRE_COMPTABLE`, `DAF`, `MEDECIN_CHEF`.

## 10. Observabilité

- Logs attendus : validation facture, encaissement, annulation, clôture caisse, écart, génération écriture.
- Métriques : montant facturé journalier, encaissement réel, solde créances, écart de caisse.
- Traces : opérations financières multi-étapes.
- Corrélation / request id : conserver le trace id existant.

## 11. Tests prévus

| Niveau | Tests attendus | Commande |
|---|---|---|
| Unit backend | Calcul facture, caisse, statuts, OHADA posting policy | `backend/mvnw test` |
| Integration backend | Controllers hospitalisation, invoices, cash, accounting | `backend/mvnw test` |
| Security backend | RBAC caissier/DAF/soignant | `backend/mvnw test` |
| Angular unit | Composants découpés, facades, états UI | `npm run test` dans `web` |
| Angular build | Compilation production | `npm run build` dans `web` |
| E2E cible | Admission -> soins -> facture -> paiement -> clôture | À planifier |

## 12. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Type de bump | MINOR |
| Justification | Ajout rétrocompatible de modules métier majeurs |
| Breaking change | Non prévu |
| Migration requise | Oui |

## 13. Risques techniques

| Risque | Impact | Mitigation |
|---|---|---|
| Monolithes Angular existants | Régressions et maintenance difficile | STORY-2101 en premier |
| Calculs financiers en double front/back | Incohérence | Backend maître, front lecture/calcul indicatif uniquement |
| Facture validée modifiable | Risque financier | Statuts immuables + audit + avoir |
| Écritures comptables incorrectes | Risque conformité | Valider comptes avec DAF |
| Multi-tenant financier | Fuite de données | `@TenantId`, tests cross-tenant |

## 14. Découpage SOLID et responsabilités

### Couche backend

| Élément | Responsabilité | Interface | Implémentation | Tests |
|---|---|---|---|---|
| Controller | Endpoint HTTP + délégation | N/A | Controllers par module | MockMvc |
| Service | Orchestration métier | Use cases ciblés | Services dédiés | Unit + integration |
| Domain policy | Règles facture/caisse/OHADA | Policies | Implémentations pures | Unit |
| Infrastructure | Persistence | Repositories | JPA | Integration |

### Couche front/mobile

| Élément | Responsabilité | Ce qui est interdit |
|---|---|---|
| Page | Orchestration UI et navigation | Calcul financier source de vérité |
| Component | Affichage réutilisable | Appels HTTP directs |
| Facade/service | Appels API et état présentation | Règle métier critique |

### Vérifications

- [ ] Le backend est maître de la règle métier.
- [ ] Le frontend ne contient pas de logique métier critique.
- [ ] Les controllers ne contiennent aucune logique métier.
- [ ] Les services/use cases sont clairement séparés.
- [ ] Les abstractions sont justifiées.
- [ ] Les limites de taille sont respectées.

## 15. Suivi technique STORY-2101

| Point | Résultat |
|---|---|
| Facturation | `billing-management-page.component.ts` réduit à 315 lignes et template à 288 lignes |
| Hospitalisation | `patient-hospitalization.component.ts` réduit à 335 lignes et template à 280 lignes |
| Composants créés | `BillingAdminTabsComponent`, `BillingInvoiceHistoryComponent`, `BillingPaymentModalComponent` |
| Comportement | Refactor présentation uniquement, API et règles métier inchangées |
| Vérifications | `npm run build` OK ; `npm run test -- --watch=false` OK, 101 tests passés |

## 16. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-08 | Codex | Création du cadrage initial |
| 2026-07-09 | Codex | STORY-2101 : refactor UI facturation/hospitalisation sous les limites de taille |
