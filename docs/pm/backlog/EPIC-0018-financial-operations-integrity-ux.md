# EPIC-0018 - Intégrité financière et poste facturation/caisse

Référence complète: `docs/ai/tickets/EPIC-0018-financial-operations-integrity-ux.md`.

## Tableau de suivi du Backlog

| Story | Priorité | SP | Estimation senior | Dépendance | Statut |
|---|---|---:|---:|---|---|
| STORY-2111 - Synchronisation règlement/créance | P0 | 3 | 0.8j | Revue DAF | **DONE** |
| STORY-2112 - État financier ventilé | P0 | 5 | 1.2j | Décision DAF | **DONE** |
| STORY-2113 - Poste caissier | P0 | 8 | 2.0j | STORY-2112 | **DONE** |
| STORY-2114 - Poste recouvrement | P1 | 8 | 2.0j | STORY-2112 | **DONE** |
| STORY-2115 - Pilotage DAF | P1 | 5 | 1.0j | STORY-2112, OHADA | **READY** |
| STORY-2116 - E2E, RBAC, a11y | P0 | 5 | 0.5j | Stories précédentes | BACKLOG |

---

## Planification STORY-2113 — Poste Caissier (DONE)

### Objectif
En tant que caissier, je veux enregistrer les encaissements, gérer l'ouverture, les mouvements (entrées, dépenses, transferts) et la clôture de session de caisse avec gestion d'écart, afin d'assurer la traçabilité complète des flux d'espèces de l'établissement.

### Critères d'acceptation
- Une session de caisse ne peut être ouverte que si un fonds de caisse initial (espèces) est déclaré.
- Les encaissements en `CASH` augmentent le solde théorique de la caisse; les chèques et virements sont suivis séparément et n'influent pas sur les espèces théoriques.
- Les versements banque (`TRANSFER_TO_BANK`) exigent une référence de bordereau de dépôt.
- Les dépenses espèces supérieures à 100 000 FCFA nécessitent un double visa (autorisation DAF) et un justificatif.
- À la clôture, le caissier saisit le montant physique constaté. Tout écart doit être obligatoirement commenté et justifié.
- Export des reçus de paiement en PDF avec numérotation unique et mention légale.

### Découpage en sous-tâches (DONE)

| Tâche | Objectif | Profil | Estimation | Tests attendus | Statut |
|---|---|---|---:|---|---|
| **TASK-2113-B1** | Implémenter le calcul de solde espèces théorique dans `CashRegisterService` | Senior Backend | 0.5j | Tests unitaires du calcul de solde (IN, OUT, TRANSFER) | **DONE** |
| **TASK-2113-B2** | Restreindre et valider les dépenses > 100 000 FCFA (double visa de la DAF) | Senior Backend | 0.4j | Tests MockMvc sur les mouvements de dépenses | **DONE** |
| **TASK-2113-F1** | Refaire l'IHM du poste caissier Angular (ouverture, dépôt banque avec bordereau obligatoire, modal de clôture avec saisie constaté et justification) | Senior Frontend | 0.6j | Tests Vitest du formulaire de caisse et de clôture | **DONE** |
| **TASK-2113-F2** | Intégrer la gestion i18n FR/EN et les thèmes light/dark sur la console caissier | Frontend Intermédiaire | 0.3j | Validation visuelle responsive et contrastes | **DONE** |
| **TASK-2113-DOC**| Mettre à jour la documentation fonctionnelle et technique | Tech Lead | 0.2j | Fichiers de spec et d'API mis à jour | **DONE** |

**Reviewer recommandé** : Lead Developer + DAF.

---

## Planification STORY-2114 — Poste Recouvrement (DONE)

### Objectif
En tant que chargé de recouvrement, je veux disposer d'une balance âgée fiable des créances clients et tiers-payant, consigner des actions de relance et consulter leur historique chronologique afin d'accélérer l'encaissement des impayés.

### Découpage en sous-tâches (DONE)

| Tâche | Objectif | Profil | Estimation | Tests attendus | Statut |
|---|---|---|---:|---|---|
| **TASK-2114-B1** | Créer la table `receivable_reminders` par Flyway et entités JPA | Senior Backend | 0.4j | Migration schema V53, tests entités | **DONE** |
| **TASK-2114-B2** | Implémenter le service et contrôleur d'historique de relances (sécurité RBAC) | Senior Backend | 0.5j | Tests MockMvc sur les droits DAF vs Caissier | **DONE** |
| **TASK-2114-B3** | Calculer dynamiquement la tranche d'ancienneté (Aging slice) des créances | Senior Backend | 0.3j | Tests DTO `ReceivableResponse` | **DONE** |
| **TASK-2114-F1** | Refaire l'IHM de suivi de créances Angular (filtre par tranche d'ancienneté, badge, modale de consignation avec timeline d'historique) | Senior Frontend | 0.6j | Tests Vitest du composant `BillingReceivablesComponent` | **DONE** |
| **TASK-2114-DOC**| Mettre à jour la documentation fonctionnelle et technique | Tech Lead | 0.2j | Fichiers de spec et design technique mis à jour | **DONE** |

---

## Planification STORY-2115 — Pilotage DAF (READY)

### Objectif
En tant que DAF, je veux analyser l'ensemble des sessions de caisses de l'établissement, leurs écarts justifiés, gérer les remises/avoirs et exporter les écritures comptables selon le plan de compte OHADA cible pour intégration dans la comptabilité générale.

### Découpage en sous-tâches (READY)

| Tâche | Objectif | Profil | Estimation | Tests attendus | Statut |
|---|---|---|---:|---|---|
| **TASK-2115-B1** | Créer l'export d'écritures comptables OHADA en format CSV/JSON | Senior Backend | 0.4j | Tests unitaires de formattage et imputation plan de compte | **READY** |
| **TASK-2115-B2** | Implémenter les APIs de supervision globale des sessions et écarts caisses | Senior Backend | 0.3j | Tests MockMvc sur les endpoints DAF | **READY** |
| **TASK-2115-F1** | Créer l'IHM du tableau de bord DAF (liste globale des sessions, filtre sur écarts, bouton d'export comptable) | Senior Frontend | 0.3j | Tests Vitest des écrans DAF | **READY** |

