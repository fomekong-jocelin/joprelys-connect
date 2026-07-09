# STORY-2107 — Bordereaux d'assurance et Tiers-payant avancé

## 1. Description du ticket
Implémenter la gestion complète des bordereaux d'assurance tiers-payant pour regrouper mensuellement les factures non réglées par convention d'assurance, suivre leur envoi et enregistrer le règlement global.

## 2. Critères d'acceptation
- [x] Une migration Flyway `V52__create_insurance_bordereaux_table.sql` crée la table `insurance_bordereaux`, la séquence SQL de numérotation, et ajoute `insurance_bordereau_id` sur la table `invoices`.
- [x] Le service backend `InsuranceBordereauService` permet de générer un bordereau pour une convention et une période en y associant automatiquement les factures validées correspondantes.
- [x] La numérotation du bordereau suit le format métier robuste `BORD-yyyyMMdd-XXXXXX` basé sur une séquence SQL.
- [x] Il est possible de marquer un bordereau comme envoyé (`SENT`) puis réglé (`PAID`).
- [x] Le règlement global d'un bordereau valide et met à jour le statut du bordereau à `PAID` et solde la part assurance des factures associées.
- [x] Les endpoints REST sont exposés dans `InsuranceBordereauController` et sécurisés pour les rôles `DAF`, `SECRETAIRE_COMPTABLE`, `ADMIN_CLINIQUE`.
- [x] Des tests d'intégration complets couvrent le cycle de vie du bordereau dans `InsuranceBordereauControllerTest.java`.
- [x] L'IHM Angular permet de lister, générer, consulter et solder les bordereaux via un onglet dédié à plat "Bordereaux Assurances" dans la page principale de facturation.
- [x] Toutes les vérifications de build Angular et tests backend passent avec succès.

## 3. Plan d'action et Suivi
- [x] **Tâche 1** : Mettre à jour la documentation technique et fonctionnelle (Documentation First).
- [x] **Tâche 2** : Écrire la migration SQL Flyway `V52`.
- [x] **Tâche 3** : Implémenter les entités JPA, enums et repositories.
- [x] **Tâche 4** : Implémenter le service métier `InsuranceBordereauService`.
- [x] **Tâche 5** : Implémenter le contrôleur REST `InsuranceBordereauController`.
- [x] **Tâche 6** : Écrire les tests d'intégration backend `InsuranceBordereauControllerTest.java`.
- [x] **Tâche 7** : Implémenter l'intégration frontend (modèles, API Service, composant UI bordereaux et onglet).
- [x] **Tâche 8** : Lancer la validation globale du projet (build Angular, mvn test).

## 4. Validation

- Test d'intégration backend `InsuranceBordereauControllerTest` : **OK** (cycle de vie complet génération → envoi → règlement).
- Build Angular (`npm run build`) : **OK** (pas d'erreur de compilation, chunk `billing-management-page-component` généré).
- Suite backend complète (`./mvnw test`) : **OK** (exit code 0, ~148k log lines).

## 6. Correctifs post-vérification

- La requête `findInvoicesForBordereau` prend maintenant les statuts `VALIDATED`, `PENDING`, `PARTIALLY_PAID` et utilise `COALESCE(validatedAt, createdAt)` pour la période, résolvant l'erreur 400 constatée en UI quand aucune facture `VALIDATED` n'existait.
- `getBordereauInvoices` et `listBordereaux` utilisent des requêtes filtrées par `organizationId` au lieu de `findAll()` en mémoire.
- `recordPayment` solde désormais les créances assurance via `ReceivableRepository.findByInvoiceIdAndDebtorTypeIgnoreCase` au lieu d'un `findAll()` filtré en mémoire.
- Le composant Angular `BillingInsuranceBordereauxComponent` utilise un helper partagé `extractApiErrorMessage` : plus aucun message brut technique du type `Http failure response for ...` n'est affiché à l'utilisateur final.
- Ajout d'un DTO `PageResponse<T>` stable et suppression de la sérialisation directe de `PageImpl` sur les endpoints paginés (`/api/pre-registrations`, `/api/notifications`) pour lever le warning Spring Data.

## 5. Impact version

- **SemVer** : `MINOR` — ajout fonctionnel de la gestion des bordereaux d'assurance tiers-payant.
