# Backlog de complétion — CDC Joprelys Connect V3.1

> Source de détail : `docs/audits/CDC-V3.1-MODULE-DELIVERY-GAP-ANALYSIS.md`  
> Epic de gouvernance : `docs/ai/tickets/EPIC-0021-CDC-V3.1-MODULE-COMPLETION.md`

## Règles de planification

- Une ligne ci-dessous représente un **lot module**, pas une story directement développable.
- Chaque lot doit être découpé en stories de **1 à 8 SP**.
- L'ordre respecte les dépendances métier et techniques, pas uniquement la priorité nominale du CDC.
- Les modules avancés doivent être complétés par exigence manquante et preuve; ils ne doivent pas être réécrits.
- Les modules non démontrés nécessitent d'abord un ADR build vs intégration.

## Backlog consolidé

| Epic | Module | Priorité | Baseline | Action de préparation |
|---|---|---:|---|---|
| EPIC-0021-A | **FND-01 — Établissements, spécialités et configuration multi-tenant** | P0 | PARTIEL AVANCÉ | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-A | **FND-02 — Identité, authentification et sessions** | P0 | PARTIEL — BLOQUANT LIVRAISON | Créer immédiatement stories secrets/OTP, sessions persistantes, refresh tokens, OIDC et révocation distribuée. |
| EPIC-0021-A | **FND-03 — Rôles, permissions et délégations** | P0 | AVANCÉ — INCOMPLET | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-A | **FND-04 — Nomenclatures, tarifs et référentiels** | P0 | PARTIEL | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-B | **PAT-01 — Identité patient unique et dédoublonnage** | P0 | PARTIEL — BLOQUANT URGENCES | Créer le modèle URG-TEMP, déclarations d'identité, niveaux de confiance, alias et rapprochement. |
| EPIC-0021-C | **PAT-02 — Portail patient, profil et représentants** | P1 | PARTIEL AVANCÉ | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-B | **CLN-01 — Accueil, rendez-vous, registre et visite** | P0 | PARTIEL | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-B | **CLN-02 — Triage, constantes et alertes cliniques** | P0 | AVANCÉ — INCOMPLET | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-C | **CLN-03 — Consultation, diagnostics et décisions** | P0 | AVANCÉ — INCOMPLET | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-C | **CLN-04 — Allergies, antécédents et traitements chroniques** | P0 | AVANCÉ — INCOMPLET | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-C | **CLN-05 — Prescriptions et ordonnances** | P0 | AVANCÉ — INCOMPLET | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-C | **CLN-06 — Demandes d'examens et actes médico-techniques** | P0 | PARTIEL | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-C | **CLN-07 — Laboratoire, résultats et valeurs critiques** | P1 | PARTIEL AVANCÉ | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-E | **CLN-08 — Imagerie et DICOMweb** | P2 | NON DÉMONTRÉ | ADR PACS/DICOMweb puis contrats d'intégration. |
| EPIC-0021-B | **CLN-09 — Urgences, patient inconscient et réanimation** | P0 | PARTIEL — BLOQUANT URGENCES | Créer une story verticale urgence inconscient avec incapacité, tiers, effets et régularisation. |
| EPIC-0021-D | **HOS-01 — Admission, hospitalisation et sortie** | P1 | PARTIEL AVANCÉ | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-D | **HOS-02 — Services, chambres et lits** | P1 | AVANCÉ — INCOMPLET | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-D | **HOS-03 — Bloc opératoire et compte rendu opératoire** | P1 | PARTIEL AVANCÉ | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-D | **HOS-04 — Anesthésie et consentements opératoires** | P1 | PARTIEL | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-D | **HOS-05 — Soins journaliers, administrations et suivi post-opératoire** | P1 | PARTIEL AVANCÉ | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-E | **HOS-06 — Kinésithérapie et rééducation** | P2 | NON DÉMONTRÉ | Atelier kiné puis modèle prescription/séances/bilan. |
| EPIC-0021-E | **HOS-07 — Maternité et obstétrique** | P2 | NON DÉMONTRÉ | Atelier obstétrique puis partogramme et dossier mère-enfant. |
| EPIC-0021-E | **HOS-08 — Décès, morgue et remise du corps** | P1 | NON DÉMONTRÉ | Atelier médico-légal puis circuit décès/morgue/remise. |
| EPIC-0021-E | **HOS-09 — Garde, passation et staff** | P1 | NON DÉMONTRÉ | Atelier soins/garde puis registre/passation signée. |
| EPIC-0021-B | **DOC-01 — Documents, GED, signatures et vérification** | P0 | AVANCÉ — INCOMPLET | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-F | **CON-01 — DPU partagé et synthèse longitudinale** | P1 | PARTIEL | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-F | **CON-02 — Consentements et préférences de partage** | P1 | PARTIEL AVANCÉ | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-F | **CON-03 — Accès externe, temporaire et urgence** | P1 | PARTIEL AVANCÉ | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-A | **CON-04 — Audit et investigation** | P0 | PARTIEL AVANCÉ | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-F | **CON-05 — Notifications et communications** | P1 | PARTIEL FAIBLE | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-F | **CON-06 — API, webhooks et partenaires** | P1 | PARTIEL | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-F | **CON-07 — Interopérabilité FHIR** | P2 | PARTIEL FAIBLE | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-B | **FIN-01 — Catalogue, devis et facturation** | P0 | AVANCÉ — INCOMPLET | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-B | **FIN-02 — Caisse, paiements, clôture et remboursement** | P0 | AVANCÉ — INCOMPLET | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-G | **FIN-03 — Assurances, conventions et tiers payant** | P1 | AVANCÉ — INCOMPLET | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-H | **OPS-01 — Achats et fournisseurs** | P2 | NON DÉMONTRÉ | ADR build vs ERP achats puis workflow de validation. |
| EPIC-0021-H | **OPS-02 — Stocks, pharmacie et traçabilité des lots** | P1 | PARTIEL | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-H | **OPS-03 — Immobilisations et maintenance** | P2 | NON DÉMONTRÉ | ADR build vs ERP immobilisations/maintenance. |
| EPIC-0021-G | **FIN-04 — Comptabilité générale OHADA** | P2 | PARTIEL FAIBLE | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |
| EPIC-0021-G | **FIN-05 — Budget et comptabilité analytique** | P2 | NON DÉMONTRÉ | ADR build vs ERP budget/analytique. |
| EPIC-0021-H | **HR-01 — Ressources humaines, planning et paie** | P2 | NON DÉMONTRÉ | Décider intégration SIRH/paie ou développement. |
| EPIC-0021-H | **OPS-04 — Restauration et régimes** | P2 | NON DÉMONTRÉ | Atelier restauration et nutrition clinique. |
| EPIC-0021-I | **BI-01 — Statistiques et tableaux de bord médico-économiques** | P1 | PARTIEL FAIBLE | Découper les exigences non couvertes en stories ≤ 8 SP avec tests et UAT. |

## Dépendances critiques

```text
FND-02 ─┬─> FND-03/CON-04
        └─> tous les parcours sensibles

PAT-01 ─> CLN-01 ─> CLN-02 ─> CLN-09 ─┬─> HOS-01
                                          ├─> DOC-01
                                          └─> FIN-01/FIN-02

FND-04 ─> CLN-05/06/07 ─> FIN-01/03 ─> FIN-04/05

DOC-01 + CON-02/03/04 ─> CON-01/06/07
```

## Vagues proposées

| Vague | Lots | Condition de sortie |
|---|---|---|
| 0 — Baseline | Audit, tracking, matrice de traçabilité, décisions build/intégration | Backlog validé et reviewers nommés |
| 1 — P0 sécurité/urgence | EPIC-0021-A et B | Parcours URG-TEMP E2E, sécurité de session industrialisée, zéro fuite tenant |
| 2 — P0 ambulatoire | EPIC-0021-C + éléments P0 finance/doc | UAT ambulatoire complète et documents vérifiables |
| 3 — P1 hospitalier/Connect | EPIC-0021-D et F | UAT métier hospitalisation, accès externe et KPI opérationnels |
| 4 — Extensions | EPIC-0021-E, G, H | Modules spécialisés ou intégrations certifiées |
| 5 — Release | EPIC-0021-I | NFR, PRA, sécurité, accessibilité, UAT et rollback validés |

## Modèle obligatoire d'une story module

Chaque story issue de ce backlog doit contenir :

- exigence(s) V3.1 et valeur métier ;
- état actuel prouvé par chemins de fichiers/tests ;
- workflow, statuts et règles ;
- modèle de données et migration additive ;
- contrat API et erreurs ;
- permissions et contraintes tenant ;
- UI, i18n, light/dark, mobile et accessibilité ;
- tests unitaires, intégration, PostgreSQL, Angular et E2E ;
- données de recette et reviewer métier ;
- documentation, changelog, SemVer et rollback.
