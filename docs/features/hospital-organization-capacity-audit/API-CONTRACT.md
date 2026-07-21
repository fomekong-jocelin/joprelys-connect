# Contrat API — Inventaire actuel et principes de la cible

**Référence** : AUDIT-20260721 / EPIC-0027  
**Statut** : inventaire prouvé pour l'existant ; proposition non implémentée pour la cible

## 1. API actuelle du périmètre

| Ressource | Endpoints principaux | Permission | Limite structurante |
|---|---|---|---|
| Organisations | `/api/organizations` CRUD/statut/admin/API keys | `ORGANIZATION_MANAGE` au contrôleur | tenant plat |
| Structure | `/api/spatial/configuration`, `/wards`, `/rooms`, `/beds` | `SPATIAL_CONFIGURATION_MANAGE` | hard delete conditionnel, pas de niveaux géographiques |
| Occupation | `/api/spatial/wards`, `/wards/{id}/occupancy` | `HOSPITALIZATION_READ` | seulement total/occupé |
| État lit | `/api/spatial/beds/{id}/status` | `HOSPITALIZATION_MANAGE` | commande générique incohérente possible |
| Transfert | `/api/spatial/transfers` | `HOSPITALIZATION_MANAGE` | séjour + nouveau lit uniquement |
| Hospitalisation | `/api/hospitalizations` et sous-ressources | `HOSPITALIZATION_READ/MANAGE`, CRO `CLINICAL_*` | admission/sortie directes, droits trop larges |
| Urgence | `/api/emergencies` + triage/médico-légal/documents | `EMERGENCY_*` granulaires | pas de box/présence/handoff atomique |
| Visite | `/api/visits` | `VISIT_*` | service/orientation texte |
| Laboratoire | `/api/lab-orders` | `LAB_*` | transitions de statut non contraintes |
| Pharmacie | `/api/public/pharmacy/prescriptions` | vérification PIN ; route web gardée | lockout mémoire, pas de pharmacie cible/stock atomique |
| Stock | `/api/pharmacy/stocks` | `PHARMACY_STOCK_MANAGE` | stock organisationnel unique |
| Personnel/RBAC | `/api/staff`, `/api/rbac` | `USER_*`, `RBAC_*` | aucun rattachement métier daté |

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
| `BED_PERIOD_CONFLICT` | 409 | réservation/occupation chevauchante |
| `PATIENT_SPACE_INCOMPATIBLE` | 422 | âge, sexe, isolement, niveau ou équipement |
| `RESERVATION_EXPIRED` | 409 | arrivée après expiration |
| `MOVEMENT_INVALID_TRANSITION` | 409 | ordre de jalons invalide |
| `DISCHARGE_PREREQUISITE_MISSING` | 422 | étape obligatoire manquante |
| `STAFF_ASSIGNMENT_REQUIRED` | 403 | acteur non affecté/délégué |
| `CLINICAL_PRIVILEGE_REQUIRED` | 403 | habilitation insuffisante |
| `TENANT_RELATION_MISMATCH` | 409 | relations entre tenants différents |
| `IDEMPOTENCY_KEY_REUSED` | 409 | même clé avec payload différent |

## 6. Compatibilité et dépréciation

Pendant la migration, l'adaptateur legacy résout les noms actuels vers les nouveaux IDs et maintient les snapshots de documents. Toute ambiguïté retourne une erreur de migration, jamais un choix silencieux. Les endpoints v1 sont marqués deprecated avec date de retrait ; leur suppression exige une version MAJOR et une release note.

## 7. Tests de contrat

- isolation tenant et unité ;
- idempotence, optimistic locking et courses concurrentes ;
- validation des transitions et Problem Details ;
- compatibilité des filtres/pagination ;
- non-divulgation des autres patients dans les conflits ;
- mapping des ressources d'interopérabilité retenues après validation.
