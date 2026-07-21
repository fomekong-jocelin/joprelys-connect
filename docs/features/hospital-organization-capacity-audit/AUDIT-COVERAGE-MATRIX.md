# Matrice de couverture — AUDIT-20260721

## Objet

Cette matrice est le registre de couverture exécutable du rapport `AUDIT-REPORT.md`. Elle relie chaque écart à une preuve dans le dépôt, à son prochain incrément et à ses validations.

Elle ne remplace ni la recette métier ni la validation réglementaire. Un écart n’est `COVERED` que lorsque le code, les migrations, les tests, la documentation, la sécurité et les validations requises sont terminés.

## Méthode de mesure

| Statut | Score de couverture | Définition |
|---|---:|---|
| `COVERED` | 1 | critères complets, QA cible verte et validations obtenues |
| `PARTIAL` | 0,5 | risque réduit, mais critères importants encore ouverts |
| `IN_PROGRESS` | 0,25 | implémentation en branche, non validée ou non fusionnée |
| `OPEN` | 0 | aucune couverture suffisante |
| `BLOCKED` | 0 | décision, environnement ou validation externe manquante |

> Le score est un indicateur de couverture documentaire, pas un pourcentage d’effort ni une prévision de délai. Les 40 écarts n’ont pas la même complexité.

## Synthèse au démarrage de HOS-BED-002-B

- Écarts audités : **40**.
- `COVERED` : **0**.
- `PARTIAL` : **4** — GAP-005, GAP-007, GAP-029 et GAP-037.
- `IN_PROGRESS` : **1** — GAP-006 sur la branche `agent/hos-bed-002-b-safe-bed-status-transitions`.
- `OPEN` : **35**.
- Score pondéré indicatif : **2,25 / 40 = 5,63 %**.

## Matrice détaillée

| Gap | Domaine | Statut | Preuve actuelle | Reste indispensable | Prochain incrément / story |
|---|---|---|---|---|---|
| GAP-001 | Organisation | OPEN | tenant `Organization` plat | groupe, établissement structuré, sites et bâtiments | HOS-ORG-001 + HOS-LOC-001 |
| GAP-002 | Organisation | OPEN | `Ward` mélange service et localisation | unités organisationnelles typées et migration legacy | HOS-ORG-001 |
| GAP-003 | Espaces | OPEN | `Room` limité aux wards hébergeants | espace générique indépendant du service | HOS-LOC-001 |
| GAP-004 | Données | OPEN | libellés service/chambre/lit recopiés dans le séjour | identifiants stables + snapshots explicites | HOS-LOC-001 + HOS-MOV-001 |
| GAP-005 | Intégrité temporelle | PARTIAL | V76–V78, unicités actives, FK séjour et tenant | PostgreSQL 16, exclusion des périodes historiques, idempotence | HOS-BED-001-D |
| GAP-006 | Cycle de vie lit | IN_PROGRESS | politique de transition manuelle sur branche HOS-BED-002-B | QA, permissions spécialisées, axes de statut et turnover | HOS-BED-002-B puis HOS-BED-002-C/D |
| GAP-007 | Capacité affichée | PARTIAL | `availableBedsCount` compte uniquement `FREE` | lits installés, ouverts, prêts et usage dérivé | HOS-BED-002-C + HOS-KPI-001 |
| GAP-008 | Sortie | OPEN | sortie unique libérant l’affectation | décision médicale, clearance, départ physique, turnover | HOS-DIS-001 |
| GAP-009 | Admission | OPEN | admission directe `EN_COURS` | demande, décision, planification, préadmission, réservation | HOS-ADM-001 |
| GAP-010 | Capacité | OPEN | statut monolithique à quatre valeurs | axes existence, ouverture, hygiène et usage dérivé | HOS-BED-002 |
| GAP-011 | Mouvement | OPEN | début/fin de `BedAssignment` | motif, acteur, origine, destination, correction append-only | HOS-MOV-001 |
| GAP-012 | Archivage | OPEN | suppressions physiques spatiales conditionnelles | statuts, dates d’effet et archivage | HOS-ORG-001 + HOS-LOC-001 |
| GAP-013 | Chambre | OPEN | `comfortLevel` mélange confort, soins et isolement | dimensions et référentiels séparés | HOS-LOC-001 |
| GAP-014 | Compatibilité patient | OPEN | aucun contrôle sexe/âge/isolement/accompagnant | moteur de compatibilité backend | HOS-ADM-001 |
| GAP-015 | Personnel | OPEN | spécialité et département texte | référentiels, emplois et affectations datées | HOS-STAFF-001 |
| GAP-016 | Séparation des tâches | OPEN | `HOSPITALIZATION_MANAGE` tenant-wide | permissions décision, mouvement, hygiène et maintenance | HOS-RBAC-001 |
| GAP-017 | Confidentialité | OPEN | accès au séjour tenant-wide | ABAC unité, affectation et relation de soin | HOS-RBAC-001 + HOS-STAFF-001 |
| GAP-018 | Parcours patient | OPEN | modules et états juxtaposés | épisode, présence, responsabilité, prochaine action | HOS-PATH-001 |
| GAP-019 | Handoff urgences | OPEN | navigation urgence vers hospitalisation non atomique | demande d’aval et confirmation transactionnelle | HOS-ADM-001 + HOS-MOV-001 |
| GAP-020 | Localisation urgences | OPEN | urgence sans box/zone/position | présence et ressources d’urgence | HOS-LOC-001 + HOS-MOV-001 |
| GAP-021 | Bloc opératoire | OPEN | CRO et implants uniquement | salles, programme, équipe, ressources et SSPI | epic bloc après HOS-RES-001 |
| GAP-022 | Réanimation | OPEN | `INTENSIVE_CARE` comme confort | unité, niveau de soins, équipements et capacité dédiée | HOS-ORG-001 + HOS-RES-001 |
| GAP-023 | Laboratoire physique | OPEN | ordres et résultats sans chaîne spatiale | facility, specimen, analyseur, emplacement et routage | epic laboratoire + HOS-LOC/RES |
| GAP-024 | États laboratoire | OPEN | statut directement remplaçable | machine à états et habilitation biologiste | epic laboratoire |
| GAP-025 | Imagerie | OPEN | aucun module | demandes, modalités, planning, résultats et PACS | epic imagerie |
| GAP-026 | Pharmacie | OPEN | dispensation et stock séparés | transaction lot-stock-dispensation | epic pharmacie/stock |
| GAP-027 | Sécurité pharmacie | OPEN | verrouillage en mémoire | compteur persistant/distribué et rate limiting | epic sécurité pharmacie |
| GAP-028 | Ressources | OPEN | aucune entité équipement | ressource, localisation, réservation, maintenance | HOS-RES-001 |
| GAP-029 | Multi-tenant DB | PARTIAL | V78 protège affectation-séjour-lit | FK composites sur toute la hiérarchie et les agrégats | transversal HOS-ORG/LOC/STAFF/RES |
| GAP-030 | Audit métier | OPEN | audit générique à raison texte | événements structurés, before/after et corrélation | transversal, démarrage HOS-MOV-001 |
| GAP-031 | Imputabilité des soins | OPEN | acteurs parfois stockés en texte | identifiant utilisateur + snapshot signé | HOS-STAFF-001 + refonte clinique |
| GAP-032 | Responsable de séjour | OPEN | praticien non validé à l’admission | tenant, activité, habilitation, affectation/délégation | HOS-STAFF-001 + HOS-ADM-001 |
| GAP-033 | Interopérabilité | OPEN | API/FHIR partiels | mappings Organization, Location, Encounter, PractitionerRole | HOS-INT-001 |
| GAP-034 | Performance capacité | OPEN | chargement service par service | projections, snapshots et index temporels | HOS-KPI-001 |
| GAP-035 | Résilience | OPEN | web connecté uniquement | mode dégradé ciblé, reprise et idempotence | HOS-INT-001 |
| GAP-036 | Maintenabilité | OPEN | grands services applicatifs et responsabilités mêlées | use cases spécialisés et événements de domaine | refactoring continu par story |
| GAP-037 | Migration V74 | PARTIAL | préflights documentés pour les incréments lits | exécution sur copie représentative et mapping V74 validé | chantier migration/DBA phase 0 |
| GAP-038 | Temps | OPEN | timestamps avec et sans fuseau | stratégie UTC/`Instant`/`timestamptz` et migration | tâche transversale data |
| GAP-039 | KPI lits ouverts | OPEN | dénominateur basé sur lits configurés | capacité installée, ouverte et opérationnelle | HOS-BED-002 + HOS-KPI-001 |
| GAP-040 | Réglementaire | OPEN | règles pays non paramétrées | politiques validées par pays et gouvernance DPO/juridique | chantier conformité transverse |

## Ordre d’exécution phase 0

1. **HOS-BED-002-B** — bloquer les mutations manuelles incohérentes du lit.
2. **Validation PostgreSQL V76–V78** — Testcontainers/PostgreSQL 16 et préflights sur copie anonymisée.
3. **HOS-BED-001-D** — exclusion des chevauchements historiques et stratégie de quarantaine.
4. **HOS-BED-002-C** — introduire le minimum de séparation capacité ouverte / hygiène / usage sans casser l’API legacy.
5. **HOS-RBAC-001-A** — permissions minimales séparant mouvement, sortie, nettoyage et maintenance.
6. **HOS-DIS-001-A** — empêcher la libération du lit avant confirmation du départ physique.

## Conditions de changement de statut

### `IN_PROGRESS → PARTIAL`

- code et documentation présents sur une branche ;
- tests ciblés verts ;
- aucun contrat critique cassé ;
- risques résiduels explicités.

### `PARTIAL → COVERED`

- critères d’acceptation complets ;
- migrations validées sur PostgreSQL cible et données représentatives ;
- suites unitaires, intégration, concurrence et E2E vertes ;
- sécurité/RBAC négatif testé ;
- validation métier et technique obtenue ;
- audit, backlog, project tracking, changelog et guides mis à jour.

## Prochaine mise à jour

Mettre à jour cette matrice après chaque PR fusionnée. Ne jamais augmenter un statut uniquement parce qu’un ticket est marqué `DONE` : la preuve primaire reste le code testé et validé.
