# TECHNICAL-DESIGN — Patient Portal (EPIC-0008)

## 1. Objectif technique

Implémenter l'authentification et l'espace sécurisé pour les patients. L'authentification reposera sur la vérification du couple **N° DPU** + **Téléphone** + **Date de naissance** (données existantes en base), suivie de la validation d'un OTP à 6 chiffres à validité courte (5 minutes) simulé en console de développement. En cas de succès, un token JWT avec le rôle `PATIENT` est généré.

## 2. Stack concernée

- [x] Spring Boot (REST API, Spring Security, JWT)
- [x] Angular (Composants, Service API, Guards, Routing)
- [x] Base de données (Aucune migration requise, utilisation des tables existantes)
- [x] Documentation (Functional Spec, Technical Design)

## 3. Contraintes projet obligatoires

- Backend Spring Boot : Maven uniquement (`pom.xml`, `mvnw`).
- Backend Spring Boot : configuration via `application.yml` uniquement.
- Angular : Tailwind CSS v4 CSS-first uniquement. Pas d'Angular Material.
- Angular : proxy de développement obligatoire (`proxy.conf.json`) et requêtes HTTP relatives.
- Thèmes Light/Dark et internationalisation FR/EN respectés.

## 4. Architecture cible

Le portail patient introduira de nouveaux services et contrôleurs isolés :

```text
PatientController (API publique / sécurisée)
  → PatientAuthService (Génération OTP, Validation, Génération JWT)
  → PatientService (Lecture des données DPU & Historique)
  → MedicalDocumentRepository (Téléchargement de ses propres ordonnances)
```

## 5. Fichiers ou modules impactés

| Module | Fichier | Type d’impact |
|---|---|---|
| Backend | `com.joprelys.backend.auth.security.JwtService` | Ajout du support de création de token pour un patient (sans UserAccountEntity) |
| Backend | `com.joprelys.backend.patient.api.PatientAuthController` | Création (Endpoints login/OTP/verify) |
| Backend | `com.joprelys.backend.patient.application.PatientAuthService` | Création (Logique OTP et génération Token) |
| Backend | `com.joprelys.backend.patient.api.PatientPortalController` | Création (Détails DPU et liste de consultations) |
| Frontend | `src/app/app.routes.ts` | Ajout des routes `/patient/login` et `/patient/dashboard` |
| Frontend | `src/app/patient/patient-portal.service.ts` | Création (Service d'appels API) |
| Frontend | `src/app/patient/patient-login.component.ts` | Création (Écran de connexion et OTP) |
| Frontend | `src/app/patient/patient-dashboard.component.ts` | Création (Affichage DPU et liste ordonnances) |

## 6. Contrats API

### 1. Demande d'OTP de connexion
- **Méthode** : `POST`
- **Endpoint** : `/api/public/patient/auth/otp`
- **Request Body** :
  ```json
  {
    "globalPatientNumber": "PAT-20260702-000001",
    "phone": "+237 699 99 99 99",
    "birthDate": "1990-01-01"
  }
  ```
- **Response** : `200 OK` (OTP envoyé/simulé en log)
- **Erreurs** : `404 NOT FOUND` (Informations incorrectes)

### 2. Validation d'OTP et obtention du Token JWT
- **Méthode** : `POST`
- **Endpoint** : `/api/public/patient/auth/verify`
- **Request Body** :
  ```json
  {
    "globalPatientNumber": "PAT-20260702-000001",
    "otpCode": "123456"
  }
  ```
- **Response** : `200 OK`
  ```json
  {
    "token": "eyJhbG...",
    "tokenType": "Bearer",
    "expiresAt": 1782398400,
    "globalPatientNumber": "PAT-20260702-000001",
    "fullName": "Jean Dupont",
    "role": "PATIENT"
  }
  ```
- **Erreurs** : `400 BAD REQUEST` (Code OTP expiré ou incorrect)

### 3. Consultation de ses propres données
- **Méthode** : `GET`
- **Endpoint** : `/api/patient/me`
- **Headers** : `Authorization: Bearer <token>`
- **Response** : `200 OK` (PatientResponse + Liste des consultations associées)

## 7. Modèle de données / migrations
Création de la migration Flyway `V11__create_patient_consents_and_emergency_access.sql` définissant :
- `patient_consents` : Table stockant les consentements explicites (`ACTIVE`, `REVOKED`) liant un patient à une organisation.
- `emergency_access_authorizations` : Table stockant les dérogations d'accès d'urgence "Brise-Glace" d'une durée de validité de 15 minutes.

## 7.1 APIs de Consentement et d'Urgence

### 1. Obtenir les consentements d'accès du patient
- **Méthode** : `GET`
- **Endpoint** : `/api/patient/consents`
- **Headers** : `Authorization: Bearer <token>`
- **Response** : `200 OK` (Liste d'objets `PatientConsentDto` avec `organizationId`, `organizationName`, `status` (`ACTIVE`/`REVOKED`/`NONE`) et `isCreator`)

### 2. Mettre à jour le consentement pour une clinique
- **Méthode** : `POST`
- **Endpoint** : `/api/patient/consents/{orgId}?status=ACTIVE` (ou `REVOKED`)
- **Headers** : `Authorization: Bearer <token>`
- **Response** : `200 OK`

### 3. Déclencher un accès d'urgence "Brise-Glace" (Praticien)
- **Méthode** : `POST`
- **Endpoint** : `/api/patients/{patientId}/emergency-access`
- **Headers** : `Authorization: Bearer <token>` (Rôle praticien requis)
- **Request Body** :
  ```json
  {
    "reason": "Justification de l'accès d'urgence"
  }
  ```
- **Response** : `201 CREATED`

---

## 8. Configuration
Aucune variable de configuration externe requise. La validité de l'OTP reste à 5 minutes et la dérogation d'urgence Brise-Glace est fixée à 15 minutes.

---

## 9. Sécurité

- Les endpoints `/api/public/patient/auth/**` sont publics.
- Les endpoints `/api/patient/**` exigent un token JWT valide avec le rôle `ROLE_PATIENT`.
- L'isolation est assurée en comparant le `globalPatientNumber` présent dans le Token JWT avec la ressource consultée.
- **Blocage d'accès (STORY-0803)** : Lors de l'appel à `PatientService.getPatientById` par un acteur clinique (`MEDECIN`, `INFIRMIER`, etc.), le backend vérifie l'existence d'un consentement `ACTIVE` ou d'une dérogation d'accès d'urgence active pour son organisation. Sinon, il lève une exception `403 FORBIDDEN` avec le message `CONSENT_REQUIRED`.
- **Accès global cross-tenant** : Utilisation d'une requête native SQL `findByIdGlobally` pour charger la fiche patient et bypasser le filtre Hibernate multi-tenant lors des contrôles de consentement globaux.

## 10. Observabilité
- Logs d'audit générés à chaque connexion réussie d'un patient.
- OTP imprimé en console standard `System.out` ou `Logger` au format :
  `[OTP PATIENT] Code de sécurité pour ${globalPatientNumber} : ${otpCode}`

## 11. Tests prévus

- **Backend** :
  - `PatientAuthControllerTest` : tests d'intégration pour login OTP, validation OTP, échec d'OTP expiré, et accès sécurisés `/api/patient/me`.
- **Frontend** :
  - Tests unitaires pour les formulaires Angular de connexion et d'OTP.

## 12. Impact version / SemVer

- **Type de bump** : `MINOR`
- **Justification** : Ajout du portail patient et de son mécanisme d'authentification.
- **Breaking change** : Non.

## 13. Risques techniques

| Risque | Impact | Mitigation |
|---|---|---|
| Fuite ou interception de l'OTP | Moyen | En production, l'OTP doit être envoyé par SMS/Email crypté. En local, il est uniquement écrit dans les logs serveurs sécurisés. |
| Bruteforce de l'OTP à 6 chiffres | Moyen | Limiter à 3 tentatives de validation par OTP généré avant invalidation complète de la session. |

## 14. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-02 | Antigravity | Création initiale de la spécification technique |
