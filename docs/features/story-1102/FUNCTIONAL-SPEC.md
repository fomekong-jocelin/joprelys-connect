# STORY-1102 — Sécurisation IDOR Portail Patient (OWASP)
## Spécification Fonctionnelle

---

## 1. Contexte métier

Le portail patient expose des endpoints REST permettant à un patient de consulter son dossier
médical, télécharger ses documents, gérer ses consentements et visualiser ses logs d'audit.

Chaque patient accède à la plateforme via un token JWT obtenu après authentification OTP.
Le **Dossier Patient Unique (DPU)** est identifié par un `globalPatientNumber` encodé dans
le token, et par un `UUID` interne en base de données.

---

## 2. Problème identifié (OWASP A01:2023)

### Vulnérabilité IDOR (*Insecure Direct Object Reference*)

Avant cette story, chaque endpoint du portail patient répétait indépendamment :
1. Le contrôle d'authentification du token (`null-check`).
2. La résolution du patient via `findByGlobalPatientNumber`.

Cette duplication présentait des risques :
- **Incohérence** : si un check était oublié dans un nouveau endpoint, le patient pouvait accéder
  à des données qui ne lui appartiennent pas.
- **Absence de vérification croisée systématique** : l'identifiant extrait du JWT n'était pas
  comparé à l'entité accédée (ex : un `visitId` pouvant appartenir à un autre patient).

---

## 3. Solution fonctionnelle

### Principe général

> Tout endpoint du portail patient doit vérifier que la ressource demandée appartient bien au
> patient dont l'identité est certifiée par le JWT.

### Comportements attendus

| Scénario | Résultat attendu |
|---|---|
| Token valide, ressource appartenant au patient authentifié | **200 OK** |
| Pas de token | **401 Unauthorized** |
| Token valide, patient inconnu en base | **404 Not Found** |
| Token valide, ressource appartenant à un autre patient | **403 Forbidden** |
| Token valide, ressource inexistante | **404 Not Found** |

---

## 4. Périmètre des endpoints protégés

| Endpoint | Méthode | Protection appliquée |
|---|---|---|
| `/api/patient/me` | GET | Résolution JWT → patient |
| `/api/patient/visits/{visitId}/document` | GET | Résolution JWT → patient + vérification croisée `visit.patient == patient` |
| `/api/patient/consents` | GET | Résolution JWT → patient |
| `/api/patient/consents/{orgId}` | POST | Résolution JWT → patient |
| `/api/patient/audit-logs` | GET | Résolution JWT → patient |

---

## 5. Critères d'acceptation

- [x] Un patient authentifié accède à son propre profil → HTTP 200.
- [x] Un accès sans token est rejeté → HTTP 401.
- [x] Un patient tente d'accéder au document d'un autre patient → HTTP 403.
- [x] Un visitId inexistant avec token valide → HTTP 404.
- [x] Les contrôles sont centralisés dans `PatientAccessGuardService` (DRY).
- [x] Des tests d'intégration couvrent les 4 scénarios ci-dessus.

---

## 6. Référence OWASP

- **OWASP API Security Top 10 : A01:2023 — Broken Object Level Authorization**
- Documentation : https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/
