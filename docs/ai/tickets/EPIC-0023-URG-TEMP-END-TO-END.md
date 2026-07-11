# EPIC-0023 — Parcours URG-TEMP de l'arrivée à la régularisation

## Statut

- **Issue GitHub :** #36
- **Priorité :** P0
- **État :** READY FOR PLANNING
- **Modules CDC :** PAT-01, CLN-01, CLN-02, CLN-09, HOS-01, DOC-01, FIN-01, FIN-02

## Objectif

Permettre la prise en charge immédiate d'un patient inconscient, non identifié ou dont l'identité n'est pas fiable, sans bloquer les soins sur l'accueil administratif, la pièce d'identité, le consentement ou le paiement.

## Parcours cible

```text
Arrivée urgente
→ création URG-TEMP minimale
→ triage et soins immédiats
→ déclarant/accompagnant/transporteur et effets personnels
→ incapacité et base d'urgence documentées
→ examens, réanimation, bloc ou hospitalisation
→ facturation différée et documents d'urgence
→ reprise de conscience / informations fiables
→ régularisation administrative
→ rapprochement ou création du DPU définitif
→ conservation intégrale de la timeline, des documents, soins, factures et audits
```

## Stories

| Story | Issue | SP | Dépendances | Résultat |
|---|---:|---:|---|---|
| STORY-2301 — Modèle patient provisoire et identifiant URG-TEMP | #40 | 8 | Aucune | Identité conditionnelle, provenance, confiance et identifiant provisoire |
| STORY-2302 — Admission d'urgence et triage sans identité définitive | #42 | 8 | #40 | Création atomique patient/urgence, triage et idempotence |
| STORY-2303 — Tiers, incapacité, urgence légale et effets personnels | #44 | 8 | #42 | Tiers qualifiés, base d'urgence et chaîne de possession |
| STORY-2304 — Régularisation et rapprochement DPU | #45 | 8 | #40, #44 | File de régularisation, candidats, décision auditée et alias permanent |
| STORY-2305 — Hospitalisation, documents et finance différée | #46 | 8 | #42, #44, #45 | Continuité hospitalière, documents vérifiables et créances conservées |
| STORY-2306 — Workspace Angular, E2E et UAT | #47 | 8 | #40 à #46 | Parcours mobile-first, régularisation et recette de bout en bout |

## Ordonnancement

```text
#40 Modèle URG-TEMP
→ #42 Admission/triage
→ #44 Tiers/incapacité/effets
→ #45 Régularisation/rapprochement
→ #46 Hospitalisation/documents/finance
→ #47 Workspace/E2E/UAT
```

#44 peut démarrer en parallèle de la fin de #42. L'ADR de #45 doit être accepté avant l'intégration de rapprochement dans #46.

## Règles métier non négociables

- Aucun soin urgent ne dépend d'une identité complète ou d'un paiement préalable.
- Aucun faux nom ou fausse date n'est requis pour satisfaire le modèle de données.
- Chaque information d'identité possède une source, un niveau de confiance et un horodatage.
- Un accompagnant n'est pas automatiquement un représentant légal.
- La base d'urgence est bornée, justifiée et réévaluée à la reprise de conscience.
- Une fusion n'écrase jamais les données source.
- Le code URG-TEMP reste un alias permanent.
- Les documents validés gardent leurs numéros, versions et hashes.
- Les soins, factures, paiements, hospitalisations et audits ne sont ni perdus ni dupliqués.

## Scénarios E2E obligatoires

1. patient inconscient amené seul ;
2. patient amené par un tiers avec identité déclarée non vérifiée ;
3. triage et réanimation avant toute action administrative ;
4. hospitalisation et facturation différée ;
5. reprise de conscience et création d'un nouveau DPU ;
6. rapprochement avec un DPU existant ;
7. plusieurs candidats et décision reportée ;
8. erreur réseau avec rejeu idempotent ;
9. tentative cross-tenant ;
10. correction contrôlée d'un mauvais rapprochement.

## Definition of Done

- création du dossier minimal en moins d'une minute lors de l'UAT ;
- migrations H2/PostgreSQL 16 ;
- backend maître des transitions ;
- tests tenant/RBAC/ABAC positifs et négatifs ;
- documents d'urgence vérifiables ;
- finance différée et rapprochement sans perte ;
- UI FR/EN, light/dark, mobile et clavier ;
- UAT signée par accueil, infirmier, médecin urgentiste, hospitalisation, caisse et DPO ;
- aucun risque critique ouvert.
