# EPIC-0027 — Organisation hospitalière, capacité et parcours patient

**Origine** : AUDIT-20260721  
**Statut** : IN_PROGRESS — HOS-ORG et HOS-LOC fusionnés ; HOS-STAFF implémenté et validé techniquement dans PR #140  
**Priorité globale** : Critique  
**SemVer** : ajouts parallèles MINOR ; suppression des contrats legacy MAJOR  
**Source d’architecture** : `docs/architecture/adr/ADR-0002-flexible-hospital-organization-and-capacity-model.md`

## 1. Vision

Fournir à Joprelys un modèle flexible, historisé et sûr permettant à un cabinet, une clinique, un hôpital ou un réseau de gérer séparément :

1. l’organisation médicale ;
2. la géographie physique ;
3. les capacités et ressources ;
4. les affectations professionnelles ;
5. les admissions, mouvements et sorties ;
6. le parcours transverse du patient ;
7. les indicateurs et l’interopérabilité.

Le principe structurant est : **organisation ≠ géographie ≠ capacité ≠ personnel**.

Aucun nouveau développement ne doit renforcer le modèle plat historique `Organization → Ward → Room → Bed` ni les champs libres `users.department` / `users.specialty`.

## 2. Décisions d’architecture actives

### 2.1 Organisation

```text
Établissement
└── Pôle (optionnel)
    └── Département (optionnel)
        └── Service
            └── Unité de soins (optionnelle)
```

Une petite structure peut créer directement un `SERVICE` sous l’établissement. Aucun niveau intermédiaire factice n’est imposé.

### 2.2 Géographie

```text
Établissement
└── Site (optionnel)
    └── Bâtiment (optionnel)
        └── Étage (optionnel)
            └── Zone (optionnelle)
                └── Espace
```

Un `SPACE` est générique : consultation, salle d’attente, box d’urgence, chambre d’hospitalisation, bloc, SSPI, réanimation, laboratoire, imagerie, pharmacie, bureau, stockage, morgue, etc.

### 2.3 Relations

- unité organisationnelle ↔ espace : N:N daté ;
- personnel ↔ unité : N:N daté ;
- personnel ↔ spécialité : N:N ;
- aucune relation métier nouvelle par nom libre ;
- UUID stables et isolation tenant explicite ;
- désactivation/fermeture plutôt que suppression destructive d’un objet historiquement utilisé.

## 3. État consolidé des stories

| Story / lot | Objectif | Statut au 24/07/2026 | SP / effort | Prochaine action |
|---|---|---|---|---|
| HOS-BED-001 | invariants et concurrence lits | PARTIAL — incréments A/B/C livrés, D à arbitrer | 12–14 SP réestimés | validation métier/DBA et chevauchements historiques |
| HOS-BED-002 | capacité et cycles de remise en état | PARTIAL — compteur A livré | 8 SP | poursuivre après socle organisation/géographie |
| HOS-RBAC-001 | permissions hospitalières | PARTIAL — segmentation A/B/C/D livrée ; contexte ABAC restant | 8+ SP | HOS-STAFF / HOS-DIS pour le contexte |
| HOS-ORG-001-A / #130 | référentiels et unités organisationnelles | **DONE — PR #133 fusionnée, V87** | 9 SP | recette métier #127 |
| HOS-LOC-001-A / #131 | sites, bâtiments, étages, zones, espaces | **DONE — PR #137 fusionnée, V88–V91** | 13 SP réévalués | recette métier #127 |
| HOS-STAFF-001-A / #132 | affectations personnel et spécialités | **READY TECHNIQUE — PR #140, V92–V95, gate #1262 vert** | 9 SP | docs finales, gate post-doc, squash merge puis #127 |
| HOS-ADM-001 | demandes, préadmissions, réservations | PROPOSED | 13 SP | après org/loc/capacité |
| HOS-MOV-001 | présence et transferts | PROPOSED | 13 SP | après admission + staff/loc |
| HOS-DIS-001 | sortie médicale/admin/physique | PARTIAL conceptuellement via RBAC, workflow complet restant | 8 SP | après mouvements/capacité |
| HOS-RES-001 | ressources et équipements partagés | PROPOSED | 13 SP | après HOS-LOC + HOS-STAFF |
| HOS-PATH-001 | parcours transverse/work-items | PROPOSED | 13 SP | après org/loc/staff/mov |
| HOS-KPI-001 | capacité et saturation | PROPOSED | 8 SP | après modèles fiables |
| HOS-INT-001 | interopérabilité et mode dégradé | PROPOSED | 8 SP | après stabilisation des contrats |

## 4. Incréments phase 0 déjà livrés

| Task | Story | Objectif | Statut |
|---|---|---|---|
| HOS-BED-001-A | HOS-BED-001 | une seule affectation active par lit | intégré / PostgreSQL validé |
| HOS-BED-001-B | HOS-BED-001 | rattachement séjour + présence active + chronologie | intégré / PostgreSQL validé |
| HOS-BED-001-C | HOS-BED-001 | cohérence tenant affectation/séjour/lit | intégré / PostgreSQL validé |
| HOS-BED-001-D | HOS-BED-001 | chevauchements historiques | à arbitrer |
| HOS-BED-002-A | HOS-BED-002 | compteur fiable des lits disponibles | intégré / QA technique verte |
| HOS-RBAC-001-A | HOS-RBAC-001 | statut opérationnel des lits | DONE — PR #100 |
| HOS-RBAC-001-B | HOS-RBAC-001 | transfert/sortie/nettoyage/maintenance | DONE — PR #102 |
| HOS-RBAC-001-C | HOS-RBAC-001 | admission/notes/consentement/soins/médicaments/consommables | DONE — PR #107 |
| HOS-RBAC-001-D | HOS-RBAC-001 | suppression de `HOSPITALIZATION_MANAGE` | DONE — PR #122 / V86 / CI #1027 |
| HOS-ORG-001-A | HOS-ORG-001 | unités + catalogues structurés | DONE — PR #133 / V87 / CI backend #1066 + frontend verte |
| HOS-LOC-001-A | HOS-LOC-001 | géographie, espaces, lits sur `space_id`, hospitalisation structurée | DONE — PR #137 / V88–V91 / main `e495477e` |
| HOS-STAFF-001-A | HOS-STAFF-001 | spécialités + affectations datées staff↔unité | READY TECHNIQUE — PR #140 / V92–V95 / CI #1262 verte |

## 5. HOS-ORG-001 — Organisation hospitalière flexible

### État livré par HOS-ORG-001-A / #130

La première tranche est fusionnée via PR #133.

Le modèle livré distingue :

- `POLE` ;
- `DEPARTMENT` ;
- `SERVICE` ;
- `CARE_UNIT`.

Les niveaux sont facultatifs et les règles de parentage sont validées côté backend.

### Référentiels livrés

- `hospital_service_catalog` ;
- `medical_specialty_catalog` ;
- `organizational_unit_type_catalog` ;
- `organizational_units`.

Un `SERVICE` est identifié par un `serviceCatalogCode` stable. Son libellé n’est pas figé en français en base : l’UI résout FR/EN depuis le catalogue.

### Sécurité / tenant

- permission dédiée `ORGANIZATION_STRUCTURE_MANAGE` ;
- `ADMIN_CLINIQUE`, `ADMIN_JOPRELYS`, `SUPER_ADMIN` selon le scope autorisé ;
- métiers cliniques non autorisés par défaut ;
- `@TenantId` + filtres repository explicites + FK parent composite tenant ;
- aucun `organizationId` métier accepté dans le body.

### Règles

1. aucun niveau intermédiaire n’est obligatoire ;
2. aucun cycle n’est autorisé ;
3. un parent appartient au même tenant ;
4. un service vient du catalogue, pas d’un nom libre ;
5. une unité désactivée conserve son historique ;
6. aucune conversion automatique depuis `Ward.name`, `users.department` ou `users.specialty`.

### Critères validés techniquement

- petite clinique : SERVICE directement à la racine ;
- CHU : POLE → DEPARTMENT → SERVICE → CARE_UNIT ;
- service sans catalogue refusé ;
- service avec nom libre refusé ;
- code unique par tenant et réutilisable dans un autre tenant ;
- parent cross-tenant refusé ;
- désactivation avec enfant actif refusée ;
- utilisateur médecin sans permission : 403 ;
- UI mobile-first FR/EN light/dark ;
- Maven strict, PostgreSQL et Angular verts.

### Reste de HOS-ORG-001 hors incrément A

Les responsabilités organisationnelles datées, liens avancés et besoins multi-établissement éventuels ne doivent être ajoutés que lorsqu’un cas métier concret les exige. Ils ne doivent pas recréer un monolithe organisationnel avant HOS-STAFF/HOS-LOC.

## 6. HOS-LOC-001 — Géographie et espaces génériques

### État livré par HOS-LOC-001-A / #131

HOS-LOC-001-A est fusionné via PR #137 dans `main@e495477ea02beb05596b20b656bc092bd8fbbd83`.

Le lot remplace l’ambiguïté applicative `Ward/Room` par un référentiel géographique et spatial indépendant, tout en conservant les snapshots lisibles nécessaires aux documents historiques.

### Modèle livré

`SITE → BUILDING → FLOOR → ZONE → SPACE`, niveaux facultatifs.

### Types d’espace contrôlés minimaux

- consultation ;
- soins/examen ;
- box urgence ;
- attente ;
- chambre d’hospitalisation ;
- bloc opératoire ;
- SSPI ;
- réanimation ;
- laboratoire ;
- imagerie ;
- pharmacie ;
- stockage ;
- bureau ;
- morgue ;
- autre contrôlé.

### Règles livrées

1. un espace possède au plus une localisation géographique ; un espace peut être directement sous l’établissement ;
2. plusieurs unités organisationnelles peuvent utiliser le même espace via liens datés ;
3. un service peut utiliser des espaces dans plusieurs bâtiments/sites ;
4. un espace d’hébergement compatible porte le profil nécessaire aux lits ;
5. les lits référencent `space_id`, pas un service ou une `Room` par héritage ;
6. aucune migration automatique de `Room` par nom si la sémantique n’est pas prouvée ;
7. admissions et transferts utilisent `serviceUnitId / spaceId / bedId`.

### Critères validés techniquement #131

- cabinet : SPACE directement sans bâtiment obligatoire ;
- hôpital : hiérarchie complète possible ;
- plateau partagé : une salle, plusieurs liens organisationnels datés ;
- espaces non hospitaliers représentables ;
- tenant et hiérarchie protégés en DB et backend ;
- capacité, admission et transfert structurés ;
- mobile-first FR/EN light/dark ;
- migrations PostgreSQL + Maven + Angular verts ;
- PR #137 squash-mergée ; issue #131 fermée.

## 7. HOS-STAFF-001 — Affectations et habilitations datées

### État de HOS-STAFF-001-A / #132

HOS-STAFF-001-A est implémenté dans PR #140 et a franchi le gate combiné #1262 avant consolidation documentaire. Un dernier gate est requis sur le head exact contenant tracking/changelog/backlog avant squash merge.

### Modèle livré

- staff ↔ `organizational_units` N:N daté ;
- affectation principale/secondaire ;
- rôle d’affectation contextuel contrôlé et distinct du RBAC global ;
- date début/fin et statut actif dérivé de la période ;
- staff ↔ `medical_specialty_catalog` N:N daté ;
- spécialité principale facultative ;
- historique conservé ;
- refus des chevauchements et des affectations principales concurrentes ;
- isolation tenant application + DB.

### Migrations

- **V92** : preflight fail-fast si `users.department` ou `users.specialty` contient encore des données libres ;
- **V93** : catalogue de rôles contextuels et tables d’affectations structurées ;
- **V94** : contraintes PostgreSQL de non-chevauchement temporel ;
- **V95** : suppression physique de `users.department` et `users.specialty`.

### Contrats / UI

- `UpdateStaffRequest`, `StaffResponse`, `UserAccountEntity` et le profil n’exposent plus les deux champs libres ;
- l’éditeur staff sélectionne spécialités et unités depuis HOS-ORG ;
- l’historique, les périodes et les affectations principales sont visibles ;
- l’annuaire patient et le parcours rendez-vous utilisent `specialtyCode` et `organizationalUnitId` ;
- `StaffApiService` expose `activeOrganizationalUnits[]` avec UUID/code/libellés ;
- le module Visite conserve pour l’instant `visits.service_name` comme snapshot texte historique ; l’éventuel `StaffMember.department` de présentation est dérivé en mémoire de l’unité principale et n’est ni persisté ni accepté en écriture staff.

### Exemple supporté

```text
Dr X
├── spécialité principale : Médecine générale
├── unité principale : Médecine générale
└── Urgences : renfort du 01/08 au 31/08
```

### Validation technique

- cross-tenant staff/unité refusé ;
- unité ou spécialité inactive refusée ;
- chevauchement d’une même unité/spécialité refusé ;
- deux principales couvrant la même période refusées ;
- clôture conserve l’historique ;
- mutation réservée à `USER_MANAGE` ;
- tests PostgreSQL/Testcontainers dédiés présents ;
- gate #1262 : Maven strict SUCCESS, tests Angular SUCCESS, build Angular production SUCCESS ;
- aucune action PROD/RECETTE.

### Reste avant clôture

1. finaliser les sources centrales de documentation ;
2. relancer le gate combiné sur le head documentaire exact ;
3. vérifier `behind_by = 0` et l’absence de review threads ;
4. squash merge PR #140 / fermeture #132 ;
5. rejouer la recette humaine #127.
