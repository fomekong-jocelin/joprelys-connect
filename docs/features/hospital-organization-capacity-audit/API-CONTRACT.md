# Contrat API — Inventaire actuel et principes de la cible

**Référence** : AUDIT-20260721 / EPIC-0027  
**Statut** : inventaire prouvé pour l'existant ; cible partiellement implémentée par les incréments de phase 0

## 1. API actuelle du périmètre

| Ressource | Endpoints principaux | Permission | Limite structurante |
|---|---|---|---|
| Organisations | `/api/organizations` CRUD/statut/admin/API keys | `ORGANIZATION_MANAGE` au contrôleur | tenant plat |
| Structure | `/api/spatial/configuration`, `/wards`, `/rooms`, `/beds` | `SPATIAL_CONFIGURATION_MANAGE` | hard delete conditionnel, pas de niveaux géographiques |
| Occupation | `/api/spatial/wards`, `/wards/{id}/occupancy` | `HOSPITALIZATION_READ` | projection par service, sans snapshots temporels |
| Capacité du lit | `/api/spatial/beds/{id}/capacity-status` | `BED_OPERATIONAL_STATUS_MANAGE` | motif codifié et journal disponibles ; périodes d'effet et sélecteur UI absents |
| État lit legacy | `/api/spatial/beds/{id}/status` | `BED_OPERATIONAL_STATUS_MANAGE` | endpoint de supervision conservé temporairement, journalisé `LEGACY_SUPERVISION` |
| Nettoyage du lit | `/api/spatial/beds/{id}/cleaning-status` | `BED_CLEANING_MANAGE` | motifs et journal disponibles ; pas de tâche assignée ni preuve structurée |
| Maintenance du lit | `/api/spatial/beds/{id}/maintenance-status` | `BED_MAINTENANCE_MANAGE` | motifs et journal disponibles ; pas d'ordre de travail |
| Historique du lit | `/api/spatial/beds/{id}/state-history` | `HOSPITALIZATION_READ` | chronologie tenant-aware non paginée ; conservation liée à la ligne physique du lit |
| Transfert | `/api/spatial/transfers` | `HOSPITALIZATION_TRANSFER` | séjour + nouveau lit uniquement ; interdit après décision médicale ; nettoyage source historisé |
| Hospitalisation | `/api/hospitalizations` et sous-ressources | lecture `HOSPITALIZATION_READ` ; écritures séparées `HOSPITALIZATION_ADMIT`, `HOSPITALIZATION_NOTE_WRITE`, `HOSPITALIZATION_CONSENT_RECORD`, `HOSPITALIZATION_CARE_WRITE`, `HOSPITALIZATION_MEDICATION_ADMINISTER`, `HOSPITALIZATION_CONSUMABLE_RECORD` ; sortie `HOSPITALIZATION_DISCHARGE_DECIDE` ; départ `HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM` ; CRO `CLINICAL_*` | clearance administrative, ABAC unité/relation de soin et corrections compensatoires absents |
| Urgence | `/api/emergencies` + triage/médico-légal/documents | `EMERGENCY_*` granulaires | pas de box/présence/handoff atomique |
| Visite | `/api/visits` | `VISIT_*` | service/orientation texte |
| Laboratoire | `/api/lab-orders` | `LAB_*` | transitions de statut non contraintes |
| Pharmacie | `/api/public/pharmacy/prescriptions` | vérification PIN ; route web gardée | lockout mémoire, pas de pharmacie cible/stock atomique |
| Stock | `/api/pharmacy/stocks` | `PHARMACY_STOCK_MANAGE` | stock organisationnel unique |
| Personnel/RBAC | `/api/staff`, `/api/rbac` | `USER_*`, `RBAC_*` | aucun rattachement métier daté |

### 1.1 Rôles système hospitaliers

| Rôle | Droits hospitaliers par défaut |
|---|---|
| `MEDECIN` | lecture, admission, notes, traçabilité des consentements, soins, transfert et décision médicale de sortie ; aucune administration médicamenteuse ni consommation par défaut, aucun départ physique ni opération technique du lit |
| `INFIRMIER` | lecture, notes, traçabilité des consentements, soins, administration médicamenteuse, consommables et transfert ; aucune admission, décision de sortie, confirmation de départ ni opération technique du lit |
| `RESPONSABLE_HOSPITALISATION` | lecture, admission, transfert, confirmation du départ physique, supervision de capacité, nettoyage et maintenance ; aucune écriture clinique ni décision médicale de sortie |
| `AGENT_HYGIENE` | lecture d'occupation et circuit de nettoyage uniquement |
| `TECHNICIEN_MAINTENANCE` | lecture d'occupation et circuit de maintenance uniquement |
| `ADMIN_CLINIQUE` | ensemble des droits de l'établissement, hors exclusions plateforme/portail existantes |

Les rôles personnalisés peuvent recevoir les permissions séparément. Le backend reste la source de vérité ; masquer un bouton ne constitue jamais une autorisation.

### 1.2 Écritures de séjour séparées par HOS-RBAC-001-C/D

Les URL et payloads existants sont conservés ; seule l'autorité exigée est spécialisée :

| Intention | Endpoint | Permission dédiée |
|---|---|---|
| admission | `POST /api/hospitalizations` | `HOSPITALIZATION_ADMIT` |
| note/transmission | `POST /api/hospitalizations/{id}/notes` | `HOSPITALIZATION_NOTE_WRITE` |
| traçabilité consentement | `POST /api/hospitalizations/{id}/consents` | `HOSPITALIZATION_CONSENT_RECORD` |
| soin journalier | `POST /api/hospitalizations/{id}/daily-cares` | `HOSPITALIZATION_CARE_WRITE` |
| administration effective d'un médicament | `POST /api/hospitalizations/{id}/medication-administrations` | `HOSPITALIZATION_MEDICATION_ADMINISTER` |
| consommable réellement utilisé | `POST /api/hospitalizations/{id}/patient-consumptions` | `HOSPITALIZATION_CONSUMABLE_RECORD` |

`HOSPITALIZATION_MEDICATION_ADMINISTER` ne permet pas de prescrire. `HOSPITALIZATION_CONSUMABLE_RECORD` ne permet pas de gérer le stock. `HOSPITALIZATION_CONSENT_RECORD` autorise la traçabilité du consentement et de la pièce associée, sans se substituer à l'information médicale ni à la décision du patient.

`HOSPITALIZATION_MANAGE` est supprimée du catalogue actif par HOS-RBAC-001-D et retirée du référentiel persistant par Flyway V86. Aucun endpoint, écran ou rôle système ne l'utilise comme fallback. Les rôles personnalisés doivent être composés explicitement avec les permissions dédiées nécessaires ; aucun remapping automatique n'est effectué.

### 1.3 Workflow de sortie implémenté par HOS-DIS-001-A

```text
POST /api/hospitalizations/{stayId}/discharge
POST /api/hospitalizations/{stayId}/physical-departure
```

La première commande enregistre le diagnostic, les consignes, le caractère contre avis médical, l'auteur et l'heure de décision. Le séjour reste `EN_COURS`, l'affectation demeure active, le lit reste occupé et le document final n'est pas disponible.

La deuxième commande exige une confirmation explicite et la permission `HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM`. Elle renseigne la présence réelle, clôt le séjour et l'affectation, fixe `dischargedAt` à l'heure du départ physique, génère le PDF, place le lit en nettoyage et crée un événement `CLEANING_AFTER_DEPARTURE`.

Les sorties historiques sont rétrocompatibles : la migration V83 déduit décision et départ physique de leur ancien `discharged_at`. L'annulation/correction formelle d'une décision et la clearance administrative restent hors de cet incrément.

### 1.4 Historique des états de lit implémenté par HOS-BED-002-D

Les commandes spécialisées exigent désormais un `reasonCode` compatible avec la transition et acceptent une note de 500 caractères maximum :

```json
{
  "status": "MAINTENANCE",
  "reasonCode": "MAINTENANCE_CORRECTIVE",
  "note": "Frein du lit défectueux"
}
```

Chaque mutation effective crée un événement contenant `axis`, `previousValue`, `newValue`, `reasonCode`, `reasonNote`, `actorId`, `actorDisplayName`, `source` et `occurredAt`.

Les motifs `CLEANING_AFTER_TRANSFER` et `CLEANING_AFTER_DEPARTURE` sont réservés aux workflows automatiques. `CAPACITY_OTHER` et `CLEANING_INCIDENT` exigent une note. La lecture de l'historique ne retourne aucune identité patient.

## 2. Principes API cibles

- Préfixe versionné `/api/v2` pendant la coexistence.
- Identifiants UUID dans les relations ; jamais de nom comme clé de commande.
- `Idempotency-Key` obligatoire pour admission, réservation, mouvement, sortie physique et dispensation.
- `If-Match`/version optimiste pour les référentiels et ordres modifiables.
- Erreurs RFC 9457/Problem Details avec `code`, `correlationId`, violations et alternatives sûres.
- Dates ISO-8601 avec offset ; stockage UTC.
- Permissions évaluées backend avec établissement, unité, affectation, relation de soin et délégation.
- Pagination, filtres et champs de projection explicites pour les listes.
- Aucun endpoint `setStatus` générique pour les agrégats critiques ; une commande correspond à une intention métier.

## 3. Ressources cibles proposées

```text
/api/v2/facilities
/api/v2/facilities/{facilityId}/organization-units
/api/v2/facilities/{facilityId}/locations
/api/v2/spaces/{spaceId}
/api/v2/rooms/{roomId}/beds
/api/v2/beds/{bedId}/opening-periods
/api/v2/beds/{bedId}/downtimes
/api/v2/admission-requests
/api/v2/preadmissions
/api/v2/bed-reservations
/api/v2/hospital-stays
/api/v2/hospital-stays/{stayId}/bed-assignments
/api/v2/hospital-stays/{stayId}/movements
/api/v2/hospital-stays/{stayId}/discharge-process
/api/v2/turnaround-tasks
/api/v2/professionals/{id}/employments
/api/v2/professionals/{id}/unit-assignments
/api/v2/resources
/api/v2/resource-reservations
/api/v2/care-episodes/{id}/journey
/api/v2/capacity/snapshots
```

## 4. Commandes critiques

### Réserver un lit

`POST /api/v2/bed-reservations`

Entrée minimale : `admissionRequestId`, `bedId`, `startAt`, `plannedEndAt`, `expiresAt`, `priority`, `reasonCode`. Réponse `201`, ou `409 BED_PERIOD_CONFLICT` avec alternatives non sensibles.

### Confirmer une arrivée

`POST /api/v2/hospital-stays/{stayId}/arrivals`

Consomme une réservation, ouvre présence et affectation dans une transaction. `409` si la réservation a expiré ou si les préconditions de compatibilité ont changé.

### Demander et exécuter un mouvement

```text
POST /api/v2/hospital-stays/{stayId}/movements
POST /api/v2/movements/{movementId}/accept
POST /api/v2/movements/{movementId}/depart
POST /api/v2/movements/{movementId}/arrive
POST /api/v2/movements/{movementId}/cancel
```

Chaque transition vérifie le rôle spécifique, la version, la chronologie et l'état source/destination.

### Sortie

```text
POST /api/v2/hospital-stays/{stayId}/discharge/medical-decisions
POST /api/v2/hospital-stays/{stayId}/discharge/administrative-clearance
POST /api/v2/hospital-stays/{stayId}/discharge/physical-departure
```

Aucune première ou deuxième commande ne clôt l'affectation. La troisième le fait et crée le turnover.

La coexistence v1 couvre maintenant les intentions `medical-decisions` et `physical-departure`. La commande `administrative-clearance`, la ressource de processus et la tâche de turnover restent à implémenter avant la cible v2 complète.

### Remise en état

```text
POST /api/v2/turnaround-tasks/{id}/start
POST /api/v2/turnaround-tasks/{id}/complete
POST /api/v2/turnaround-tasks/{id}/validate
```

## 5. Codes d'erreur minimaux

| Code | HTTP | Sens |
|---|---:|---|
| `BED_NOT_OPERATIONAL` | 409 | lit fermé, bloqué ou maintenance |
| `BED_NOT_READY` | 409 | nettoyage/désinfection non validé |
| `BED_STATE_REASON_INVALID` | 400 | motif incompatible avec la transition demandée |
| `BED_STATE_REASON_NOTE_REQUIRED` | 400 | note obligatoire pour un motif ouvert ou incident |
| `BED_PERIOD_CONFLICT` | 409 | réservation/occupation chevauchante |
| `PATIENT_SPACE_INCOMPATIBLE` | 422 | âge, sexe, isolement, niveau ou équipement |
| `RESERVATION_EXPIRED` | 409 | arrivée après expiration |
| `MOVEMENT_INVALID_TRANSITION` | 409 | ordre de jalons invalide |
| `DISCHARGE_PREREQUISITE_MISSING` | 422 | décision, clearance ou confirmation obligatoire manquante |
| `DISCHARGE_ALREADY_DECIDED` | 409 | décision médicale déjà enregistrée |
| `PHYSICAL_DEPARTURE_ALREADY_CONFIRMED` | 409 | départ physique déjà confirmé |
| `STAFF_ASSIGNMENT_REQUIRED` | 403 | acteur non affecté/délégué |
| `CLINICAL_PRIVILEGE_REQUIRED` | 403 | habilitation insuffisante |
| `TENANT_RELATION_MISMATCH` | 409 | relations entre tenants différents |
| `IDEMPOTENCY_KEY_REUSED` | 409 | même clé avec payload différent |

## 6. Compatibilité et dépréciation

Pendant la migration, l'adaptateur legacy résout les noms actuels vers les nouveaux IDs et maintient les snapshots de documents. Toute ambiguïté retourne une erreur de migration, jamais un choix silencieux. Les endpoints v1 sont marqués deprecated avec date de retrait ; leur suppression exige une version MAJOR et une release note.

L'endpoint `/api/spatial/beds/{id}/status` est conservé comme commande de supervision pour les intégrations existantes. Ses changements sont historisés sous `LEGACY_SUPERVISION`. Son retrait sera préparé après inventaire des consommateurs et publication d'une date de fin de support.

Le chemin `/api/hospitalizations/{id}/discharge` est conservé pour compatibilité de nom, mais son effet est désormais limité à la décision médicale. Tout consommateur qui supposait une libération immédiate doit appeler explicitement `/physical-departure` avec le droit correspondant.

Pour HOS-RBAC-001-C/D, aucune URL ni structure de payload ne change. En revanche, le modèle d'autorisation est volontairement nettoyé : `HOSPITALIZATION_MANAGE` n'existe plus à partir de V86. Les rôles personnalisés doivent recevoir explicitement les permissions correspondant à leurs actions. Les tokens existants doivent être renouvelés après resynchronisation du catalogue.

## 7. Tests de contrat

- isolation tenant et unité ;
- refus de l'admission sans `HOSPITALIZATION_ADMIT` ;
- refus de l'écriture de note sans `HOSPITALIZATION_NOTE_WRITE` ;
- refus de la traçabilité du consentement sans `HOSPITALIZATION_CONSENT_RECORD` ;
- refus des soins sans `HOSPITALIZATION_CARE_WRITE` ;
- refus de l'administration médicamenteuse sans `HOSPITALIZATION_MEDICATION_ADMINISTER` ;
- refus de la consommation patient sans `HOSPITALIZATION_CONSUMABLE_RECORD` ;
- vérification que l'administration médicamenteuse n'accorde aucun droit de prescription ;
- vérification que la consommation patient n'accorde aucun droit de gestion de stock ;
- refus d'un contexte stale ne contenant que `HOSPITALIZATION_MANAGE` ;
- vérification V86 : permission legacy supprimée, association de rôle personnalisé supprimée, rôle conservé, aucun remapping automatique ;
- refus du transfert sans `HOSPITALIZATION_TRANSFER` ;
- refus du transfert après décision médicale de sortie ;
- refus de la décision de sortie sans `HOSPITALIZATION_DISCHARGE_DECIDE` ;
- refus du départ physique sans `HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM` ;
- décision médicale sans clôture du séjour, de l'affectation ni du lit ;
- refus du départ physique sans décision préalable ou sans confirmation explicite ;
- clôture de l'affectation et passage en nettoyage uniquement au départ réel ;
- impossibilité pour le nettoyage de terminer une maintenance et inversement ;
- refus des opérations techniques sur un lit affecté ;
- refus des motifs incompatibles et des motifs ouverts sans note ;
- motifs automatiques impossibles à soumettre manuellement ;
- intégrité tenant et domaines du journal sous PostgreSQL ;
- idempotence, optimistic locking et courses concurrentes ;
- validation des transitions et Problem Details ;
- compatibilité des filtres/pagination ;
- non-divulgation des autres patients dans les conflits ;
- mapping des ressources d'interopérabilité retenues après validation.
