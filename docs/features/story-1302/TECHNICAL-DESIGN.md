# TECHNICAL-DESIGN — Validation de demande d'accès externe - STORY-1302

## 1. Objectif technique

Ajouter les endpoints d'approbation et de rejet des demandes d'accès temporaires sur le portail patient, et implémenter l'IHM Angular correspondante en Tailwind CSS v4.

## 2. Stack concernée

- [x] Spring Boot
- [x] Angular
- [ ] Base de données
- [ ] CI/CD
- [x] Documentation

## 3. Contraintes projet obligatoires

- Backend Spring Boot : Maven uniquement (`pom.xml`, `mvnw`).
- Angular : Tailwind CSS v4 obligatoire, pas d'Angular Material.
- Angular : `proxy.conf.json` obligatoire et URLs API relatives.
- Angular : light/dark, i18n FR/EN, configuration app/branding.
- Documentation fonctionnelle et technique maintenue dès le démarrage.

## 4. Architecture cible

Le contrôleur de portail patient existant relaiera les appels vers le service d'accès externe :
```text
PatientPortalController (REST api/patient)
  → PatientAccessGuardService (Validation IDOR)
  → ExternalAccessService (Traitement métier et d'audit)
```

Côté Frontend, nous ajouterons un onglet "Demandes d'accès" dans l'espace portail patient.

## 5. Fichiers ou modules impactés

| Module | Fichier | Type d’impact |
|---|---|---|
| Backend | `com/joprelys/backend/patient/api/ExternalAccessResponse.java` | Mise à jour DTO (ajout du nom d'organisation) |
| Backend | `com/joprelys/backend/patient/application/ExternalAccessService.java` | Ajout des méthodes list, approve et reject |
| Backend | `com/joprelys/backend/patient/api/PatientPortalController.java` | Ajout des endpoints GET / POST |
| Backend | `com/joprelys/backend/patient/api/PatientPortalControllerTest.java` | Ajout des tests d'intégration patient |
| Frontend | `web/src/app/patient/patient-api.service.ts` | Ajout des méthodes HTTP de client API |
| Frontend | `web/src/app/core/i18n/i18n.service.ts` | Ajout des traductions FR/EN |
| Frontend | `web/src/app/patient/patient-portal.component.ts` | Redesign et ajout de l'onglet Demandes d'accès |
| Frontend | `web/src/app/patient/patient-portal.component.html` | Intégration de la liste des demandes dans le template HTML |

## 6. Contrats API

### `GET /api/patient/me/access-requests`
- **Authentification** : Requise (PATIENT)
- **Response** : `List<ExternalAccessResponse>`

### `POST /api/patient/me/access-requests/{id}/approve`
- **Authentification** : Requise (PATIENT)
- **Response** : `ExternalAccessResponse` (statut `APPROUVEE`, `expiresAt` mis à jour)

### `POST /api/patient/me/access-requests/{id}/reject`
- **Authentification** : Requise (PATIENT)
- **Response** : `ExternalAccessResponse` (statut `REFUSEE`)

## 7. Modèle de données / migrations

Aucune (table déjà existante).

## 8. Configuration

N/A.

## 9. Sécurité

- [x] Contrôle d'accès IDOR strict via `PatientAccessGuardService` pour s'assurer que le patient connecté ne manipule que ses propres demandes d'accès.
- [x] Vérification que la demande d'accès est bien à l'état `EN_ATTENTE` avant d'autoriser l'approbation ou le rejet.

## 10. Observabilité

- Logs d'audit générés : `APPROVE_EXTERNAL_ACCESS` (succès) et `REJECT_EXTERNAL_ACCESS` (succès).

## 11. Tests prévus

- Tests d'intégration backend dans `PatientPortalControllerTest.java`.
- Tests unitaires Vitest de composant Angular.

## 12. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Type de bump | MINOR |
| Justification | Ajout d'IHM et d'API de gestion d'accès pour le patient |
| Breaking change | Non |

## 13. Risques techniques

N/A.

## 14. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-04 | Antigravity | Création initiale du technical design |
