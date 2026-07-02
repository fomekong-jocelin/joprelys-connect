# TECHNICAL-DESIGN — Traçabilité & Audit Logs

## 1. Objectif technique

Concevoir l'architecture backend pour l'enregistrement robuste et la consultation sécurisée des logs d'audit. L'implémentation doit garantir l'immuabilité (pas d'opérations de modification ou de suppression) et respecter l'isolation multi-tenant (un administrateur ne voit que les logs de sa propre organisation).

## 2. Stack concernée

- [x] Spring Boot
- [ ] Angular (STORY-0702)
- [ ] Flutter
- [x] Base de données (H2 / PostgreSQL)
- [ ] CI/CD
- [x] Documentation

## 3. Contraintes projet obligatoires

- Backend Spring Boot : Maven uniquement (`pom.xml`, `mvnw`).
- Backend Spring Boot : `application.yml` obligatoire, pas de `application.properties`.
- Découpage SOLID : Séparation claire entre contrôleurs, services d'application et persistance.
- Immuabilité : Endpoints en lecture seule, aucune méthode de mise à jour.

## 4. Architecture cible

Nous créons un module d'audit isolé sous le package `com.joprelys.backend.audit` :

```text
AuditController (api)
  → AuditService (application)
  → AuditLogRepository (infrastructure.persistence)
  → AuditLogEntity (infrastructure.persistence)
```

Pour la capture automatique des événements de connexion et de déconnexion, nous implémentons un écouteur d'événements Spring Security (`AuditSecurityEventListener`).
Pour les actions cliniques, nous injectons `AuditService` dans les services métier concernés (ou nous émettons des événements applicatifs Spring).

## 5. Fichiers ou modules impactés

| Module | Fichier | Type d’impact |
|---|---|---|
| DB | `V10__create_audit_logs_table.sql` | Création (Migration Flyway) |
| Audit | `AuditLogEntity.java` | Création (Entité JPA) |
| Audit | `AuditLogRepository.java` | Création (Repository Spring Data JPA) |
| Audit | `AuditService.java` | Création (Interface de service) |
| Audit | `AuditServiceImpl.java` | Création (Implémentation du service) |
| Audit | `AuditController.java` | Création (Endpoints de lecture) |
| Audit | `AuditLogResponse.java` | Création (DTO d'exposition des logs) |
| Security | `AuditSecurityEventListener.java` | Création (Écouteur d'événements d'authentification) |
| Visit | `DocumentService.java` | Modification (Journalisation du téléchargement et de la révocation) |
| Patient | `PatientService.java` | Modification (Journalisation de la création / modification) |

## 6. Contrats API

### 1. Consulter les logs d'un patient
- **Méthode** : `GET`
- **Endpoint** : `/api/audit/patients/{patientId}`
- **Rôles autorisés** : `AUDITEUR`, `ADMIN_CLINIQUE`, `MEDECIN`
- **Response** : `List<AuditLogResponse>`
- **Erreurs** :
  - 401 Unauthorized : Non authentifié.
  - 403 Forbidden : Accès à un patient d'un autre établissement.

### 2. Consulter les logs d'une organisation
- **Méthode** : `GET`
- **Endpoint** : `/api/audit/organizations/{organizationId}`
- **Rôles autorisés** : `AUDITEUR`, `ADMIN_CLINIQUE`
- **Response** : `List<AuditLogResponse>`
- **Erreurs** :
  - 401 Unauthorized : Non authentifié.
  - 403 Forbidden : Accès à une organisation différente du tenant de l'utilisateur (sauf si rôle `AUDITEUR`).

```json
// Exemple de réponse AuditLogResponse
[
  {
    "id": "e229c15d-85fa-4411-bdcf-8874bb55c4d2",
    "actorUserId": "3c02506b-ecbe-4cfc-8ff1-789a4497e2b1",
    "actorName": "Dr. Alpha",
    "actorOrganizationId": "2c02506b-ecbe-4cfc-8ff1-789a4497e2b0",
    "patientId": "1c02506b-ecbe-4cfc-8ff1-789a4497e2a9",
    "patientName": "Jean Dupont",
    "resourceType": "PATIENT_RECORD",
    "resourceId": "1c02506b-ecbe-4cfc-8ff1-789a4497e2a9",
    "action": "CONSULTATION",
    "reason": "Accès au dossier médical complet",
    "ipAddress": "192.168.1.50",
    "userAgent": "Mozilla/5.0...",
    "status": "SUCCESS",
    "createdAt": "2026-07-02T12:00:00Z"
  }
]
```

## 7. Modèle de données / migrations

### Table `audit_logs`

| Colonne | Type | Nullable | Index / Contrainte |
|---|---|---|---|
| `id` | UUID | NON | PRIMARY KEY |
| `actor_user_id` | UUID | OUI | Index |
| `actor_organization_id` | UUID | OUI | Index |
| `patient_id` | UUID | OUI | Index |
| `resource_type` | VARCHAR(80) | OUI | |
| `resource_id` | UUID | OUI | |
| `action` | VARCHAR(80) | NON | |
| `reason` | TEXT | OUI | |
| `ip_address` | VARCHAR(80) | OUI | |
| `user_agent` | TEXT | OUI | |
| `status` | VARCHAR(30) | NON | SUCCESS, DENIED |
| `created_at` | TIMESTAMP WITH TIME ZONE | NON | |

**Index requis** :
- `idx_audit_logs_patient_id` sur `patient_id`
- `idx_audit_logs_organization_id` sur `actor_organization_id`

## 8. Configuration

Aucun paramètre spécifique dans `application.yml` pour le MVP, sauf si nous souhaitons activer/désactiver les logs d'audit.

## 9. Sécurité

- [x] Authentification requise.
- [x] Autorisation par rôles (`AUDITEUR`, `ADMIN_CLINIQUE`, `MEDECIN`) avec contrôle multi-tenant (l'organisation de l'acteur doit correspondre à `actor_organization_id` de l'enregistrement ou à l'organisation du patient).
- [x] Requêtes paramétrées Spring Data JPA.
- [x] Pas de stack trace de base de données retournée en cas d'erreur.

## 10. Observabilité

- Les logs d'audit sont sauvegardés en base pour garantir la conformité réglementaire.
- Un logger standard Spring Boot (`LoggerFactory.getLogger(AuditService.class)`) consignera également en console/fichier de logs les écritures d'audit pour des analyses rapides.

## 11. Tests prévus

| Niveau | Tests attendus | Commande |
|---|---|---|
| Unit | Validation du mapper de DTO et de la logique de filtrage du service. | `mvn test` |
| Integration | Test du contrôleur d'audit `AuditControllerTest` : vérification du multi-tenant (accès interdit d'une clinique B aux logs d'une clinique A), anonymes (401), et filtres. | `mvn test` |

## 12. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Type de bump | MINOR |
| Justification | Ajout d'un nouveau module d'audit avec endpoints REST de consultation et modèle de données additionnel, entièrement rétrocompatible. |
| Breaking change | Non |
| Migration requise | Oui (Flyway V10) |

## 13. Risques techniques

| Risque | Impact | Mitigation |
|---|---|---|
| Surcharge de la base de données par l'écriture des logs | Moyen | Indexation fine. Par la suite, possibilité de passer en écriture asynchrone via Spring events (`@Async`) ou message broker. |

## 14. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-02 | Antigravity | Création initiale |
