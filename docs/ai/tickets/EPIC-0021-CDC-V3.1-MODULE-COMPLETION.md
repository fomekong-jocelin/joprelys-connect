# EPIC-0021 — Complétion module par module du CDC Joprelys Connect V3.1

## Statut

- **Issue GitHub :** #25
- **Type :** Epic de conformité et de livraison
- **Priorité :** P0
- **État :** IN_PROGRESS
- **Source :** Cahier des charges V3.1 du 11/07/2026
- **Rapport :** `docs/audits/CDC-V3.1-MODULE-DELIVERY-GAP-ANALYSIS.md`
- **Version auditée :** `0.10.1`
- **Baseline Git :** `main@817d971c`

## Objectif

Traiter les 43 modules du cahier des charges comme des unités de livraison complètes, avec exigences traçables, code backend maître, migrations, UI, sécurité, tests, documentation, recette métier et preuves d'exploitation.

## Décision produit du 11/07/2026

L'intégration d'un fournisseur externe OTP par SMS/e-mail est reportée à la fin du projet pour raison budgétaire.

Cette décision ne bloque pas les travaux gratuits et structurants de sécurité :

- sessions persistantes ;
- refresh tokens rotatifs ;
- révocation distribuée ;
- détection du rejeu ;
- gestion Angular des sessions ;
- suppression de l'exposition OTP et des logs OTP en production.

La prochaine vague est donc constituée de :

1. **EPIC-0022 — Sessions persistantes et révocation distribuée** — issue #29 ;
2. **EPIC-0023 — Parcours URG-TEMP de bout en bout** — issue #36.

## Constat de l'audit

- 43 modules et 197 exigences testables ;
- identité provisoire et parcours urgence inconscient non pris en charge de bout en bout ;
- sessions/révocation actuellement en mémoire ;
- 10 modules non démontrés ;
- portes de sortie NFR, PRA, sécurité et UAT non prouvées globalement.

## Lots

| Lot | Périmètre | Statut |
|---|---|---|
| EPIC-0022 | Sessions et révocation | READY FOR PLANNING |
| EPIC-0023 | URG-TEMP | READY FOR PLANNING |
| Lot A restant | FND-01, FND-03, FND-04, CON-04 | BACKLOG |
| Lot C | PAT-02, CLN-03 à CLN-07 | BACKLOG |
| Lot D | HOS-01 à HOS-05 hors intégration URG-TEMP | BACKLOG |
| Lot E | CLN-08, HOS-06 à HOS-09 | BACKLOG |
| Lot F | CON-01 à CON-07 | BACKLOG |
| Lot G | FIN-01 à FIN-05 hors intégration URG-TEMP | BACKLOG |
| Lot H | OPS-01 à OPS-04, HR-01 | BACKLOG |
| Lot I | BI-01, NFR, PRA, observabilité, accessibilité et release | BACKLOG |
| Lot J | Fournisseur OTP, MFA de production et éventuel OIDC | DEFERRED — fin de projet |

## Actions de gouvernance

- [x] Lire le cahier des charges V3.1.
- [x] Extraire les 43 modules et 197 exigences.
- [x] Comparer la V3.1 avec la baseline `main`.
- [x] Produire la matrice module → preuves → écarts.
- [x] Créer EPIC-0022 et ses trois stories.
- [x] Créer EPIC-0023 et ses six stories.
- [x] Reporter le fournisseur OTP à la fin du projet.
- [ ] Faire valider les règles URG-TEMP par accueil, médecin urgentiste, DPO et DAF.
- [ ] Calculer la capacité et affecter les stories.
- [ ] Décider pour chaque module P2 : développement interne ou intégration.
- [ ] Créer la matrice automatisable `Requirement → Code → Test → UAT`.
- [ ] Préparer une release candidate après fermeture des P0.

## Definition of Ready des stories enfants

- exigence V3.1 identifiée ;
- règles métier et états clarifiés ;
- API/data/UI initialement documentées ;
- dépendances et données de test disponibles ;
- estimation ≤ 8 SP ;
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

## Risques

- l'OTP actuel ne doit pas être considéré comme MFA de production tant que le fournisseur n'est pas intégré ;
- le modèle patient actuel bloque le vrai URG-TEMP ;
- le rapprochement DPU touche de nombreuses relations et exige un ADR ;
- la finance et les documents ne doivent subir ni perte ni duplication ;
- le backlog doit rester synchronisé avec les issues et les PR.

## Prochain ordre d'exécution

```text
#31 STORY-2401 — Sessions persistantes et rotation
#40 STORY-2301 — Modèle patient provisoire
```

Ces deux stories peuvent être développées en parallèle par deux profils backend seniors distincts.
