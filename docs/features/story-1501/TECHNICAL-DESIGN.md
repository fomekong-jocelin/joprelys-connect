# TECHNICAL-DESIGN — Socle de notifications (backend) - STORY-1501

## 1. Objectif technique

Mettre en place la table SQL Flyway, l'entité JPA, le repository, le service d'envoi simulé et l'exposition des API REST du module de notifications sur le portail patient. Connecter la création de demandes d'accès externes et l'accès d'urgence Brise-Glace pour émettre des notifications.

## 2. Stack concernée

- [x] Spring Boot
- [x] Base de données (Flyway migration SQL + H2/PostgreSQL)
- [ ] Angular (traité dans STORY-1502)
- [x] Documentation

## 3. Contraintes projet obligatoires

- Backend Spring Boot : Maven uniquement (`pom.xml`, `mvnw`).
- Modèle de données : table SQL `notifications` versionnée via Flyway.
- Sécurisation : filtrage par IDOR au niveau des API patient.

## 4. Architecture cible

Une nouvelle entité `NotificationEntity` sera stockée :
```text
Table: notifications
  ├── id (UUID, PK)
  ├── patient_id (UUID, FK patients)
  ├── title (VARCHAR)
  ├── message (VARCHAR)
  ├── type (VARCHAR) -- SECURITY, INFO, EMERGENCY
  ├── status (VARCHAR) -- LU, NON_LU
  ├── created_at (TIMESTAMP)
  └── version (INTEGER)
```

Le service métier centralisera les notifications :
```text
NotificationService
  ├── sendNotification(patientId, title, message, type)
  ├── getPatientNotifications(patientId)
  └── markAsRead(patientId, notificationId)
```

Le raccordement se fera par injection directe du service de notification dans `ExternalAccessService` et `PatientService`.

## 5. Fichiers impactés ou créés

| Fichier | Type d'impact | Rôle |
|---|---|---|
| `backend/src/main/resources/db/migration/V21__create_notifications_table.sql` | Nouveau | Script Flyway de création de la table |
| `com/joprelys/backend/notification/infrastructure/persistence/NotificationEntity.java` | Nouveau | Entité JPA avec concurrence optimiste |
| `com/joprelys/backend/notification/infrastructure/persistence/NotificationRepository.java` | Nouveau | Repository Spring Data JPA |
| `com/joprelys/backend/notification/application/NotificationService.java` | Nouveau | Service de gestion des notifications |
| `com/joprelys/backend/patient/api/PatientPortalController.java` | Modification | Ajout des endpoints de notifications |
| `com/joprelys/backend/patient/application/ExternalAccessService.java` | Modification | Appel de NotificationService à la création de demande |
| `com/joprelys/backend/patient/application/PatientService.java` | Modification | Appel de NotificationService lors de l'accès d'urgence |
| `com/joprelys/backend/patient/api/PatientPortalControllerTest.java` | Modification | Tests d'intégration d'API de notifications |

## 6. Endpoints REST

### `GET /api/patient/me/notifications`
- **Authentification** : PATIENT
- **Réponse** : Liste JSON de notifications.

### `POST /api/patient/me/notifications/{id}/read`
- **Authentification** : PATIENT
- **Réponse** : Notification mise à jour (statut `LU`).
