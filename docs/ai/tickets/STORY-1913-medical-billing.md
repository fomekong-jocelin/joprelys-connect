# STORY-1913 — Facturation Médicale (Actes K & Conventions)

## 1. Description et contexte

**Epic** : EPIC-0014 — Alignement des modules 4 à 12 du CDC
**Titre** : Facturation Médicale (Actes K & Conventions)
**Statut** : **DONE**
**Assigné à** : Antigravity
**Priorité** : P1
**Sprint** : SPRINT-0012
**SP** : 8
**Profil recommandé** : Senior Full-stack
**Estimation** : 3.5j (Senior: 2.5j, Junior: 6.0j)

## 2. Objectifs

Permettre de gérer la tarification des actes d'urgences, les tarifs K (Chirurgien, Anesthésiste, Bloc), les frais d'hospitalisation à la nuitée, le Tiers Payant (Assurances & Conventions) et l'impression des factures certifiées au format PDF.

## 3. Critères d'acceptation (DoD)

- [ ] Script de migration SQL Flyway `V45__create_billing_tables.sql` pour les tables `insurance_conventions`, `tariff_grid`, `invoices`, `invoice_items` et `payments`.
- [ ] Entités JPA avec multi-tenancy (`@TenantId`).
- [ ] Calculateur automatique des actes K (lettre clé * Valeur unitaire) et nuitées de séjour (différence de date * prix de chambre).
- [ ] Prise en compte de la grille des conventions d'assurances pour le calcul de la part assurance (Tiers Payant) et part patient.
- [ ] Endpoints REST CRUD dans `InvoiceController` et `PaymentController`.
- [ ] Modèle PDF de facture (avec numéro séquentiel unique, répartition des montants, signatures et QR code de vérification).
- [ ] Interface frontend de facturation Angular sous Tailwind CSS v4, permettant d'éditer, payer (partiellement ou totalement) et imprimer la facture.
- [ ] Traductions FR/EN complètes.
- [ ] Tests unitaires et d'intégration backend/frontend au vert.

## 4. Plan de découpage en sous-tâches

### Subtasks Techniques

- [x] **SUB-1913-01** : Script SQL de migration et configuration des entités JPA (factures, conventions, tarifs).
- [x] **SUB-1913-02** : Implémentation du moteur de calcul financier (BillingService) et des validations.
- [x] **SUB-1913-03** : Création du service de génération de facture PDF certifiée.
- [x] **SUB-1913-04** : Création des controllers REST `/api/invoices/*` et `/api/payments/*` avec tests d'intégration.
- [x] **SUB-1913-05** : Développement de l'IHM Angular de facturation et de gestion des règlements de caisse.
- [x] **SUB-1913-06** : Intégration i18n et tests de validation de calculs monétaires.

## 5. Définition de Prêt (DoR)

- [x] Spécifications fonctionnelles rédigées dans `docs/features/medical-billing/FUNCTIONAL-SPEC.md`.
- [x] Conception technique rédigée dans `docs/features/medical-billing/TECHNICAL-DESIGN.md`.
- [x] Modèle de données SQL validé.

## 6. Définition de Fini (DoD)

- [x] Critères d'acceptation validés.
- [x] Tous les tests passent avec succès (backend Maven et frontend Vitest).
- [x] Documentation mise à jour et changelog complété.
- [x] Review de code faite et approuvée par le Reviewer.

## 7. Notes de correction UI (2026-07-08)

- Remplacement des emojis par le composant d'icônes `app-ui-icon`.
- Utilisation de `ui-card-subtle` et `ui-card-muted` pour respecter les ombres et arrondis du design system.
- Correction du radius des boutons et inputs (4px via `--radius-brand-sm`).
- Internationalisation complète des labels, placeholders et options des onglets facturation, conventions et tarifs.
- Ajout des clés de traduction manquantes en FR/EN.

**Reviewer** : Lead Developer / DAF
