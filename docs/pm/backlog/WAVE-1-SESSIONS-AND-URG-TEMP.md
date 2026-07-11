# WAVE 1 — Sessions, révocation et parcours URG-TEMP

## Décision

Le fournisseur OTP payant est reporté à la fin du projet. La vague 1 traite les fonctionnalités sans dépendance payante :

- sessions persistantes et révocation distribuée ;
- parcours URG-TEMP de l'arrivée à la régularisation.

## Capacité à planifier

- **Total : 66 SP**
- **Deux lanes parallèles recommandées** : sécurité/authentification et clinique/urgence.
- Ne pas engager les 66 SP dans un seul sprint sans capacité prouvée.

## Lane A — Sessions et révocation

| Ordre | Story | Issue | SP | Profil | Dépendance |
|---:|---|---:|---:|---|---|
| 1 | STORY-2401 — Sessions persistantes et rotation | #31 | 8 | Backend sécurité senior | Aucune |
| 2 | STORY-2402 — Révocation et rejeu | #33 | 5 | Backend sécurité senior/intermédiaire | #31 |
| 3 | STORY-2403 — Angular et gestion des sessions | #34 | 5 | Angular intermédiaire/senior | #31, #33 |

## Lane B — URG-TEMP

| Ordre | Story | Issue | SP | Profil | Dépendance |
|---:|---|---:|---:|---|---|
| 1 | STORY-2301 — Modèle patient provisoire | #40 | 8 | Backend/data senior | Aucune |
| 2 | STORY-2302 — Admission et triage | #42 | 8 | Backend clinique senior | #40 |
| 3 | STORY-2303 — Tiers, incapacité et effets | #44 | 8 | Backend clinique senior + fonctionnel | #42 |
| 4 | STORY-2304 — Régularisation et rapprochement | #45 | 8 | Backend/data senior | #40, #44, ADR |
| 5 | STORY-2305 — Hospitalisation, documents et finance | #46 | 8 | Full-stack senior | #42, #44, #45 |
| 6 | STORY-2306 — Workspace, E2E et UAT | #47 | 8 | Frontend senior + QA | #40 à #46 |

## Ordonnancement recommandé

### Vague 1A — Fondations

- #31 — sessions persistantes et rotation ;
- #40 — modèle patient provisoire.

Ces deux stories peuvent démarrer en parallèle.

### Vague 1B — Flux métier principaux

- #33 — révocation/rejeu ;
- #42 — création urgence/triage ;
- #44 — tiers/incapacité/effets, après stabilisation du contrat de #42.

### Vague 1C — Intégrations

- #34 — Angular sessions ;
- #45 — régularisation/rapprochement avec ADR ;
- #46 — hospitalisation/documents/finance.

### Vague 1D — Qualification

- #47 — workspace final, tests E2E, accessibilité et UAT.

## Portes de passage

| Gate | Condition |
|---|---|
| G1 | Migrations H2/PostgreSQL et contrats API de #31/#40 validés |
| G2 | Tests de concurrence refresh et création URG-TEMP idempotente verts |
| G3 | ADR de rapprochement patient accepté avant #46 |
| G4 | Aucun blocage de paiement/identité dans le parcours urgent |
| G5 | UAT complète signée avant déclaration de livraison |

## Risques et traitement

| Risque | Traitement |
|---|---|
| Contrat de login cassant | Compatibilité transitoire, documentation API et décision SemVer |
| Double refresh concurrent | Rotation atomique et verrouillage transactionnel |
| Faux doublon patient | Aucun rapprochement automatique ; décision humaine auditée |
| Perte de relations au rapprochement | ADR + inventaire complet des FK + tests d'intégrité |
| Facture/document dupliqué | Idempotence et invariants de numérotation/hash |
| Scope trop large de #46/#47 | Redécouper immédiatement si estimation dépasse 8 SP réels |

## Definition of Ready

- documents fonctionnels/techniques initiaux ;
- données de test ;
- reviewers désignés ;
- dépendances disponibles ;
- stratégie de migration et rollback ;
- critères d'acceptation testables.

## Definition of Done

- code, migration, UI et documentation ;
- tests unitaires, intégration, sécurité, Angular et E2E ;
- PostgreSQL 16 ;
- tenant/RBAC/ABAC ;
- FR/EN, light/dark, mobile, clavier ;
- changelog, tracking et SemVer ;
- UAT métier signée.
