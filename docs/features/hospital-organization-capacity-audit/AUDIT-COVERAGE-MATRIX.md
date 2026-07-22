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

## Synthèse après les incréments techniques de phase 0

- Écarts audités : **40**.
- `COVERED` : **0**.
- `PARTIAL` : **8** — GAP-005, GAP-006, GAP-007, GAP-010, GAP-016, GAP-029, GAP-037 et GAP-039.
- `IN_PROGRESS` : **0**.
- `OPEN` : **32**.
- Score pondéré indicatif : **4 / 40 = 10 %**.

Les preuves techniques sont réparties sur une pile de PR encore en brouillon :

1. **#98** — HOS-BED-002-B, transitions manuelles sûres et validation PostgreSQL V76–V78 ;
2. **#99** — HOS-BED-001-D, exclusion des chevauchements historiques et quarantaine ;
3. **#100** — HOS-RBAC-001-A, permission dédiée au statut opérationnel du lit ;
4. **#101** — HOS-BED-002-C, capacité ouverte, préparation, usage dérivé et KPI associés ;
5. **#102** — HOS-RBAC-001-B, transfert, sortie, nettoyage et maintenance séparés, CI #932 verte.

Aucune de ces améliorations n’est comptée `COVERED` avant fusion, recette métier et validations externes. Le score reste inchangé avec #102 : l’incrément approfondit GAP-016 sans fermer le découpage des soins ni ajouter le contexte d’unité.

## Matrice détaillée

| Gap | Domaine | Statut | Preuve actuelle | Reste indispensable | Prochain incrément / story |
|---|---|---|---|---|---|
| GAP-001 | Organisation | OPEN | tenant `Organization` plat | groupe, établissement structuré, sites et bâtiments | HOS-ORG-001 + HOS-LOC-001 |
| GAP-002 | Organisation | OPEN | `Ward` mélange service et localisation | unités organisationnelles typées et migration legacy | HOS-ORG-001 |
| GAP-003 | Espaces | OPEN | `Room` limité aux wards hébergeants | espace générique indépendant du service | HOS-LOC-001 |
| GAP-004 | Données | OPEN | libellés service/chambre/lit recopiés dans le séjour | identifiants stables + snapshots explicites | HOS-LOC-001 + HOS-MOV-001 |
| GAP-005 | Intégrité temporelle | PARTIAL | V76–V78 validées PostgreSQL 16 ; V79/V80, préflight, quarantaine et exclusion GiST testés en CI #916 | préflight sur copie représentative, validation DBA/bed manager, idempotence métier | validation externe HOS-BED-001-D puis HOS-ADM-001 |
| GAP-006 | Cycle de vie lit | PARTIAL | transitions manuelles sûres ; capacité `OPEN/CLOSED`, préparation `READY/CLEANING/MAINTENANCE`, usage dérivé et refus des mutations sur lit affecté, CI #926 verte | motifs, acteurs spécialisés, historisation, turnover et validations métier | HOS-BED-002-D + HOS-DIS-001 |
| GAP-007 | Capacité affichée | PARTIAL | compteurs installés, ouverts, prêts, occupés et disponibles séparés ; usage dérivé des affectations actives | validation direction hospitalière, projections historisées et performance multi-services | HOS-KPI-001 |
| GAP-008 | Sortie | OPEN | sortie unique libérant l’affectation ; droit de décision médicale séparé dans #102 | clearance, départ physique, turnover et états indépendants | HOS-DIS-001 |
| GAP-009 | Admission | OPEN | admission directe `EN_COURS` ; claim limité aux lits ouverts, prêts et libres | demande, décision, planification, préadmission, réservation | HOS-ADM-001 |
| GAP-010 | Capacité | PARTIAL | V81/V82 séparent capacité ouverte, préparation et usage ; projection legacy conservée | existence/archivage, motifs et périodes de fermeture, preuves hygiène/maintenance | HOS-BED-002-D + HOS-LOC-001 |
| GAP-011 | Mouvement | OPEN | début/fin de `BedAssignment` ; permission de transfert dédiée dans #102 | motif, acteur, origine, destination, correction append-only | HOS-MOV-001 |
| GAP-012 | Archivage | OPEN | suppressions physiques spatiales conditionnelles | statuts, dates d’effet et archivage | HOS-ORG-001 + HOS-LOC-001 |
| GAP-013 | Chambre | OPEN | `comfortLevel` mélange confort, soins et isolement | dimensions et référentiels séparés | HOS-LOC-001 |
| GAP-014 | Compatibilité patient | OPEN | aucun contrôle sexe/âge/isolement/accompagnant | moteur de compatibilité backend | HOS-ADM-001 |
| GAP-015 | Personnel | OPEN | spécialité et département texte ; rôles hygiène/maintenance présents sans affectation d’unité | référentiels, emplois et affectations datées | HOS-STAFF-001 |
| GAP-016 | Séparation des tâches | PARTIAL | #100 sépare la supervision du lit ; #102 ajoute transfert, décision médicale de sortie, nettoyage et maintenance, rôles hygiène/maintenance et UI contextuelle ; CI #932 verte | admission, notes, consentements, soins, médicaments, consommables, clearance et départ physique restent regroupés ; contexte unité/soin absent | HOS-RBAC-001-C + HOS-DIS-001 + HOS-STAFF-001 |
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
| GAP-029 | Multi-tenant DB | PARTIAL | V78 protège affectation-séjour-lit ; index de capacité tenant-aware | FK composites sur toute la hiérarchie et les agrégats | transversal HOS-ORG/LOC/STAFF/RES |
| GAP-030 | Audit métier | OPEN | audit générique à raison texte ; capacité, nettoyage et maintenance ont des actions distinctes | événements structurés, before/after et corrélation | transversal, démarrage HOS-MOV-001 |
| GAP-031 | Imputabilité des soins | OPEN | acteurs parfois stockés en texte | identifiant utilisateur + snapshot signé | HOS-STAFF-001 + refonte clinique |
| GAP-032 | Responsable de séjour | OPEN | praticien non validé à l’admission | tenant, activité, habilitation, affectation/délégation | HOS-STAFF-001 + HOS-ADM-001 |
| GAP-033 | Interopérabilité | OPEN | API/FHIR partiels | mappings Organization, Location, Encounter, PractitionerRole | HOS-INT-001 |
| GAP-034 | Performance capacité | OPEN | lecture groupée des affectations actives au niveau d’un service | projections multi-services, snapshots et index temporels | HOS-KPI-001 |
| GAP-035 | Résilience | OPEN | web connecté uniquement | mode dégradé ciblé, reprise et idempotence | HOS-INT-001 |
| GAP-036 | Maintenabilité | OPEN | politiques de transition et circuits spécialisés isolés, mais grands services subsistent | use cases spécialisés et événements de domaine | refactoring continu par story |
| GAP-037 | Migration V74 | PARTIAL | préflights documentés et tests PostgreSQL 16 automatisés pour V76–V82 | exécution sur copie représentative et mapping V74 validé | chantier migration/DBA phase 0 |
| GAP-038 | Temps | OPEN | timestamps avec et sans fuseau ; V80 utilise `tsrange` conformément au schéma actuel | stratégie UTC/`Instant`/`timestamptz`, conversion et passage à `tstzrange` | tâche transversale data |
| GAP-039 | KPI lits ouverts | PARTIAL | taux d’occupation calculé sur `openBedsCount` et disponibilité sur ouvert + prêt + non affecté | validation métier des dénominateurs, tendances et agrégats temporels | HOS-KPI-001 |
| GAP-040 | Réglementaire | OPEN | règles pays non paramétrées | politiques validées par pays et gouvernance DPO/juridique | chantier conformité transverse |

## Ordre d’exécution phase 0 révisé

1. **Valider et intégrer #98** — garde-fous des transitions et socle PostgreSQL V76–V78.
2. **Valider et intégrer #99** — chevauchements historiques, extension et quarantaine.
3. **Valider et intégrer #100** — permission opérationnelle du lit.
4. **Valider et intégrer #101** — capacité ouverte, préparation, usage dérivé et KPI.
5. **Valider et intégrer #102** — transfert, décision de sortie, nettoyage et maintenance séparés.
6. **HOS-DIS-001-A** — empêcher toute libération avant confirmation du départ physique.
7. **HOS-BED-002-D** — motifs, acteurs et historisation des changements de capacité/préparation.
8. **HOS-RBAC-001-C** — séparer notes, consentements, soins, médicaments et consommables.

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
