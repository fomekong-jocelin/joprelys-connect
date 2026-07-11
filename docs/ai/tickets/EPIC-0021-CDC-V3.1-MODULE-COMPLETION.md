# EPIC-0021 — Complétion module par module du CDC Joprelys Connect V3.1

## Statut

- **Type :** Epic de conformité et de livraison
- **Priorité :** P0
- **État :** IN_PROGRESS — audit initial produit, validation produit/métier attendue
- **Source :** Cahier des charges V3.1 du 11/07/2026
- **Rapport :** `docs/audits/CDC-V3.1-MODULE-DELIVERY-GAP-ANALYSIS.md`
- **Version auditée :** `0.10.1`
- **Baseline Git :** `main@817d971c`

## Objectif

Traiter les 43 modules du cahier des charges comme des unités de livraison complètes, avec exigences traçables, code backend maître, migrations, UI, sécurité, tests, documentation, recette métier et preuves d'exploitation.

## Constat

Le dépôt possède un socle riche mais la présence d'un écran, d'une table ou d'un endpoint ne suffit pas à déclarer un module livré. L'audit recense :

- 197 exigences testables ;
- 3 écarts P0 bloquants : authentification/sessions, identité provisoire et urgence inconscient ;
- 10 modules non démontrés ;
- des portes de sortie NFR, PRA, sécurité et UAT non prouvées globalement.

## Périmètre

### Lot A — Socle sécurisé

- FND-01 à FND-04
- CON-04

### Lot B — Patient inconscient et identité provisoire

- PAT-01
- CLN-01, CLN-02, CLN-09
- DOC-01
- FIN-01, FIN-02
- dépendance HOS-01

### Lot C — Parcours ambulatoire

- PAT-02
- CLN-03 à CLN-07

### Lot D — Hospitalisation

- HOS-01 à HOS-05

### Lot E — Extensions hospitalières

- CLN-08
- HOS-06 à HOS-09

### Lot F — Connect

- CON-01 à CON-07

### Lot G — Finance

- FIN-01 à FIN-05

### Lot H — Back Office

- OPS-01 à OPS-04
- HR-01

### Lot I — BI et industrialisation

- BI-01
- NFR, sécurité, PRA/PCA, observabilité, accessibilité, migration et release

## Actions

### Audit et gouvernance

- [x] Lire le cahier des charges V3.1.
- [x] Extraire les 43 modules et 197 exigences.
- [x] Comparer la V3.1 avec la baseline `main`.
- [x] Produire la matrice module → preuves → écarts.
- [x] Identifier les risques P0 et les modules non démontrés.
- [ ] Faire valider les statuts par Product Owner, Tech Lead, Médecin chef, DAF, DPO et QA.
- [ ] Décider pour chaque module P2 : développement interne ou intégration ERP/service spécialisé.
- [ ] Créer les epics enfants et les stories ≤ 8 SP.
- [ ] Affecter un reviewer métier et un reviewer technique à chaque lot.
- [ ] Planifier les vagues selon la capacité réelle.

### P0 sécurité

- [ ] Désactiver toute exposition OTP au frontend hors profil de test.
- [ ] Supprimer l'écriture des OTP en console.
- [ ] Persister sessions, facteurs MFA, refresh tokens et révocations.
- [ ] Implémenter OIDC/OAuth2 ou documenter formellement la stratégie locale de secours.
- [ ] Étendre l'autorisation au contexte service/relation de soins/consentement.
- [ ] Implémenter délégations temporaires et revue périodique des droits.

### P0 urgence inconscient

- [ ] Créer le profil patient `PROVISOIRE_URGENCE`.
- [ ] Générer un identifiant `URG-TEMP` en moins d'une minute sans identité fiable.
- [ ] Permettre l'ouverture du triage avant l'accueil administratif.
- [ ] Qualifier accompagnant, témoin, transporteur, représentant et garant.
- [ ] Enregistrer incapacité, base d'urgence, finalité, durée et actes couverts.
- [ ] Gérer effets personnels et chaîne médico-légale.
- [ ] Produire les documents d'urgence versionnés.
- [ ] Permettre admission, examens, bloc, soins et facturation différée.
- [ ] Régulariser l'identité et les finances après stabilisation.
- [ ] Rapprocher avec un DPU existant sans perdre timeline, factures, documents ni audits.
- [ ] Couvrir le parcours complet par tests E2E et UAT.

### Delivery par module

- [ ] Créer une matrice de traçabilité automatisable `Requirement → Code → Test → UAT`.
- [ ] Ajouter une checklist de sortie commune à chaque module.
- [ ] Interdire le statut DONE sans preuve de tests et validation métier.
- [ ] Mettre à jour `PROJECT-TRACKING`, changelog, version matrix et release notes à chaque fusion.
- [ ] Préparer une release candidate après fermeture des P0.

## Critères d'acceptation de l'epic

- Les 43 modules possèdent un propriétaire, une priorité, un statut réel et un backlog.
- Chaque exigence de la vague cible est reliée à au moins une preuve de code et de test.
- Aucun module n'est marqué livré avec un écart critique ouvert.
- Les parcours P0 passent en E2E sur PostgreSQL 16 et Angular production.
- Les tests cross-tenant retournent un refus sans fuite d'existence ou de contenu.
- Le parcours patient inconscient fonctionne sans identité complète et sans paiement préalable.
- Les documents validés sont versionnés et non écrasés.
- Les règles médicales, financières et réglementaires sont signées par les responsables habilités.
- Une restauration de sauvegarde prouve les objectifs RPO/RTO.
- La release dispose d'une note, d'un rollback et d'une UAT signée.

## Definition of Ready des stories enfants

- exigence V3.1 identifiée ;
- règles métier et états clarifiés ;
- API/data/UI initialement documentées ;
- dépendances et données de test disponibles ;
- estimation ≤ 8 SP ou story redécoupée ;
- reviewers métier et technique nommés.

## Definition of Done des stories enfants

- backend, migration et UI terminés selon le périmètre ;
- tests unitaires, intégration, sécurité, Angular et E2E verts ;
- H2 et PostgreSQL 16 validés ;
- tenant, RBAC/ABAC, audit et idempotence vérifiés ;
- FR/EN, light/dark, mobile, clavier et accessibilité vérifiés ;
- documentation et suivi mis à jour ;
- recette métier signée ;
- aucun risque critique ouvert.

## Estimation de cadrage

L'ensemble de la V3.1 représente plusieurs vagues de produit et non un sprint unique. Les neuf lots sont estimés initialement entre **764 et 1 165 SP**, avant ateliers de découpage. Toute story supérieure à 8 SP devra être redécoupée.

## Risques

- urgence bloquée par le modèle d'identité actuel ;
- exposition OTP et sécurité en mémoire ;
- règles métier médicales/comptables non validées ;
- stockage local des documents ;
- modules P2 non démontrés ;
- confusion entre fonctionnalité présente et module livré ;
- backlog et tracking désynchronisés.

## Prochaine story à préparer

`STORY-2301 — Créer et prendre en charge un patient URG-TEMP de l'arrivée à la régularisation`.
