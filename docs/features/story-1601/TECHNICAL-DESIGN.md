# TECHNICAL-DESIGN — Télétransmission d'ordonnances à AllôPharma - STORY-1601 & STORY-1602

## 1. Objectif technique

Implémenter l'infrastructure backend et frontend de télétransmission d'ordonnances : migration Flyway, DTOs, service d'intégration simulé `AlloPharmaClient`, contrôleurs REST sécurisés (avec isolation IDOR), bouton et badges Angular dans les vues patient/médecin, et les tests associés.

## 2. Stack concernée

- [x] Spring Boot
- [x] Base de données (Flyway migration SQL + H2/PostgreSQL)
- [x] Angular (Frontend)
- [x] Documentation

## 3. Contraintes projet obligatoires

- Backend Spring Boot : Maven uniquement (`pom.xml`, `mvnw`).
- Modèle de données : champs `transmission_status` (PENDING, TRANSMITTED, FAILED) et `transmitted_at` (TIMESTAMP) dans `prescriptions`.
- Thème centralisé light/dark et internationalisation FR/EN respectés côté Angular.
- Validation des tests unitaires backend et frontend.

## 4. Architecture cible

Un service client simulé gérera les appels sortants :
```text
AlloPharmaClient
  └── transmit(PrescriptionEntity)
        ├── Logs payload JSON transmis (médicaments, médecin, etc.)
        └── Retourne true/false (succès simulé)
```

Les contrôleurs REST exposent les endpoints d'action :
```text
POST /api/patient/me/prescriptions/{id}/transmit -> PatientPortalController
POST /api/prescriptions/{id}/transmit -> PrescriptionController (médecins)
```

## 5. Fichiers impactés ou à créer

| Fichier | Type d'impact | Rôle |
|---|---|---|
| `backend/src/main/resources/db/migration/V22__add_teletransmission_to_prescriptions.sql` | Nouveau | Migration Flyway (champs transmission) |
| `com/joprelys/backend/prescription/infrastructure/persistence/PrescriptionEntity.java` | Modification | Ajout des colonnes et accesseurs JPA |
| `com/joprelys/backend/prescription/application/AlloPharmaClient.java` | Nouveau | Client d'intégration simulé |
| `com/joprelys/backend/prescription/application/PrescriptionService.java` | Modification | Méthode de télétransmission, validation de statut, log d'audit |
| `com/joprelys/backend/prescription/api/PrescriptionController.java` | Modification | Endpoint médecin de transmission |
| `com/joprelys/backend/patient/api/PatientPortalController.java` | Modification | Endpoint patient de transmission |
| `com/joprelys/backend/patient/api/PatientPortalControllerTest.java` | Modification | Tests MockMvc de transmission patient |
| `com/joprelys/backend/prescription/api/PrescriptionControllerTest.java` | Modification | Tests MockMvc de transmission médecin |
| `web/src/app/patient/portal/services/patient-portal.service.ts` | Modification | Ajout de la méthode HTTP de télétransmission |
| `web/src/app/patient/portal/components/patient-visits-list.component.ts` | Modification | Ajout du bouton et badge de télétransmission |
| `web/src/app/core/i18n/i18n.service.ts` | Modification | Traductions associées (FR/EN) |
