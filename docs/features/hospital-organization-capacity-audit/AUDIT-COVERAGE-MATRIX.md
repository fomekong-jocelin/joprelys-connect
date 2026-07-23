# Matrice de couverture — AUDIT-20260721

## Objet

Cette matrice est le registre de couverture exécutable du rapport `AUDIT-REPORT.md`. Elle relie chaque écart à une preuve dans le dépôt, à son prochain incrément et à ses validations.

Elle ne remplace ni la recette métier ni la validation réglementaire. Un écart n’est `COVERED` que lorsque le code, les migrations, les tests, la documentation, la sécurité et les validations requises sont terminés.

## Méthode de mesure

| Statut | Score de couverture | Définition |
|---|---:|---|
| `COVERED` | 1 | critères complets, QA cible verte et validations obtenues |
| `PARTIAL` | 0,5 | risque réduit, mais critères importants encore ouverts |
| `IN_PROGRESS` | 0,25 | implémentation en branche, non validée ou non testée |
| `OPEN` | 0 | aucune couverture suffisante |
| `BLOCKED` | 0 | décision, environnement ou validation externe manquante |

> Le score est un indicateur de couverture documentaire, pas un pourcentage d’effort ni une prévision de délai. Les 40 écarts n’ont pas la même complexité.

## Synthèse après intégration du socle de phase 0 et HOS-RBAC-001-C

- Écarts audités : **40**.
- `COVERED` : **0**.
- `PARTIAL` : **10** — GAP-005, GAP-006, GAP-007, GAP-008, GAP-010, GAP-016, GAP-029, GAP-030, GAP-037 et GAP-039.
- `IN_PROGRESS` : **0** au niveau du statut principal des gaps ; HOS-RBAC-001-D / #121 est un incrément de durcissement de GAP-016, déjà `PARTIAL`.
- `OPEN` : **30**.
- Score pondéré indicatif : **5 / 40 = 12,5 %**.

Les incréments #98 à #107 sont **fusionnés dans `main`** pour le périmètre de phase 0 traité ici :

1. **#98** — HOS-BED-002-B, transitions manuelles sûres et validation PostgreSQL V76–V78 ;
2. **#99** — HOS-BED-001-D, exclusion des chevauchements historiques et quarantaine ;
3. **#100** — HOS-RBAC-001-A, permission dédiée au statut opérationnel du lit ;
4. **#101** — HOS-BED-002-C, capacité ouverte, préparation, usage dérivé et KPI associés ;
5. **#102** — HOS-RBAC-001-B, transfert, décision médicale de sortie, nettoyage et maintenance séparés ;
6. **#103** — HOS-DIS-001-A, décision médicale sans libération, départ physique dédié et migration V83 ;
7. **#104** — HOS-BED-002-D, motifs codifiés, acteurs, sources et chronologie des états de lit, migrations V84/V85 ;
8. **#107** — HOS-RBAC-001-C, séparation de l’admission, des notes, consentements, soins, administrations médicamenteuses et consommables.

HOS-RBAC-001-D / **#121** est l'incrément courant : il supprime définitivement `HOSPITALIZATION_MANAGE` du catalogue et du référentiel persistant via V86, sans mapping automatique. GAP-016 reste `PARTIAL` car ABAC, habilitations professionnelles, clearance et validations externes restent ouverts.

## Matrice détaillée

| Gap | Domaine | Statut | Preuve actuelle | Reste indispensable | Prochain incrément / story |
|---|---|---|---|---|---|
| GAP-001 | Organisation | OPEN | tenant `Organization` plat | groupe, établissement structuré, sites et bâtiments | HOS-ORG-001 + HOS-LOC-001 |
| GAP-002 | Organisation | OPEN | `Ward` mélange service et localisation | unités organisationnelles typées et migration legacy | HOS-ORG-001 |
| GAP-003 | Espaces | OPEN | `Room` limité aux wards hébergeants | espace générique indépendant du service | HOS-LOC-001 |
| GAP-004 | Données | OPEN | libellés service/chambre/lit recopiés dans le séjour | identifiants stables + snapshots explicites | HOS-LOC-001 + HOS-MOV-001 |
| GAP-005 | Intégrité temporelle | PARTIAL | V76–V78 validées PostgreSQL 16 ; V79/V80, préflight, quarantaine et exclusion GiST intégrés via #99 | préflight sur copie représentative, validation DBA/bed manager, idempotence métier | validation externe HOS-BED-001-D puis HOS-ADM-001 |
| GAP-006 | Cycle de vie lit | PARTIAL | capacité, préparation et usage séparés ; libération uniquement au départ physique ; V84/V85 historisent valeur, motif, acteur et source, y compris transfert/départ | sélecteur UI de motif, tâche de turnover, preuves hygiène/maintenance et validations métier | HOS-BED-002-E + HOS-DIS-001-B |
| GAP-007 | Capacité affichée | PARTIAL | compteurs installés, ouverts, prêts, occupés et disponibles séparés ; usage dérivé des affectations actives | validation direction hospitalière, projections historisées et performance multi-services | HOS-KPI-001 |
| GAP-008 | Sortie | PARTIAL | V83 et #103 séparent décision médicale et départ physique ; seul le départ clôt le séjour, l'affectation et déclenche le nettoyage ; #104 historise ce nettoyage automatique | clearance administrative/financière, correction append-only, remise des documents/effets, tâche de turnover et validation métier | HOS-DIS-001-B/C + HOS-BED-002-E |
| GAP-009 | Admission | OPEN | admission directe `EN_COURS` ; claim limité aux lits ouverts, prêts et libres ; autorisation d’admission séparée et fusionnée via #107 | demande, décision, planification, préadmission, réservation | HOS-ADM-001 |
| GAP-010 | Capacité | PARTIAL | V81/V82 séparent capacité ouverte, préparation et usage ; V84/V85 ajoutent motifs et chronologie tenant-aware | existence/archivage, périodes d'effet, sélecteur UI, preuves hygiène/maintenance | HOS-BED-002-E + HOS-LOC-001 |
| GAP-011 | Mouvement | OPEN | début/fin de `BedAssignment` ; permission de transfert dédiée ; transfert interdit après décision médicale ; nettoyage source historisé | motif et ordre du mouvement, acteur/origine/destination structurés, correction append-only | HOS-MOV-001 |
| GAP-012 | Archivage | OPEN | suppressions physiques spatiales conditionnelles ; historique du lit encore supprimé par cascade | statuts, dates d’effet et archivage garantissant la conservation | HOS-ORG-001 + HOS-LOC-001 |
| GAP-013 | Chambre | OPEN | `comfortLevel` mélange confort, soins et isolement | dimensions et référentiels séparés | HOS-LOC-001 |
| GAP-014 | Compatibilité patient | OPEN | aucun contrôle sexe/âge/isolement/accompagnant | moteur de compatibilité backend | HOS-ADM-001 |
| GAP-015 | Personnel | OPEN | spécialité et département texte ; rôles hygiène/maintenance présents sans affectation d’unité | référentiels, emplois et affectations datées | HOS-STAFF-001 |
| GAP-016 | Séparation des tâches | PARTIAL | #100 sépare la supervision ; #102 sépare transfert, sortie, nettoyage et maintenance ; #103 sépare décision/départ ; #104 attribue les événements aux acteurs ; #107 sépare les six écritures cliniques ; #121/V86 retire la dernière permission générique `HOSPITALIZATION_MANAGE` en fail-closed | validation RSSI/médicale, clearance, contexte unité/relation de soin, habilitations et délégations datées | finaliser #121 puis HOS-DIS-001-B + HOS-STAFF-001 |
| GAP-017 | Confidentialité | OPEN | accès au séjour et à l'historique du lit tenant-wide ; granularité d’action renforcée par #107/#121 | ABAC unité, affectation et relation de soin | HOS-RBAC-001 + HOS-STAFF-001 |
| GAP-018 | Parcours patient | OPEN | modules et états juxtaposés ; jalons de sortie explicites et nettoyage historisé | épisode, présence, responsabilité, clearance et prochaine action | HOS-PATH-001 + HOS-DIS-001-B |
| GAP-019 | Handoff urgences | OPEN | navigation urgence vers hospitalisation non atomique ; #107 exige désormais l’autorité d’admission | demande d’aval et confirmation transactionnelle | HOS-ADM-001 + HOS-MOV-001 |
| GAP-020 | Localisation urgences | OPEN | urgence sans box/zone/position | présence et ressources d’urgence | HOS-LOC-001 + HOS-MOV-001 |
| GAP-021 | Bloc opératoire | OPEN | CRO et implants uniquement ; CRO reste volontairement sous `CLINICAL_WRITE` | salles, programme, équipe, ressources, habilitations et SSPI | epic bloc après HOS-RES-001 |
| GAP-022 | Réanimation | OPEN | `INTENSIVE_CARE` comme confort | unité, niveau de soins, équipements et capacité dédiée | HOS-ORG-001 + HOS-RES-001 |
| GAP-023 | Laboratoire physique | OPEN | ordres et résultats sans chaîne spatiale | facility, specimen, analyseur, emplacement et routage | epic laboratoire + HOS-LOC/RES |
| GAP-024 | États laboratoire | OPEN | statut directement remplaçable | machine à états et habilitation biologiste | epic laboratoire |
| GAP-025 | Imagerie | OPEN | aucun module | demandes, modalités, planning, résultats et PACS | epic imagerie |
| GAP-026 | Pharmacie | OPEN | dispensation et stock séparés ; #107 distingue explicitement administration hospitalière et prescription/pharmacie | transaction lot-stock-dispensation | epic pharmacie/stock |
| GAP-027 | Sécurité pharmacie | OPEN | verrouillage en mémoire | compteur persistant/distribué et rate limiting | epic sécurité pharmacie |
| GAP-028 | Ressources | OPEN | aucune entité équipement | ressource, localisation, réservation, maintenance | HOS-RES-001 |
| GAP-029 | Multi-tenant DB | PARTIAL | V78 protège affectation-séjour-lit ; V84 ajoute une FK composite événement-lit et V85 des domaines PostgreSQL | FK composites sur toute la hiérarchie et les agrégats | transversal HOS-ORG/LOC/STAFF/RES |
| GAP-030 | Audit métier | PARTIAL | `bed_state_changes` fournit before/after, motif, note, acteur, source et date pour capacité/préparation ; transfert et départ produisent des événements structurés | généralisation aux autres agrégats, correlationId, correction compensatoire, conservation indépendante du lit et validation RSSI | transversal HOS-MOV-001/HOS-DIS-001-B + plateforme audit |
| GAP-031 | Imputabilité des soins | OPEN | acteurs parfois stockés en texte ; sortie et événements de lit enregistrent UUID + snapshot ; #107 sépare les droits mais ne change pas encore la preuve d’auteur des soins | identifiant utilisateur + snapshot signé sur tous les actes | HOS-STAFF-001 + refonte clinique |
| GAP-032 | Responsable de séjour | OPEN | praticien non validé à l’admission | tenant, activité, habilitation, affectation/délégation | HOS-STAFF-001 + HOS-ADM-001 |
| GAP-033 | Interopérabilité | OPEN | API/FHIR partiels | mappings Organization, Location, Encounter, PractitionerRole | HOS-INT-001 |
| GAP-034 | Performance capacité | OPEN | lecture groupée des affectations actives au niveau d’un service ; historique indexé par lit/date | projections multi-services, snapshots, pagination et index temporels validés en charge | HOS-KPI-001 |
| GAP-035 | Résilience | OPEN | web connecté uniquement | mode dégradé ciblé, reprise et idempotence | HOS-INT-001 |
| GAP-036 | Maintenabilité | OPEN | politiques de transition, orchestration de sortie et service d'événements de lit isolés ; #107 remplace un droit générique par six intentions explicites et #121 retire le reliquat legacy | use cases spécialisés et événements de domaine généralisés | refactoring continu par story |
| GAP-037 | Migration V74 | PARTIAL | préflights documentés et tests PostgreSQL 16 automatisés pour V76–V85 ; V86 ajoute un nettoyage RBAC simple et forward-only | validation V86 en CI/PostgreSQL et exécution sur copie représentative selon la stratégie de livraison | chantier migration/DBA phase 0 |
| GAP-038 | Temps | OPEN | timestamps avec et sans fuseau ; V80 utilise `tsrange`, V83–V85 suivent le schéma `TIMESTAMP` actuel | stratégie UTC/`Instant`/`timestamptz`, conversion et passage à `tstzrange` | tâche transversale data |
| GAP-039 | KPI lits ouverts | PARTIAL | taux d’occupation calculé sur `openBedsCount` et disponibilité sur ouvert + prêt + non affecté | validation métier des dénominateurs, tendances et agrégats temporels | HOS-KPI-001 |
| GAP-040 | Réglementaire | OPEN | règles pays non paramétrées | politiques validées par pays et gouvernance DPO/juridique | chantier conformité transverse |

## Ordre d’exécution phase 0 révisé

1. **INTÉGRÉ — #98** : garde-fous des transitions et socle PostgreSQL V76–V78.
2. **INTÉGRÉ — #99** : chevauchements historiques, extension et quarantaine.
3. **INTÉGRÉ — #100** : permission opérationnelle du lit.
4. **INTÉGRÉ — #101** : capacité ouverte, préparation, usage dérivé et KPI.
5. **INTÉGRÉ — #102** : transfert, décision de sortie, nettoyage et maintenance séparés.
6. **INTÉGRÉ — #103** : décision médicale sans libération et départ physique dédié.
7. **INTÉGRÉ — #104** : motifs, acteurs et chronologie des changements de lit.
8. **INTÉGRÉ — #107 / HOS-RBAC-001-C** : admission, notes, consentements, soins, administration médicamenteuse et consommables séparés.
9. **EN COURS — #121 / HOS-RBAC-001-D** : supprimer `HOSPITALIZATION_MANAGE` du catalogue et de la base via V86, sans remapping automatique.
10. **VALIDATIONS HOS-RBAC-001** — revue RSSI/DPO, direction médicale, responsable hospitalisation et recette multi-profils sur les permissions explicites.
11. **HOS-DIS-001-B** — ajouter clearance administrative, prérequis et correction du processus de sortie.
12. **HOS-BED-002-E** — sélecteur UI, turnover et preuves opérationnelles.

## Conditions de changement de statut

### `IN_PROGRESS → PARTIAL`

- code et documentation présents sur une branche ;
- tests ciblés et suites complètes verts ;
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

Mettre à jour cette matrice après chaque PR fusionnée et après chaque validation externe. Ne jamais augmenter un statut uniquement parce qu’un ticket est marqué `DONE` : la preuve primaire reste le code testé et validé.
