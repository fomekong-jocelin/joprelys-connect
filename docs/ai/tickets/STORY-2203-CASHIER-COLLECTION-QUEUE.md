# STORY-2203 — Poste caissier simplifié et file d’encaissement

## Mode

Product Design + Backend Engineering + Frontend Engineering + QA.

## Statut

QA — implémentation et documentation terminées, pipeline permanent entièrement vert ; revue de la PR et QA visuelle navigateur restantes.

## Objectif

Permettre au caissier de traiter les règlements patient depuis une file de travail unique, sans rechercher manuellement un patient ni naviguer entre plusieurs espaces.

## Périmètre

- endpoint tenanté listant les factures réellement encaissables côté patient ;
- ordre de priorité du plus ancien au plus récent ;
- identité minimale du patient, numéro DPU, facture et reste patient ;
- recherche locale par patient, DPU ou numéro de facture ;
- encaissement total ou partiel depuis la file ;
- validation de la session de caisse active ;
- récupération et affichage immédiat du reçu après paiement ;
- actualisation de la file après encaissement ;
- états chargement, erreur, vide et session fermée ;
- responsive, light/dark, clavier et i18n FR/EN ;
- tests backend, Angular et non-régression globale.

## Hors périmètre

- paiement de la part assurance ;
- progression des bordereaux (`STORY-2204`) ;
- résolution DAF des écarts de caisse ;
- refonte du moteur comptable ;
- création d’une nouvelle table ou migration Flyway ;
- impression PDF du reçu, l’API actuelle exposant un reçu structuré.

## Règles métier

1. La file contient uniquement les factures dont `collectionStatus` vaut `PATIENT_DUE` ou `PATIENT_PARTIALLY_PAID`.
2. Les factures `PENDING`, `PROFORMA`, `PAID` avec assurance seule, `SETTLED` et `CANCELLED` sont exclues.
3. Le montant proposé par défaut est le reste patient fourni par le backend.
4. Le montant encaissé doit être strictement positif et ne doit pas dépasser le reste patient.
5. Une session de caisse active est obligatoire pour confirmer un paiement.
6. La référence est obligatoire pour les chèques et virements.
7. Après paiement, la file est rechargée depuis le backend et le reçu numéroté est affiché.
8. Toutes les données restent isolées par organisation via le tenant Hibernate actif.

## Plan d’action

- [x] Auditer le poste caisse, les contrats d’API et le modèle financier existants.
- [x] Définir le parcours, les états et les règles d’éligibilité.
- [x] Documenter la spécification, la conception et le plan de tests.
- [x] Ajouter le DTO et le service backend de file d’encaissement.
- [x] Exposer l’endpoint sécurisé `GET /api/invoices/collection-queue`.
- [x] Couvrir l’éligibilité, l’ordre, le RBAC et l’isolation tenant.
- [x] Ajouter le contrat Angular et la méthode API.
- [x] Créer le composant de file caissier responsive et accessible.
- [x] Réutiliser la modale de paiement avec le reste patient réel.
- [x] Afficher le reçu après paiement et rafraîchir la file.
- [x] Ajouter une route et une navigation dédiées au rôle `CAISSIER`.
- [x] Ajouter les traductions FR/EN et les tests Angular.
- [x] Exécuter la CI permanente : Maven strict, H2/PostgreSQL 16, tests Angular et build de production.
- [x] Mettre à jour le suivi central et le changelog.

## Critères d’acceptation

- [x] Un caissier voit uniquement les factures patient encaissables de son établissement.
- [x] Les factures sont triées de la plus ancienne à la plus récente.
- [x] La recherche filtre par nom, DPU, téléphone ou numéro de facture.
- [x] Le reste patient affiché provient de `InvoiceSettlementSummary` sans recalcul monétaire dans Angular.
- [x] Aucun bouton d’encaissement n’est disponible sans session ouverte.
- [x] Un paiement supérieur au reste patient est bloqué côté interface et côté backend existant.
- [x] Les chèques et virements exigent une référence.
- [x] Après succès, le reçu numéroté est visible et la file est actualisée.
- [x] Les états vide, erreur et chargement sont distincts et accessibles.
- [ ] Le parcours est validé manuellement à 360 px, 768 px et 1440 px, en thèmes light/dark.
- [x] La CI permanente backend, Angular et build de production est verte.

## Estimation et réalisation

| Champ | Valeur |
|---|---|
| Priorité | P0 |
| Story points | 8 |
| Estimation senior | 2,5 j |
| Temps passé cumulé | 2,3 j |
| Profil | Senior full-stack + Product Design |
| Reviewer | Lead Developer + DAF |
| Sprint | SPRINT-0014 |

## Résultats de validation

- pipeline permanent sur le commit propre : ✅ ;
- `npm test` : ✅ ;
- build Angular de production : ✅ ;
- Maven `clean verify` : ✅ ;
- migrations H2 et PostgreSQL 16 via Testcontainers : ✅ ;
- éligibilité `PATIENT_DUE` et `PATIENT_PARTIALLY_PAID` : ✅ ;
- exclusion des états assurance, soldé, annulé et non validé : ✅ ;
- RBAC `CAISSIER` et refus du rôle clinique : ✅ ;
- isolation entre deux organisations : ✅ ;
- recherche, filtres, surpaiement, référence obligatoire, reçu et rafraîchissement : ✅ ;
- suivi central et entrée `STORY-2203` du changelog : ✅ ;
- aucun fichier ou workflow de diagnostic dans le diff final : ✅ ;
- aucun test désactivé ou contourné.

## Sécurité et régression

- aucune organisation n’est fournie par le client ;
- l’isolation repose sur le tenant Hibernate installé depuis le JWT ;
- l’endpoint est limité aux rôles `CAISSIER`, `AGENT_ACCUEIL`, `ADMIN_CLINIQUE` et `DAF` ;
- la page dédiée `/clinic/cashier` est réservée au rôle `CAISSIER` ;
- les rôles cliniques sont refusés par test d’intégration ;
- le DTO ne contient aucune donnée médicale ni détail assurance inutile ;
- l’encaissement réutilise le service transactionnel existant ;
- les tests ne suppriment plus les données globales et restent indépendants de leur ordre d’exécution.

## Risques résiduels

- QA visuelle manuelle à effectuer en 360 px, 768 px et 1440 px, en thèmes light/dark ;
- validation Product/DAF de la densité des cartes, des indicateurs et des libellés ;
- endpoint non paginé pour le pilote : surveiller la volumétrie et les temps de réponse ;
- le calcul de synthèse par facture pourra nécessiter un traitement batch lorsque la file grandira ;
- `BillingCashRegisterComponent` reste un composant dense malgré l’extraction de la file.

## Impact version

MINOR — ajout rétrocompatible d’un endpoint de lecture et d’un nouveau parcours caissier, sans modification de schéma.

## Reste à faire

- revue de la PR #16 ;
- QA visuelle light/dark et responsive ;
- validation Product/DAF ;
- fusion après approbation.
