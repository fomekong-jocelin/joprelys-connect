# STORY-2115 — Pilotage DAF: validation de caisse, exceptions et exports comptables OHADA

> Ticket d'ingénierie préparé et mis au statut READY suite à la complétion de la STORY-2114.

## 1. Objectif

Permettre à la Direction Administrative et Financière (DAF) de superviser l'ensemble des sessions de caisses de la clinique (en cours, fermées, avec ou sans écarts), de valider les justifications d'écarts de clôture et de générer un export normé des écritures comptables selon le plan de compte OHADA cible pour intégration dans la comptabilité générale externe.

## 2. Critères d'acceptation

- [x] Tableau de bord centralisé pour le DAF listant l'historique global des sessions de caisse (statut, caissier, solde initial, solde attendu, solde déclaré, écart et motif).
- [x] Possibilité pour le DAF de valider ou de traiter un écart de caisse constaté.
- [x] Génération d'un export au format standard CSV/Excel des écritures comptables sur une période donnée selon le plan de compte OHADA cible :
  - **Validation Facture (Part Patient)** : Débit `411100` / Crédit `706100` (Journal Ventes).
  - **Validation Facture (Tiers-Payant)** : Débit `411200` / Crédit `706100` (Journal Ventes).
  - **Règlement Patient (Caisse)** : Débit `571100` / Crédit `411100` (Journal Caisse).
  - **Versement Banque (Espèces)** : Débit `585000` / Crédit `571100` (Journal Caisse).
  - **Validation Versement (Banque)** : Débit `521100` / Crédit `585000` (Journal Banque).
  - **Règlement Assurance (Bordereau)** : Débit `521100` / Crédit `411200` (Journal Banque).
  - **Déficit de Caisse (Clôture)** : Débit `656000` / Crédit `571100` (Journal Caisse).
  - **Excédent de Caisse (Clôture)** : Débit `571100` / Crédit `756000` (Journal Caisse).
- [x] Sécurisation stricte de l'accès aux fonctionnalités de pilotage et d'export (autorisé uniquement pour le rôle `DAF` et `ADMIN_CLINIQUE`).

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0018 - Intégrité financière et poste facturation/caisse |
| User story parent | STORY-2115 |
| Sprint cible | SPRINT-0013 |
| Priorité business | P1 |
| Complexité | M |
| Story points | 5 |
| Profil recommandé | Senior full-stack + DAF |
| Effort estimé senior | 1.0 j |
| Effort estimé intermédiaire | 1.3 j |
| Effort estimé junior | 2.0 j |
| Responsable | Codex |
| Reviewer obligatoire | Lead Developer + DAF |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | STORY-2113 (sessions caisses) et STORY-2112 |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé
- [x] Spécifications fonctionnelles dans `DAF-VALIDATION.md` analysées
- [x] Backend Maven uniquement vérifié
- [x] Backend `application.yml` / profils YAML vérifiés
- [x] Frontend Tailwind CSS v4 vérifié
- [x] Absence Angular Material vérifiée

## 5. Hypothèses

- L'export comptable doit extraire les écritures sous forme de lignes comptables (Date, Journal, N° de Compte, Libellé de Compte, Débit, Crédit, Référence/Pièce, Libellé Écritures).
- Seuls les événements validés/clôturés génèrent des écritures (les brouillons et devis ne sont pas exportés).

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Incompatibilité de l'export avec le logiciel comptable final | Moyen | Proposer un format CSV standardisé et configurable facilement (Sage / Odoo). |

## 7. Action plan

- [x] Créer le script de migration ou de persistance pour les écritures comptables si besoin d'un historique stocké (ou génération à la volée sur les tables existantes).
- [x] Implémenter le service backend d'export comptable OHADA et les endpoints REST associés.
- [x] Créer les tests unitaires et d'intégration MockMvc pour le contrôleur comptable.
- [x] Implémenter le service Angular et les écrans de supervision pour la console DAF.
- [x] Ajouter l'option de téléchargement du CSV/Excel de grand livre.
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 8. Implémentation réalisée

- Migration Flyway `V54` créée pour stocker le statut et les notes de résolution d'écart.
- Entité `CashRegisterSessionEntity` mise à jour avec les getters/setters et champs JPA correspondants.
- Implémentation de `AccountingExportService` et `AccountingController` pour la génération et le téléchargement du grand livre comptable sous format standard CSV de Sage 100.
- Intégration de la logique de supervision des sessions de caisse globales et de traitement des écarts.
- Écriture d'un test d'intégration complet `AccountingControllerTest.java` (couverture RBAC et format CSV Sage).
- Création de `BillingDafDashboardComponent` côté Angular avec modales de traitement des écarts et formulaire d'export, restreint aux rôles DAF et ADMIN_CLINIQUE.
- Validation finale réussie par compilation de production Angular et Vitest (101/101 tests réussis).

## 9. Statut final

Statut : **DONE**
