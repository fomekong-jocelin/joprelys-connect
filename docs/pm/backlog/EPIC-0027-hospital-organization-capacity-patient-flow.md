# EPIC-0027 — Organisation hospitalière, capacité et parcours patient

**Origine** : AUDIT-20260721  
**Statut** : IN_PROGRESS — architecture cible acceptée, socle phase 0 et HOS-ORG-001-A engagés/livrés  
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

| Story / lot | Objectif | Statut au 23/07/2026 | SP / effort | Prochaine action |
|---|---|---|---|---|
| HOS-BED-001 | invariants et concurrence lits | PARTIAL — incréments A/B/C livrés, D à arbitrer | 12–14 SP réestimés | validation métier/DBA et chevauchements historiques |
| HOS-BED-002 | capacité et cycles de remise en état | PARTIAL — compteur A livré | 8 SP | poursuivre après socle organisation/géographie |
| HOS-RBAC-001 | permissions hospitalières | PARTIAL — segmentation A/B/C/D livrée ; contexte ABAC restant | 8+ SP | HOS-STAFF / HOS-DIS pour le contexte |
| HOS-ORG-001-A / #130 | référentiels et unités organisationnelles | **DONE CODE — PR #133 fusionnée, V87** | 9 SP | consolidation docs puis validation métier |
| HOS-LOC-001-A / #131 | sites, bâtiments, étages, zones, espaces | READY | 9 SP | prochain incrément, depuis `main` post-#133 |
| HOS-STAFF-001-A / #132 | affectations personnel et spécialités | BLOCKED par #131 | 9 SP | après HOS-LOC |
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
| HOS-ORG-001-A | HOS-ORG-001 | unités + catalogues structurés | DONE CODE — PR #133 / V87 / CI backend #1066 + frontend verte |

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

### Incrément suivant : HOS-LOC-001-A / #131

Objectif : remplacer progressivement l’ambiguïté `Ward/Room` par un référentiel géographique indépendant.

### Modèle cible

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

### Règles

1. un espace possède une localisation géographique unique ;
2. plusieurs unités organisationnelles peuvent utiliser le même espace via liens datés ;
3. un service peut utiliser des espaces dans plusieurs bâtiments/sites ;
4. une chambre d’hospitalisation est un espace compatible avec des lits ;
5. les lits ne sont pas rattachés à un service administratif par simple héritage ;
6. aucune migration automatique de `Room` par nom si la sémantique n’est pas prouvée.

### Critères d’acceptation #131

- cabinet : SPACE directement sans bâtiment obligatoire ;
- hôpital : hiérarchie complète possible ;
- plateau partagé : une salle, plusieurs liens organisationnels datés ;
- espaces non hospitaliers représentables ;
- tenant et hiérarchie protégés en DB et backend ;
- mobile-first FR/EN light/dark ;
- migrations PostgreSQL + Maven + Angular verts.

## 7. HOS-STAFF-001 — Affectations et habilitations datées

### Incrément HOS-STAFF-001-A / #132

Objectif : supprimer le besoin fonctionnel de `users.department` et `users.specialty` libres.

### Modèle attendu

- staff ↔ organizational_unit N:N daté ;
- affectation principale/secondaire ;
- date début/fin ;
- statut actif ;
- staff ↔ specialty_catalog N:N ;
- spécialité principale facultative ;
- historique conservé.

### Exemple attendu

```text
Dr X
├── spécialité : Médecine générale
├── service principal : Médecine générale
└── Urgences : renfort du 01/08 au 31/08
```

### Règles

1. une affectation expirée ne donne plus de contexte actif ;
2. aucun lien cross-tenant ;
3. le retrait clôt la période sans supprimer l’historique ;
4. aucun fallback texte `department/specialty` après bascule du parcours staff ;
5. les rôles RBAC restent distincts des affectations métier.

## 8. HOS-BED-001 — Intégrité temporelle des lits

Objectif : garantir qu’une réservation/occupation incohérente ne peut pas être créée par concurrence, import ou futur endpoint.

Règles structurantes :

- une seule affectation active compatible par lit ;
- un séjour du même tenant ;
- une seule présence active compatible par séjour ;
- chronologie valide ;
- retries idempotents ;
- chevauchements historiques à traiter dans HOS-BED-001-D.

Dépendances : PostgreSQL réel, DBA, cadre infirmier.

## 9. HOS-BED-002 — Capacité et remise en état

Objectif : séparer existence, ouverture clinique, hygiène, maintenance et occupation.

Règles :

- `FREE` est une projection et non une commande ;
- disponible = installé + ouvert + prêt + non occupé/réservé ;
- départ physique → turnover ;
- maintenance impossible sur lit occupé sans processus préalable ;
- nettoyage/maintenance non comptés comme libres.

## 10. HOS-RBAC-001 — Contextualisation des droits

La segmentation des intentions A/B/C/D est livrée. Le reliquat n’est **pas** une nouvelle permission générique.

Le contexte restant doit s’appuyer sur :

- affectation active à l’unité ;
- relation de soin ;
- délégation/habilitation datée ;
- clearance de sortie ;
- tenant et éventuel scope plateforme.

Dépendances principales : HOS-STAFF-001 et HOS-DIS-001.

## 11. HOS-ADM-001 — Demandes, préadmissions et réservations

Objectif : ne plus créer systématiquement un séjour directement `EN_COURS` avec lit.

Règles :

- demande médicale ;
- décision ;
- préadmission ;
- recherche de compatibilité ;
- réservation expirante ;
- arrivée confirmée ;
- urgence pouvant différer l’administratif avec motif/échéance ;
- lit occupé uniquement à l’arrivée confirmée.

Dépendances : HOS-BED, HOS-ORG, HOS-LOC, RBAC.

## 12. HOS-MOV-001 — Présence et transferts

Objectif : connaître position et responsabilité du patient à chaque instant.

Règles :

- mouvement avec motif et jalons ;
- acceptation destination avant départ sauf urgence documentée ;
- changement de responsabilité au handoff défini ;
- événements immuables ;
- idempotence après coupure réseau ;
- une seule présence active cohérente.

Dépendances : HOS-ADM, HOS-LOC, HOS-STAFF.

## 13. HOS-DIS-001 — Sortie multi-étapes

Objectif : séparer décision médicale, clearance administrative, départ physique et lit prêt.

Règles :

- décision médicale ≠ libération du lit ;
- dette éventuellement autorisée selon politique, jamais masquée ;
- départ physique → clôture présence + turnover ;
- nettoyage validé avant disponibilité ;
- décès, contre-avis, évasion, transfert externe = issues explicites.

## 14. HOS-RES-001 — Ressources et équipements

Objectif : gérer espaces/équipements partagés, réservations, pannes et maintenance.

Règles :

- pas de réservation ferme chevauchante pour ressource non partageable ;
- panne → indisponibilité + impact des réservations ;
- remise en service validée ;
- équipements mobiles et maintenance préventive supportés.

Dépendances : HOS-LOC, HOS-STAFF.

## 15. HOS-PATH-001 — Parcours transverse

Objectif : afficher position, responsable, prochaine étape et actions en attente sans déplacer les règles critiques vers Angular.

Règles :

- work-item avec propriétaire, échéance, statut, idempotency key ;
- modules sources restent maîtres de leurs faits ;
- visibilité filtrée par profil/contexte ;
- handoff ouvert jusqu’à acceptation/arrivée ;
- événements corrélés et rejouables.

Dépendances : HOS-ORG, HOS-LOC, HOS-STAFF, HOS-MOV.

## 16. HOS-KPI-001 — Capacité et saturation

Objectif : produire des indicateurs réconciliables.

Règles :

- formule/source/fuseau/version pour chaque KPI ;
- capacité installée, ouverte, prête, réservée et occupée distinguées ;
- agrégats direction minimisant les données patient ;
- incohérences signalées ;
- corrections tardives versionnées.

Dépendances : HOS-BED-002, HOS-MOV.

## 17. HOS-INT-001 — Interopérabilité et réseau instable

Objectif : versionner les contrats et rendre les commandes critiques rejouables.

Règles :

- aucune donnée clinique critique confiée au cache navigateur sans politique/chiffrement validés ;
- admission, réservation, mouvement idempotents ;
- backend maître des conflits ;
- fraîcheur des données visible en mode dégradé ;
- identifiants stables pour Organization/Location/Encounter et autres mappings pertinents.

## 18. Definition of Ready globale

- ticket et documentation avant code ;
- modèle de données et contrat API versionnés ;
- stratégie migration/rollback écrite ;
- permissions et tests identifiés ;
- reviewers métier/technique nommés ;
- aucune dépendance à un champ libre legacy sans plan de retrait ;
- aucun développement serveur.

## 19. Definition of Done globale

- documentation fonctionnelle, technique, API, data et tests alignée ;
- migration testée sur PostgreSQL ;
- Maven strict vert ;
- Angular tests + build production verts pour tout changement frontend ;
- isolation tenant et matrice négative vérifiées ;
- audit des actions sensibles ;
- responsive 320/375/768/1366 ;
- FR/EN et light/dark ;
- aucun Angular Material ;
- changelog et tracking alignés ;
- PR fusionnée sur `main` avant démarrage du lot dépendant.

## 20. Ordre de réalisation courant

```text
DONE  HOS-BED / RBAC phase 0
  ↓
DONE  HOS-ORG-001-A #130 / PR #133 / V87
  ↓
NEXT  HOS-LOC-001-A #131
  ↓
NEXT  HOS-STAFF-001-A #132
  ↓
      données structurées + répétition démo #127
  ↓
      HOS-BED-002 complet
  ↓
      HOS-ADM → HOS-MOV → HOS-DIS
  ↓
      HOS-RES
  ↓
      HOS-PATH → HOS-KPI → HOS-INT
```

HOS-STAFF intervient immédiatement après HOS-LOC dans le jalon actuel parce que la démonstration doit créer des professionnels correctement rattachés, sans réintroduire de saisie libre.

## 21. Capacité et engagement

Les estimations globales issues de l’audit restent des ordres de grandeur et non une promesse de remplissage.

Pour le jalon court :

- HOS-ORG-001-A : 9 SP, techniquement livré ;
- HOS-LOC-001-A : 9 SP ;
- HOS-STAFF-001-A : 9 SP ;
- QA démo #127 : répétition et GO/NO-GO après intégration.

Chaque lot est développé et fusionné séparément. Aucune PR « monstre » combinant organisation, géographie et personnel n’est autorisée.

## 22. Risques et garde-fous

- **Risque migration legacy** : aucun mapping automatique par nom ; remapping explicite uniquement si la sémantique est prouvée.
- **Risque tenant** : FK composites + filtres explicites + tests cross-tenant.
- **Risque i18n** : codes stables, libellés localisés hors faits métier quand nécessaire.
- **Risque UI** : mobile-first, composants réutilisables, pas de logique métier dupliquée côté Angular.
- **Risque calendrier démo** : seuls les blockers P0 justifient une dérogation au gel ; la qualité et les tests ne sont jamais supprimés pour gagner du temps.
- **Risque dette technique** : aucun alias/fallback legacy durable n’est accepté sous prétexte de rétrocompatibilité pendant la phase de développement.

## 23. Mise à jour jalon HOS-LOC / HOS-STAFF — 24/07/2026

Cette section met à jour l'état opérationnel sans réécrire les sections historiques ci-dessus.

### HOS-LOC-001-A / #131

- **DONE** — PR #137 squash-mergée dans `main@e495477ea02beb05596b20b656bc092bd8fbbd83` ;
- effort réévalué : **13 SP** ;
- Flyway V88–V91 ;
- géographie/espaces, lits sur `space_id`, rattachements datés unité-espace et hospitalisation UUID livrés ;
- gates #1196/#1202 verts ;
- reste : recette humaine #127.

### HOS-STAFF-001-A / #132

- **READY TECHNIQUE** — PR #140 ;
- estimation maintenue : **9 SP** ;
- Flyway V92–V95 ;
- affectations datées staff↔unité et staff↔spécialité, rôle contextuel distinct du RBAC global ;
- suppression physique de `users.department/users.specialty` après preflight fail-fast ;
- UI staff structurée et portail rendez-vous migré vers `specialtyCode` / `organizationalUnitId` ;
- gate combiné #1262 : Maven strict SUCCESS, tests Angular SUCCESS, build production SUCCESS ;
- aucun mapping automatique par texte libre ;
- aucune action PROD/RECETTE ;
- reste avant clôture : gate final post-documentation sur le head exact, revue, squash merge #140, puis recette humaine #127.

### Ordre courant actualisé

```text
DONE  HOS-BED / RBAC phase 0
  ↓
DONE  HOS-ORG-001-A #130
  ↓
DONE  HOS-LOC-001-A #131
  ↓
READY HOS-STAFF-001-A #132 / PR #140
  ↓
      répétition démo #127
  ↓
      HOS-BED-002 complet
  ↓
      HOS-ADM → HOS-MOV → HOS-DIS
  ↓
      HOS-RES
  ↓
      HOS-PATH → HOS-KPI → HOS-INT
```
