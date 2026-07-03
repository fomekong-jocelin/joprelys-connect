# STORY-1102 — Sécurisation IDOR Portail Patient
## Design Technique

---

## 1. Composants créés / modifiés

### 1.1 PatientAccessGuardService (nouveau)

**Fichier** : `backend/src/main/java/com/joprelys/backend/patient/application/PatientAccessGuardService.java`

Centralise les contrôles d'accès IDOR pour tous les endpoints du portail patient.

```
PatientAccessGuardService
├── resolve(Authentication) : PatientEntity
│     ├── 401 si authentication == null || getName() == null
│     └── 404 si globalPatientNumber inconnu en base
└── resolveAndGuard(Authentication, UUID requestedPatientId) : PatientEntity
      ├── appelle resolve()
      └── 403 si requestedPatientId != patient.getId()
```

**Dépendances injectées** :
- `PatientRepository` — pour `findByGlobalPatientNumber(String)`

---

### 1.2 PatientPortalController (modifié)

**Fichier** : `backend/src/main/java/com/joprelys/backend/patient/api/PatientPortalController.java`

**Avant** : chaque méthode répétait 6 lignes de null-check + repository.
**Après** : une seule ligne `patientAccessGuardService.resolve(authentication)`.

Méthodes refactorisées :
- `getMe()`
- `downloadOwnDocument()` — la vérification croisée `doc.visit.patient == patient` est **conservée**
- `getConsents()`
- `updateConsent()`
- `getAuditLogs()`

---

## 2. Architecture de sécurité

```
HTTP Request
    │
    ▼
JwtAuthFilter (filtre Spring Security)
    │  extrait principal = globalPatientNumber
    ▼
PatientPortalController (endpoint @PreAuthorize("hasRole('PATIENT')"))
    │
    ▼
PatientAccessGuardService.resolve(authentication)
    │  401 si non authentifié
    │  404 si patient inconnu
    ▼
PatientEntity (objet métier sécurisé)
    │
    ▼
[Vérification croisée optionnelle]
    │  downloadOwnDocument() : doc.visit.patient.id == patient.id
    │  403 si IDOR détecté
    ▼
Réponse métier (200)
```

---

## 3. Pattern SOLID appliqué

| Principe | Application |
|---|---|
| **Single Responsibility** | `PatientAccessGuardService` n'a qu'un rôle : sécurité d'accès IDOR |
| **Open/Closed** | Nouveaux endpoints peuvent appeler `resolve()` sans modifier le service |
| **Dependency Inversion** | Le controller dépend du service via injection, pas de la classe concrète |
| **DRY** | Le code null-check + repository n'est plus dupliqué (5 occurrences → 1) |

---

## 4. Tests d'intégration

**Fichier** : `backend/src/test/java/com/joprelys/backend/patient/PatientIdorSecurityTest.java`

| Cas de test | Scénario | HTTP attendu |
|---|---|---|
| `givenValidPatientToken_whenGetMe_thenReturns200` | Token A valide, GET /me | 200 |
| `givenNoToken_whenGetMe_thenReturns401` | Aucun token, GET /me | 401 |
| `givenPatientAToken_whenAccessingPatientBDocument_thenReturns403` | Token A, visitId de B | 403 |
| `givenValidPatientToken_whenAccessingUnknownVisitId_thenReturns404` | Token A, visitId inconnu | 404 |

**Pattern** : `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` — identique aux autres tests du projet.

---

## 5. Impact sur les tests existants

`PatientPortalControllerTest.java` contient déjà les cas :
- `givenPatientMe_whenAuthorized_thenReturnProfile` → 200 ✅
- `givenPatientMe_whenNoToken_thenUnauthorized` → 401 ✅
- `givenPatientOtherDocument_whenDownload_thenForbidden` → 403 ✅

Ces tests continuent de passer car le comportement observable est **identique**.
Seule l'implémentation interne a changé (délégation à `PatientAccessGuardService`).

---

## 6. Risques et mitigations

| Risque | Probabilité | Mitigation |
|---|---|---|
| Régression sur `PatientPortalControllerTest` | Faible | Comportement identique, même HTTP status codes |
| Oubli d'appeler `resolve()` dans un futur endpoint | Moyen | Revue de code obligatoire, service documenté |
| LazyInitializationException dans `downloadOwnDocument` | Faible | La query `findByVisitIdWithVisitAndPatient` fait déjà un JOIN FETCH |

---

## 7. Référence

- OWASP A01:2023 Broken Object Level Authorization
- Spring Security `Authentication` — `getName()` retourne le `subject` du JWT (= `globalPatientNumber`)
