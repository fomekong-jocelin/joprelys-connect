# STORY-2203 — Poste caissier simplifié et file d’encaissement

## Mode

Product Design + Backend Engineering + Frontend Engineering + QA.

## Statut

IN_PROGRESS — cadrage terminé, implémentation en cours sur `feature/story-2203-cashier-collection-queue`.

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
- [ ] Ajouter le DTO et le service backend de file d’encaissement.
- [ ] Exposer l’endpoint sécurisé `GET /api/invoices/collection-queue`.
- [ ] Couvrir l’éligibilité, l’ordre, le RBAC et l’isolation tenant.
- [ ] Ajouter le contrat Angular et la méthode API.
- [ ] Créer le composant de file caissier responsive et accessible.
- [ ] Réutiliser la modale de paiement avec le reste patient réel.
- [ ] Afficher le reçu après paiement et rafraîchir la file.
- [ ] Autoriser explicitement le rôle `CAISSIER` à accéder à l’onglet caisse.
- [ ] Ajouter les traductions FR/EN et les tests Angular.
- [ ] Exécuter Maven strict, H2/PostgreSQL 16, tests Angular et build de production.
- [ ] Mettre à jour le suivi central et le changelog.

## Critères d’acceptation

- [ ] Un caissier voit uniquement les factures patient encaissables de son établissement.
- [ ] Les factures sont triées de la plus ancienne à la plus récente.
- [ ] La recherche filtre par nom, DPU ou numéro de facture.
- [ ] Le reste patient affiché provient de `InvoiceSettlementSummary` sans recalcul monétaire dans Angular.
- [ ] Aucun bouton d’encaissement n’est disponible sans session ouverte.
- [ ] Un paiement supérieur au reste patient est bloqué côté interface et côté backend existant.
- [ ] Les chèques et virements exigent une référence.
- [ ] Après succès, le reçu numéroté est visible et la file est actualisée.
- [ ] Les états vide, erreur et chargement sont distincts et accessibles.
- [ ] Le parcours est utilisable à 360 px, 768 px et 1440 px, en thèmes light/dark.
- [ ] Les tests backend, Angular et le build de production sont verts.

## Estimation

| Champ | Valeur |
|---|---|
| Priorité | P0 |
| Story points | 8 |
| Estimation senior | 2,5 j |
| Profil | Senior full-stack + Product Design |
| Reviewer | Lead Developer + DAF |
| Sprint | SPRINT-0014 |

## Risques

- N+1 lors du calcul des synthèses si la file grossit fortement ; une pagination/batch pourra devenir nécessaire après le pilote.
- Contrats historiques sans créance persistée ; le service financier central fournit déjà un fallback prudent.
- Confusion entre part patient et part assurance ; la file n’expose que le reste patient encaissable.
- Double soumission ; les boutons sont bloqués pendant l’enregistrement et le backend conserve ses contrôles transactionnels.

## Impact version

MINOR — ajout rétrocompatible d’un endpoint de lecture et d’un nouveau parcours caissier, sans modification de schéma.