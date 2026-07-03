# TECHNICAL-DESIGN — Demande d'accès externe (backend) - STORY-1301

## 1. Objectif technique

Mettre en place la persistance et l'API REST backend pour soumettre des demandes d'accès temporaires inter-établissements aux DPU des patients.

## 2. Stack concernée

- [x] Spring Boot
- [ ] Angular
- [ ] Flutter
- [x] Base de données
- [ ] CI/CD
- [x] Documentation

## 3. Contraintes projet obligatoires

- Backend Spring Boot : Maven uniquement (`pom.xml`, `mvnw`).
- Backend Spring Boot : `application.yml` obligatoire, pas de `application.properties`.
- Documentation fonctionnelle et technique maintenue dès le démarrage.

## 4. Architecture cible

Le code suivra les architectures en couches existantes du backend :
```text
ExternalAccessController (API REST, validation DTO)
  → ExternalAccessService (Règles métier, coordination, audit)
  → ExternalAccessRequestRepository (Persistence JPA)
```

## 5. Fichiers ou modules impactés

| Module | Fichier | Type d’impact |
|---|---|---|
| Backend | `db/migration/V20__create_external_access_requests_table.sql` | Ajout migration Flyway |
| Backend | `com/joprelys/backend/patient/infrastructure/persistence/ExternalAccessRequestEntity.java` | Nouvelle entité JPA |
| Backend | `com/joprelys/backend/patient/infrastructure/persistence/ExternalAccessRequestRepository.java` | Nouveau repository JPA |
| Backend | `com/joprelys/backend/patient/api/CreateExternalAccessRequest.java` | Nouveau DTO de requête |
| Backend | `com/joprelys/backend/patient/api/ExternalAccessResponse.java` | Nouveau DTO de réponse |
| Backend | `com/joprelys/backend/patient/api/ExternalAccessController.java` | Nouveau contrôleur REST |
| Backend | `com/joprelys/backend/patient/application/ExternalAccessService.java` | Nouveau service métier |
| Backend | `com/joprelys/backend/audit/domain/AuditEventType.java` | Ajout du type d'événement `REQUEST_EXTERNAL_ACCESS` |
| Backend | `com/joprelys/backend/patient/api/ExternalAccessControllerTest.java` | Tests d'intégration MockMvc |

## 6. Contrats API

### `POST /api/external-access/requests`
- **Authentification** : Requise (Bearer JWT)
- **Rôles autorisés** : `MEDECIN`, `INFIRMIER`, `ADMIN_CLINIQUE`
- **Request Body** :
```json
{
  "patientId": "d4929486-8ec1-4f70-92db-bc848c34f993",
  "reason": "Consultation cardiologique spécialisée externe",
  "durationHours": 24
}
```
- **Response Body (201 Created)** :
```json
{
  "id": "7a256a8e-2fe1-4c1d-88ec-88abacde9901",
  "patientId": "d4929486-8ec1-4f70-92db-bc848c34f993",
  "requesterUserId": "a96d1a10-7f89-4d72-9eda-71df72a27f39",
  "requesterOrganizationId": "50a256d0-b2fe-4e01-b5ab-7e09e0837e31",
  "reason": "Consultation cardiologique spécialisée externe",
  "durationHours": 24,
  "status": "EN_ATTENTE",
  "createdAt": "2026-07-04T01:00:00Z"
}
```
- **Erreurs** :
  - `400 Bad Request` : Si `reason` est invalide (< 10 caractères) ou `durationHours` hors limites (1-168).
  - `404 Not Found` : Si le patient n'existe pas.
  - `409 Conflict` : Si une demande `EN_ATTENTE` ou `APPROUVEE` existe déjà.

## 7. Modèle de données / migrations

### Table `external_access_requests`
```sql
CREATE TABLE external_access_requests (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    requester_user_id UUID NOT NULL REFERENCES users(id),
    requester_organization_id UUID NOT NULL REFERENCES organizations(id),
    reason TEXT NOT NULL,
    requested_duration_hours INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_ext_access_patient ON external_access_requests(patient_id);
CREATE INDEX idx_ext_access_requester_org ON external_access_requests(requester_organization_id);
```

## 8. Configuration

N/A.

## 9. Sécurité

- [x] Authentification requise (JWT)
- [x] Autorisation / rôle requis (`MEDECIN`, `INFIRMIER`, `ADMIN_CLINIQUE`)
- [x] Inputs validés (`@NotBlank`, `@Size`, `@Min`, `@Max`)
- [x] Requêtes paramétrées (JPA Repositories)
- [x] Pas de secret dans le code
- [x] PII masquée dans logs

## 10. Observabilité

- Logs attendus : Audit logs avec `action = REQUEST_EXTERNAL_ACCESS`, IP address, et DPU concerné.

## 11. Tests prévus

| Niveau | Tests attendus | Commande |
|---|---|---|
| Integration | Création valide (201 Created), validation des limites (400), doublons (409), RBAC (403), DPU inexistant (404) | `./mvnw test -Dtest=ExternalAccessControllerTest` |

## 12. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Type de bump | MINOR |
| Justification | Ajout d'une nouvelle API et persistance de demande d'accès inter-établissements |
| Breaking change | Non |
| Migration requise | Oui (Flyway V20) |

## 13. Risques techniques

| Risque | Impact | Mitigation |
|---|---|---|
| Verrouillage concurrent de requêtes multiples | Moyen | Utilisation de verrous JPA optimistes et indexation unique. |

## 14. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-04 | Antigravity | Création initiale du technical design |

## Découpage SOLID et responsabilités

### Couche backend

| Élément | Responsabilité | Interface | Implémentation | Tests |
|---|---|---|---|---|
| Controller | Validation d'entrée et routes HTTP | N/A | `ExternalAccessController` | `ExternalAccessControllerTest` |
| Service | Exécution des vérifications de règles métier et journalisation | `ExternalAccessService` | `ExternalAccessServiceImpl` (si interface requise, ou classe concrète directe pour simplicité) | Unit/Integration |
| Domain / Infra | Modélisation et accès aux données | `ExternalAccessRequestRepository` | JPA Hibernate | N/A |
