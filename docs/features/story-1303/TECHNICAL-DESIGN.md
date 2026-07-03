# TECHNICAL-DESIGN — Contrôle d'accès & Expiration des droits externes - STORY-1303

## 1. Objectif technique

Sécuriser le chargement du DPU par un praticien d'un établissement externe en vérifiant l'existence d'une demande d'accès externe active, approuvée et non expirée. Mettre en place la tâche de planification d'expiration asynchrone et adapter le type de log de sécurité d'urgence à `EMERGENCY_DPU_ACCESS` avec son rendu IHM en rouge critique.

## 2. Stack concernée

- [x] Spring Boot (Backend)
- [x] Angular (Frontend)
- [ ] Base de données
- [ ] CI/CD
- [x] Documentation

## 3. Contraintes projet obligatoires

- Backend Spring Boot : Maven uniquement (`pom.xml`, `mvnw`).
- Base de données : Java Scheduler `@Scheduled` pour la vérification automatique des expirations.
- IHM : Rendu en rose/rouge pour l'action d'urgence critique `EMERGENCY_DPU_ACCESS` dans la timeline d'audit du patient.

## 4. Architecture cible

Le contrôle d'accès dans `PatientService.getPatientById` sera étendu :
```text
PatientService.getPatientById(patientId)
  ├── checkConsent(patientId, organizationId)
  │     ├── [OK] Même clinique ?
  │     ├── [OK] Consentement local "ACTIVE" existant ?
  │     └── [NEW] Demande d'accès externe "APPROUVEE" et non expirée ?
  └── Si non autorisé :
        └── checkEmergencyAccess(patientId, organizationId)
              ├── [OK] Autorisation d'urgence active ?
              └── [Forbidden] Sinon lever ResponseStatusException(HttpStatus.FORBIDDEN, "CONSENT_REQUIRED")
```

Un scheduler d'expiration s'exécutera périodiquement en arrière-plan :
```text
ExternalAccessExpirationScheduler
  └── @Scheduled(fixedDelay = 60000)
        └── ExternalAccessRequestRepository.expireRequests(Instant.now())
              └── UPDATE external_access_requests SET status = 'EXPIREE' WHERE status = 'APPROUVEE' AND expires_at <= :now
```

## 5. Fichiers impactés

| Fichier | Type d'impact | Rôle |
|---|---|---|
| `com/joprelys/backend/JoprelysBackendApplication.java` | Modification | Ajout de `@EnableScheduling` |
| `com/joprelys/backend/patient/infrastructure/persistence/ExternalAccessRequestRepository.java` | Modification | Ajout de la requête d'expiration SQL native |
| `com/joprelys/backend/patient/application/ExternalAccessExpirationScheduler.java` | Nouveau | Planificateur d'expiration automatique |
| `com/joprelys/backend/patient/application/PatientService.java` | Modification | Injection d'ExternalAccessRequestRepository, extension de `checkConsent`, mise à jour du log d'accès d'urgence vers `EMERGENCY_DPU_ACCESS` |
| `web/src/app/core/i18n/i18n.service.ts` | Modification | Traduction de `EMERGENCY_DPU_ACCESS` (FR/EN) |
| `web/src/app/patient/portal/components/patient-audit-list.component.ts` | Modification | Formatage et style du badge pour `EMERGENCY_DPU_ACCESS` |
| `com/joprelys/backend/patient/application/PatientServiceTest.java` | Nouveau/Modification | Tests unitaires de la sécurité d'accès et du scheduler |

## 6. Détails de l'implémentation

### Requête de mise à jour en lot dans `ExternalAccessRequestRepository.java`
```java
@Modifying
@Query("UPDATE ExternalAccessRequestEntity r SET r.status = 'EXPIREE' WHERE r.status = 'APPROUVEE' AND r.expiresAt <= :now")
int expireRequests(@Param("now") java.time.Instant now);
```

### Rendu IHM Timeline d'Audit (Frontend)
Toute action `EMERGENCY_DPU_ACCESS` est classée en critique rouge (`bg-rose-100 text-rose-800`).
