# Modèle de données cible — Organisation, capacité et parcours hospitalier

**Statut** : proposition soumise à ADR et ateliers métier  
**Référence** : AUDIT-20260721 / EPIC-0027

## 1. Principes de modélisation

- Les structures juridique, organisationnelle et géographique sont séparées.
- Chaque niveau est facultatif ; aucune chaîne complète n'est requise pour une petite structure.
- Les relations susceptibles de changer sont datées avec `valid_from`, `valid_to` et un motif.
- Les identifiants UUID stables portent les relations. Les noms sont des attributs ou snapshots documentaires.
- Les événements de séjour et de mouvement sont append-only ; une correction ajoute un événement.
- Toute table tenant-scoped porte `organization_id` et des FK composites empêchent les relations inter-tenant accidentelles.
- Toutes les dates métier utilisent `TIMESTAMP WITH TIME ZONE` et sont manipulées par `Instant` côté Java.
- L'occupation et la disponibilité sont dérivées de faits, pas modifiées par un setter générique.

## 2. Vue d'ensemble

```text
OrganizationGroup 1 ── 0..* HealthcareFacility
HealthcareFacility 1 ── 0..* OrganizationalUnit
OrganizationalUnit 0..1 ── 0..* OrganizationalUnit (parent facultatif)
OrganizationalUnit * ── * Specialty (UnitSpecialty datée)

HealthcareFacility 1 ── 0..* LocationNode
LocationNode 0..1 ── 0..* LocationNode (SITE/BUILDING/FLOOR/ZONE/SPACE)
Space 1 ── 0..1 InpatientRoom ── 0..* Bed
OrganizationalUnit * ── * Space (UnitSpaceAssignment datée)

ProfessionalProfile 1 ── * Employment ── 1 HealthcareFacility
Employment 1 ── * UnitAssignment ── 1 OrganizationalUnit
ProfessionalProfile * ── * Specialty / ClinicalPrivilege
UserAccount 1 ── * ApplicationRoleAssignment (distinct du métier)

Patient 1 ── * CareEpisode 1 ── * Encounter
CareEpisode 1 ── * PatientPresence / PatientMovement / WorkItem
AdmissionRequest 0..1 ── 1 HospitalStay
HospitalStay 1 ── * BedReservation / BedAssignment / DischargeProcess

Resource * ── * Space (ResourceLocationAssignment datée)
Resource 1 ── * ResourceReservation / ResourceDowntime
```

## 3. Référentiel juridique

### 3.1 `organization_group`

Rôle : réseau, groupe, holding sanitaire ou autorité de rattachement facultative.

| Colonne | Contrainte |
|---|---|
| `id` | PK UUID |
| `code` | unique stable, insensible à la casse |
| `name` | obligatoire |
| `status` | `DRAFT/ACTIVE/SUSPENDED/CLOSED/ARCHIVED` |
| `valid_from/valid_to` | intervalle cohérent |
| `country_code`, `timezone` | obligatoires à l'activation |

### 3.2 `healthcare_facility`

Remplace progressivement le sens actuel de `organizations` sans supprimer le tenant. Une organisation existante devient un établissement. `facility_type` provient d'un catalogue (`HOSPITAL`, `CLINIC`, `MEDICAL_CENTER`, `PRACTICE`, `LABORATORY`, `IMAGING_CENTER`, `PHARMACY`, `SPECIALIZED_FACILITY`).

Cardinalités : groupe facultatif 0..1 ; plusieurs établissements par groupe ; un tenant de données par établissement dans un premier incrément. Une stratégie de tenant de groupe nécessite une décision distincte.

## 4. Référentiel organisationnel

### 4.1 `organizational_unit`

| Champ | Description |
|---|---|
| `id`, `organization_id` | identité et tenant |
| `parent_unit_id` | parent facultatif du même établissement |
| `unit_type` | pôle, département médical/administratif, service, unité de soins, unité fonctionnelle, centre de responsabilité, transversal |
| `code`, `name`, `short_name` | code unique tenant + libellés |
| `clinical` | distingue unité clinique d'une unité administrative |
| `status` | cycle de vie du référentiel |
| `valid_from/valid_to` | dates d'effet |
| `medical_manager_assignment_id` | lien vers responsabilité datée, pas un nom |
| `administrative_manager_assignment_id` | idem |
| `cost_center_code` | facultatif, distinct du centre de responsabilité |

Contraintes : pas de cycle dans l'arbre ; parent du même tenant ; les types parent/enfant autorisés sont configurables ; aucune profondeur obligatoire.

### 4.2 `specialty` et `unit_specialty`

Catalogue local ou national versionné. `unit_specialty(unit_id, specialty_id, is_primary, valid_from, valid_to)` permet une clinique multi-spécialités et un service partagé.

### 4.3 `unit_relationship`

Relation datée pour les rattachements non hiérarchiques : `FUNCTIONALLY_ATTACHED_TO`, `SHARED_WITH`, `SUPPORTED_BY`, `REPORTS_TO`. Elle évite de détourner l'arbre principal.

## 5. Référentiel géographique

### 5.1 `location_node`

Arbre physique facultatif par établissement : `SITE`, `BUILDING`, `FLOOR`, `ZONE`, `SPACE`. Un cabinet peut créer directement un `SPACE` sous l'établissement ; un hôpital peut détailler tous les niveaux.

Champs : code, nom, type, parent, adresse/coordonnées facultatives, accessibilité, statut, dates d'effet, timezone héritée. Les codes sont uniques par parent et insensibles à la casse.

### 5.2 `space`

Extension 1–1 de `location_node` lorsque `node_type=SPACE`.

| Champ | Description |
|---|---|
| `space_type` | consultation, soins, examen, intervention, bloc, SSPI, chambre, box urgence, laboratoire, imagerie, attente, pharmacie, dépôt, isolement, morgue, administratif |
| `nominal_capacity` | capacité physique, non assimilée aux lits ouverts |
| `care_level` | standard, surveillance continue, intensif, réanimation, etc. |
| `sex_policy`, `age_policy` | politiques configurables |
| `isolation_capability` | aucune/contact/gouttelettes/air, catalogue validé |
| `bookable`, `shared` | capacités de réservation |
| `status` | cycle administratif |

### 5.3 `inpatient_room`

Extension facultative de `space`. Elle porte catégorie hôtelière, politique de cohabitation, capacité lits installables et contraintes d'accompagnant. Le niveau de soins et l'isolement restent des champs séparés.

### 5.4 `unit_space_assignment`

Relation N–N datée : une unité utilise plusieurs espaces ; un espace peut être principal, partagé ou temporairement affecté. Champs : `usage_type`, priorité, capacité réservée, approbateur, motif, dates.

## 6. Lits et capacité

### 6.1 `bed`

| Groupe | Champs |
|---|---|
| Identité | `id`, `organization_id`, `room_id`, `code`, `label` |
| Existence | `existence_status`, `installed_at`, `decommissioned_at` |
| Ouverture | `operational_status`, `operational_reason_code` |
| Hygiène | `hygiene_status` |
| Caractéristiques | type, niveau de soins, facturable, temporaire, isolement |
| Concurrence | `version` |
| Audit | dates et acteur des dernières transitions, événements séparés |

La disponibilité `FREE/RESERVED/OCCUPIED/UNAVAILABLE` est une projection, non un champ libre.

### 6.2 `bed_state_event`

Journal append-only avec axe, ancienne/nouvelle valeur, acteur, rôle effectif, raison, corrélation, date effective et date d'enregistrement. Il rend les changements de capacité auditables.

### 6.3 `capacity_opening_period`

Historise les lits ouverts/fermés planifiés et permet de calculer la capacité réelle sur une période. Les fermetures temporaires portent cause, décideur et date de révision.

## 7. Ressources et équipements

### 7.1 `resource`

Ressource humaine non nominative ou matérielle : équipement médical, table opératoire, respirateur, échographe, véhicule, fauteuil, poste, ressource générique. `equipment` étend `resource` avec fabricant, modèle, série, criticité, classe, dates de qualification et maintenance.

### 7.2 Relations

- `resource_location_assignment` : emplacement daté ;
- `resource_ownership_assignment` : unité responsable et unité utilisatrice ;
- `resource_reservation` : plage, demandeur, patient/encounter facultatif, statut ;
- `resource_downtime` : panne, maintenance, désinfection, fermeture ;
- `maintenance_work_order` : diagnostic, intervenant, pièces, remise en service.

Une exclusion temporelle empêche les réservations fermes incompatibles d'une ressource non partageable.

## 8. Personnel et habilitations

### 8.1 Entités

| Entité | Rôle |
|---|---|
| `person` | identité d'une personne, distincte du compte |
| `professional_profile` | profession, numéro d'enregistrement, statut professionnel |
| `professional_specialty` | spécialités datées avec source de vérification |
| `employment` | lien daté personne-établissement, type et statut |
| `unit_assignment` | service/unité, fonction, poste, principal/secondaire/temporaire |
| `clinical_privilege` | actes/périmètres autorisés et dates |
| `duty_period` | planning, garde, astreinte |
| `delegation` | délégant, délégataire, périmètre, dates, motif |
| `application_role_assignment` | rôle applicatif, séparé des entités métier |

Une personne peut travailler dans plusieurs établissements sans dupliquer son identité. Le retrait met fin à l'affectation ; il ne supprime aucun historique.

## 9. Épisode, présence et parcours patient

### 9.1 `care_episode` et `encounter`

`CareEpisode` regroupe une continuité de prise en charge ; `Encounter` représente un contact : accueil, consultation, urgence, laboratoire, imagerie, bloc, hospitalisation. Les entités actuelles peuvent rester leurs extensions durant la migration.

### 9.2 `patient_presence`

Position effective : patient, encounter, espace, unité responsable, professionnel/équipe responsable, `arrived_at`, `departed_at`, statut (`EXPECTED`, `PRESENT`, `IN_TRANSIT`, `DEPARTED`).

Contrainte : au plus une présence physique active, sauf exceptions documentées comme dialyse ambulatoire virtuelle ; la règle doit être configurable et auditée.

### 9.3 `patient_movement`

Événement immuable avec origine, destination, demandeur, approbateur, transporteur, raisons, jalons demandé/accepté/départ/arrivée/annulation et corrélation de l'épisode.

### 9.4 `work_item`

Prochaine action ou attente : type, propriétaire (équipe/unité/personne), priorité, échéance, dépendances, statut et outcome. Permet de répondre à « que reste-t-il à faire ? » sans déduire des dizaines de statuts.

## 10. Hospitalisation

### 10.1 `admission_request`

Patient, prescripteur, motif, niveau de soins, unité souhaitée, priorité, type (`PLANNED`, `EMERGENCY`, `DIRECT`, `TRANSFER_IN`), date souhaitée, contraintes et décision.

### 10.2 `preadmission`

Identité/contacts, représentant légal, couverture, documents, autorisations et checklist. La complétude administrative peut être différée en urgence avec motif et échéance.

### 10.3 `hospital_stay`

Référence l'épisode et l'unité responsable actuelle. Le statut suit la machine définie dans le rapport. Le type d'issue est séparé du statut technique de clôture.

### 10.4 `bed_reservation`

| Champ critique | Règle |
|---|---|
| `bed_id`, `stay/admission_request_id` | FK tenant cohérentes |
| `start_at`, `expires_at`, `planned_end_at` | expiration obligatoire avant consommation |
| `status` | hold/confirmed/consumed/expired/cancelled/no-show |
| `priority`, `reason` | obligatoires selon politique |
| `version`, `idempotency_key` | concurrence/reprise |

### 10.5 `bed_assignment`

Remplace/enrichit l'actuel : FK séjour réelle, `start_at/end_at`, origine, motif, mouvement, acteur, correction. Une affectation active matérialise l'occupation.

### 10.6 `discharge_process` et `turnaround_task`

Le premier trace décision médicale, clearance administrative, départ physique et issue. Le second trace nettoyage/désinfection/inspection jusqu'à `READY`.

## 11. Contraintes PostgreSQL indispensables

Exemples à adapter après prototype PostgreSQL/H2 de test :

```sql
-- Une seule affectation active par lit.
CREATE UNIQUE INDEX ux_bed_assignment_active
ON bed_assignments (bed_id)
WHERE end_at IS NULL AND status = 'ACTIVE';

-- Une seule affectation active par séjour.
CREATE UNIQUE INDEX ux_stay_assignment_active
ON bed_assignments (hospital_stay_id)
WHERE end_at IS NULL AND status = 'ACTIVE';

-- Aucune période chevauchante, y compris sur données clôturées/importées.
ALTER TABLE bed_assignments ADD CONSTRAINT ex_bed_assignment_period
EXCLUDE USING gist (
  bed_id WITH =,
  tstzrange(start_at, COALESCE(end_at, 'infinity'), '[)') WITH &&
) WHERE (status IN ('ACTIVE', 'CLOSED'));
```

Autres contraintes :

- `end_at > start_at` ;
- expiration de réservation postérieure au début ;
- index unique actif patient/séjour selon politique ;
- FK composites `(organization_id, id)` sur tous les parents tenant-scoped ;
- statut contrôlé côté DB et Java depuis une définition partagée/testée ;
- impossibilité de supprimer une entité référencée par un fait clinique ;
- code stable unique en `lower(code)` dans son périmètre.

H2 ne doit pas dicter l'affaiblissement du schéma de production. Les contraintes PostgreSQL doivent être testées avec Testcontainers/PostgreSQL ; H2 peut rester pour les tests unitaires non structurels.

## 12. Historisation

| Objet | Stratégie |
|---|---|
| Référentiels | statut + validité ; archivage, jamais hard delete après usage |
| Relations organisationnelles | tables datées sans écrasement |
| État lit/espace/ressource | événement append-only + projection courante |
| Réservation/affectation | lignes immuables après clôture ; correction compensatrice |
| Mouvement patient | événement immuable et jalons datés |
| Rôles/habilitations | affectations datées + audit before/after |
| Documents | snapshot des libellés et hash, référence aux IDs sources |

## 13. Migration depuis le modèle actuel

1. Préflight V74 : recenser et typer toute `ward` sans chambre avant déploiement.
2. Créer les nouvelles tables sans retirer les anciennes.
3. Transformer chaque `organization` en `healthcare_facility`.
4. Transformer chaque `ward` en `organizational_unit(type=SERVICE)`.
5. Transformer chaque `room` en `location_node(SPACE)` + `space` + `inpatient_room`.
6. Migrer les lits et convertir les quatre statuts en axes cibles.
7. Rejouer `bed_assignments`, détecter doublons, chevauchements, tenants incohérents et hospitalisations inexistantes.
8. Ajouter les FK et contraintes uniquement après rapport de réconciliation signé.
9. Introduire des API v2 et une couche anti-corruption alimentant temporairement les champs texte historiques.
10. Migrer Angular écran par écran, comparer les projections de capacité.
11. Geler les écritures legacy, puis supprimer les contrats historiques dans une version breaking distincte.

Tout conflit de données va dans une table/quarantaine de migration ; aucune ligne clinique ne doit être supprimée automatiquement.

## 14. Données minimales et facultatives

- Activation établissement : code, nom, type, pays, timezone.
- Activation unité : code, nom, type, établissement, date d'effet.
- Activation espace : code, type, parent facultatif, statut, capacité si applicable.
- Installation lit : code, chambre, date, type ; caractéristiques cliniques selon politique.
- Admission : patient ou identité provisoire, décision clinique, établissement/unité, début et responsable habilité.
- Réservation : cible, période, expiration, priorité et demande.
- Mouvement : patient/séjour, origine, destination, motif, demandeur.
- Sortie : décision médicale ; clearance et départ physique séparés.

## 15. Validations requises

- médecin responsable : types d'unités, niveaux de soins, issues et compatibilités ;
- cadre infirmier : statuts lit, bionettoyage, transferts, charge ;
- admissions : préadmission, représentant légal, clearance et no-show ;
- biomédical : ressources, criticité et maintenance ;
- laboratoire/imagerie/pharmacie : workflows et traçabilité spécifique ;
- DPO/RSSI/juriste local : accès contextuel, rétention, mineurs, décès et transfert ;
- DBA/architecte : contraintes temporelles, migration et performance.
