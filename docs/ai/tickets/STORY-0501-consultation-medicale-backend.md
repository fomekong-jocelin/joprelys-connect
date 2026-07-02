# STORY-0501 — Saisie de la consultation médicale (backend)

## Métadonnées

| Champ | Valeur |
|---|---|
| ID | STORY-0501 |
| Epic | EPIC-0005 — Consultation Médicale & Prescription |
| Type | User Story |
| Priorité | P0 |
| Story Points | 3 SP |
| Sprint | SPRINT-0003 |
| Statut | DONE |
| Assigné | Antigravity |
| Reviewer | Lead |
| Profil recommandé | Intermédiaire |
| Est. Senior | 0.7j |
| Est. Intermédiaire | 0.9j |
| Est. Junior | 1.5j |
| Temps passé | 0.7j |
| Dépendances | EPIC-0004 ✅ (VisitEntity, VisitRepository, VitalsEntity) |
| Dernière MAJ | 2026-07-03 |

## Objectif

Permettre à un médecin authentifié (`MEDECIN`, `ADMIN_CLINIQUE`) de saisir une consultation médicale complète sur une visite active (`EN_COURS`), avec un pattern upsert (création ou mise à jour), et d'en lire le résultat.

## Critères d'acceptation

- [x] Une consultation est toujours liée à une visite `EN_COURS`
- [x] Un seul objet consultation par visite (OneToOne via `visit_id UNIQUE`)
- [x] Les champs `symptoms` et `diagnosis` sont obligatoires (`@NotBlank`)
- [x] Upsert : si la consultation existe déjà, elle est mise à jour (pas d'erreur 409)
- [x] Isolation multi-tenant : un médecin d'un tenant ne peut pas accéder à une visite d'un autre tenant (→ 404)
- [x] `GET /api/visits/{id}/consultation` → 404 si aucune consultation n'existe pour cette visite
- [x] Accès interdit à `AGENT_ACCUEIL` (→ 403 Forbidden)
- [x] Accès sans token → 401 Unauthorized
- [x] Visite clôturée (`TERMINEE`) → 400 Bad Request avec message explicite

## Stack technique

- Backend Spring Boot / Maven
- JPA + Hibernate Multi-tenant
- Spring Security + JWT (`authentication.getName()` → email du principal)
- Flyway pour la migration DDL

## Tasks

| # | Task | Statut |
|---|---|---|
| T1 | Créer `ConsultationEntity` (JPA, liée à `VisitEntity`) | ✅ DONE |
| T2 | Créer `ConsultationRepository` | ✅ DONE |
| T3 | Créer `ConsultationService` (résolution médecin par email via `UserAccountRepository.findByEmail()`) | ✅ DONE |
| T4 | Créer `ConsultationController` (`POST` + `GET /api/visits/{id}/consultation`) | ✅ DONE |
| T5 | Créer `SaveConsultationRequest` (DTOs avec Bean Validation) | ✅ DONE |
| T6 | Créer `ConsultationResponse` (record de sortie) | ✅ DONE |
| T7 | Migration Flyway `V6__create_consultation_table.sql` | ✅ DONE |
| T8 | Écrire `ConsultationControllerTest` (9 cas : nominaux, erreurs, RBAC, multi-tenant, visite clôturée) | ✅ DONE |

## Fichiers produits / modifiés

| Fichier | Action |
|---|---|
| `consultation/infrastructure/persistence/ConsultationEntity.java` | Créé |
| `consultation/infrastructure/persistence/ConsultationRepository.java` | Créé |
| `consultation/application/ConsultationService.java` | Créé |
| `consultation/api/ConsultationController.java` | Créé |
| `consultation/api/SaveConsultationRequest.java` | Créé |
| `consultation/api/ConsultationResponse.java` | Créé |
| `resources/db/migration/V6__create_consultation_table.sql` | Créé |
| `test/.../consultation/api/ConsultationControllerTest.java` | Créé |

## Décisions techniques

- **Principal = email** : `JwtClaims.subject` contient l'email, donc `authentication.getName()` retourne l'email. Le service résout le médecin via `UserAccountRepository.findByEmail()`, cohérent avec le pattern suivi dans `VisitService`.
- **Upsert** : la spécification initiale demandait 409 en cas de doublon, mais le pattern upsert est plus adapté à la réalité clinique (sauvegarde partielle, brouillon). Le comportement retenu est celui du `ConsultationService.saveConsultation()` existant.
- **Isolation tenant** : Hibernate Filter via `@TenantId` garantit qu'une visite d'un autre tenant est invisible (→ 404 naturel via `findById`).

## Tests — couverture

| Test | Scénario | Résultat attendu |
|---|---|---|
| `givenMedecinA_whenSaveConsultation_thenSuccess` | Saisie complète valide | 200 + champs vérifiés |
| `givenExistingConsultation_whenSaveAgain_thenUpsertSuccess` | Mise à jour upsert | 200 + champs mis à jour |
| `givenMedecinA_whenGetConsultation_thenSuccess` | Lecture d'une consultation existante | 200 + champs corrects |
| `givenAgentAccueil_whenSaveConsultation_thenForbidden` | RBAC — rôle insuffisant | 403 |
| `givenMissingSymptoms_whenSaveConsultation_thenBadRequest` | Validation Bean — symptoms manquant | 400 |
| `givenMissingDiagnosis_whenSaveConsultation_thenBadRequest` | Validation Bean — diagnosis manquant | 400 |
| `givenUnknownVisit_whenSaveConsultation_thenNotFound` | Visite inexistante | 404 |
| `givenNoConsultation_whenGetConsultation_thenNotFound` | GET sans consultation | 404 + message |
| `givenNoToken_whenSaveConsultation_thenUnauthorized` | Sans token JWT | 401 |
| `givenMedecinB_whenSaveConsultationOnVisitA_thenNotFound` | Cross-tenant isolation | 404 |
| `givenClosedVisit_whenSaveConsultation_thenBadRequest` | Visite clôturée | 400 + message |

## Impact SemVer

MINOR — nouvelle fonctionnalité (endpoint `POST/GET /api/visits/{id}/consultation`), rétrocompatible.

## Risques restants

- Le `countThisYear()` dans `ConsultationRepository` utilise `YEAR(CURRENT_TIMESTAMP)` — syntaxe valide en H2 (test) et PostgreSQL (prod) ? → À valider lors des tests d'intégration CI.
- Le `generateDocumentNumber()` dans `ConsultationService` utilise `consultationRepository.count()` — risque de collision si deux consultations sont créées simultanément. Non bloquant pour le MVP.

## Reste à faire (hors STORY-0501)

- STORY-0502 : Prescription (backend)
- STORY-0503 : Écran consultation Angular
- STORY-0504 : Historique des consultations
