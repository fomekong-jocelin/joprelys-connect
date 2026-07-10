# EPIC-0018 - Intégrité financière et poste de travail facturation/caisse

| Champ | Valeur |
|---|---|
| Type | Epic - Diagnostic, Engineering, UI/UX, Project Manager |
| Statut | IN_PROGRESS |
| Priorité | P0 |
| Estimation | 7.5 j senior |
| Profil recommandé | Senior full-stack, designer produit, DAF |
| Reviewer | Lead Developer + DAF + responsable recouvrement |
| SemVer cible | MINOR |

## Constat initial

Les écrans transmis montrent une contradiction critique: la facture `FAC-20260709-000002` est affichée comme réglée, mais ses créances patient et assurance sont affichées comme impayées. L'analyse confirme que `BillingService.addPayment` ne solde pas la créance patient alors qu'il modifie le statut de facture. Pour les factures assurées, ce statut représente actuellement le règlement de la seule part patient, ce qui est incompréhensible sans un état de règlement ventilé.

## Objectif

Fournir un parcours cohérent pour un caissier, une secrétaire comptable, le recouvrement et la DAF: une source de vérité financière, des états non ambigus, une caisse opérable, et des listes d'action adaptées à chaque rôle.

## Découpage

| Story | Objectif | SP | Est. senior | Statut |
|---|---|---:|---:|---|
| STORY-2111 | Synchroniser règlements facture, créances patient et états visibles | 3 | 0.8 j | DONE |
| STORY-2112 | Définir le modèle d'état financier ventilé patient/assurance | 5 | 1.2 j | DONE |
| STORY-2113 | Refaire le poste caissier: encaissement, reçu, ouverture/clôture et exceptions | 8 | 2.0 j | DONE |
| STORY-2114 | Refaire le poste recouvrement: listes actionnables, échéances, relances et aging | 8 | 2.0 j | DONE |
| STORY-2115 | Consolider le pilotage DAF: bordereaux, caisse, contrôle et export | 5 | 1.0 j | READY |
| STORY-2116 | Tests E2E, RBAC, accessibilité et régression financière | 5 | 0.5 j | BACKLOG |

## STORY-2111 - Definition of Ready

### User story

En tant que caissier, je veux qu'un règlement mette à jour immédiatement la créance de la part patient afin que l'historique facture et le suivi de créances ne se contredisent pas.

### Critères d'acceptation

- [ ] Un paiement partiel met la créance patient à `PARTIALLY_PAID` et conserve le solde exact.
- [ ] Un paiement complet met la créance patient à `PAID` et son solde à zéro.
- [ ] Une créance assurance reste ouverte tant que le bordereau n'est pas réglé.
- [ ] L'historique facture libelle explicitement le règlement de la part patient quand une part assurance reste à recouvrer.
- [ ] Les tests couvrent paiement partiel, paiement complet, facture assurée et facture non assurée.
- [ ] Aucun mouvement de caisse, reçu ou contrat existant ne régresse.

### Tâches

| Tâche | Estimation | Profil | Tests attendus |
|---|---:|---|---|
| Synchroniser les créances patient dans le cas d'usage de paiement | 0.35 j | Senior backend | Tests service/MockMvc paiement complet et partiel |
| Rendre l'état de règlement patient lisible dans l'historique | 0.2 j | Senior frontend | Test composant états assuré/non assuré |
| Corriger la liste de créances et les filtres trompeurs | 0.15 j | Frontend intermédiaire | Test chargement/filtre |
| Vérifier la non-régression financière | 0.1 j | Senior full-stack | Maven, Angular build/tests |

## Risques et dépendances

- La signification future des statuts de facture doit être validée avec la DAF: statut global de document ou statut de règlement patient.
- Les relances, échéances, autorisations de remise/avoir et écritures OHADA exigent une validation métier avant implémentation.
- Les changements de modèle ou de contrat nécessitent une migration compatible et une note de release.

## Plan d'action

- [x] Auditer les écrans transmis et reproduire la contradiction dans le code.
- [x] Identifier l'absence de synchronisation des créances lors du paiement.
- [x] Découper l'epic en stories, tâches et tests.
- [x] Implémenter STORY-2111.
- [x] Implémenter STORY-2112: read model de règlement et affichage de l'état ventilé.
- [x] Valider le modèle d'état avec DAF pour STORY-2112 à 2115 (voir [DAF-VALIDATION.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/features/financial-operations-integrity/DAF-VALIDATION.md)).
- [x] Exécuter le parcours E2E facture -> règlement patient -> assurance -> bordereau -> clôture (test E2E implémenté et validé).

## Vérifications STORY-2111


- [x] `npm run test -- --watch=false`: 101 tests Angular réussis.
- [x] `npm run build`: build Angular de production réussi.
- [x] `mvn -Dmaven.repo.local=<cache temporaire> -o test -Dtest=InvoiceControllerTest`: 2 tests réussis.

## Vérifications STORY-2112

- [x] `InvoiceControllerTest`: la synthèse expose `PATIENT_PARTIALLY_PAID`, puis `INSURANCE_DUE` après règlement complet de la part patient.
- [x] `npm run test -- --watch=false`: 101 tests Angular réussis.
- [x] `npm run build`: build Angular de production réussi.

## Vérifications STORY-2113

- [x] `CashRegisterControllerTest.shouldExecuteFullE2EWorkflow` : test d'intégration E2E complet réussi (facture -> paiement patient -> génération bordereau -> règlement assurance -> dépôt banque -> clôture caisse).
- [x] `./mvnw test -Dtest=CashRegisterControllerTest` : 4 tests réussis.
- [x] `npm run test -- --watch=false` : tests Angular au vert.

## Vérifications STORY-2114

- [x] `ReceivableReminderControllerTest` : tests d'intégration REST de création et listing d'actions de relance réussis (validation des droits DAF, caissier 403, etc.).
- [x] `npm run build` : compilation réussie du bundle Angular de production.
- [x] `npm run test -- --watch=false` : 101 tests Angular réussis (Vitest).

## Reste à faire

- Lancer le développement de la STORY-2115 (Bordereaux et exports comptables).
- Lancer le développement de la STORY-2116 (E2E complet).
- Livrer les stories 2111 à 2114 en production.

