# Conception Technique — STORY-2116 : Tests E2E globaux et Accessibilité

Ce document décrit l'implémentation des tests d'intégration de bout en bout et les validations d'accessibilité.

## 1. Plan des Tests d'Intégration E2E (Backend JUnit)

Pour valider l'intégrité fonctionnelle et technique du cycle financier complet, nous allons créer ou étendre la classe de test `FullFinancialE2ETest.java` (dans le package `com.joprelys.backend.billing`).

Ce test d'intégration s'exécutera dans un contexte transactionnel sécurisé, simulant les appels MockMvc successifs avec des tokens JWT distincts selon les rôles (Agent d'accueil, Caissier, DAF).

### Structure du test :
- `shouldExecuteFullFinancialLifecycle()` :
  1. **Phase 1 : Enregistrement Patient & Admission** (Rôle `AGENT_ACCUEIL`).
     - `POST /api/patients` -> Création d'un patient et DPU.
     - `POST /api/visits` -> Admission.
  2. **Phase 2 : Saisie Constantes Vitales** (Rôle `INFIRMIER` / `MEDECIN`).
     - `POST /api/visits/{id}/vitals` -> Enregistrement IMC/Constantes.
  3. **Phase 3 : Consultation & Actes de Soins** (Rôle `MEDECIN`).
     - `POST /api/consultations` -> Écriture consultation, prescription et actes.
  4. **Phase 4 : Facturation Tiers-Payant** (Rôle `AGENT_ACCUEIL`).
     - `POST /api/invoices` -> Facturation d'un acte K à 100 000 FCFA avec convention d'assurance à 80% (Part Patient = 20 000 FCFA, Part Assurance = 80 000 FCFA).
     - Vérification : Création de la créance patient `UNPAID` de 20 000 FCFA.
  5. **Phase 5 : Encaissement & Session de Caisse** (Rôle `CAISSIER`).
     - `POST /api/cash-registers/sessions/open` -> Ouverture session (solde initial = 0).
     - `POST /api/cash-registers/payments` -> Encaissement des 20 000 FCFA espèces du patient.
     - Vérification : Création automatique d'un reçu `REC-*` et d'un mouvement d'entrée espèces.
     - Vérification : Créance patient mise à jour à `PAID`.
  6. **Phase 6 : Bordereau & Recouvrement Assurance** (Rôle `ADMIN_CLINIQUE`).
     - `POST /api/billing/insurance-bordereaux` -> Regroupement de la part assurance.
     - `POST /api/billing/insurance-bordereaux/{id}/send` -> Envoi du bordereau.
     - `POST /api/billing/insurance-bordereaux/{id}/pay` -> Enregistrement du règlement virement de 80 000 FCFA.
     - Vérification : Facture mise à jour au statut `PAID`/`SETTLED`.
  7. **Phase 7 : Versement Banque & Clôture de Session** (Rôle `CAISSIER`).
     - `POST /api/cash-registers/movements` -> Transfert de 15 000 FCFA espèces vers la banque (Virement interne, réf bordereau de dépôt).
     - `POST /api/cash-registers/sessions/close` -> Déclaration de solde physique espèces de 4 000 FCFA (écart = -1 000 FCFA, motif saisi).
     - Vérification : Session passée au statut `CLOSED`.
  8. **Phase 8 : Résolution d'Écart & Export Sage 100** (Rôle `DAF`).
     - `POST /api/cash-registers/sessions/{id}/resolve-discrepancy` -> Enregistrement des notes de traitement de l'écart.
     - `GET /api/accounting/export` -> Téléchargement du CSV.
     - Vérification : Formatage des lignes comptables et équilibre Débit/Crédit global.

## 2. Accessibilité & Standards Frontend (Angular)

Pour garantir que les composants UI respectent les standards d'accessibilité (contraste, focus, structure sémantique), nous intégrons des assertions d'accessibilité dans les tests unitaires et vérifions les éléments de design.

### Liste des assertions d'accessibilité :
1. **Focus visible** : S'assurer que les boutons clés du tableau de bord possèdent la classe `focus:ring-*` et `focus:outline-none`.
2. **ARIA Labels** : Les boutons iconographiques d'impression de reçus et de téléchargement d'exports doivent contenir un attribut `aria-label` descriptif pour les synthétiseurs vocaux.
3. **HTML sémantique** : Les tableaux d'historique de caisse et de créances doivent utiliser `<table class="...">` avec `<thead>`, `<tbody>`, `<th>` et `<td>` structurés.
