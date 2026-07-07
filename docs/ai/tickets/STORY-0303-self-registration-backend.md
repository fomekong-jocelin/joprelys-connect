# STORY-0303 — API Backend pour l'enregistrement temporaire et la validation (Self-Registration)

## 1. Objectif

Implémenter le socle backend pour l'enregistrement de patient en autonomie. Cela inclut le modèle de base de données temporaire, les endpoints de soumission publics (avec captcha médical et rate limiting), et les endpoints de validation / réconciliation sécurisés pour le back-office d'accueil.

## 2. Critères d'acceptation

- [x] **DB Migration** : Script Flyway SQL pour créer la table `patient_pre_registrations`.
- [x] **Entities & Repositories** : Création de `PatientPreRegistrationEntity`, mapping JPA, et `PatientPreRegistrationRepository`.
- [x] **Captcha Médical Service** :
  - Service backend capable de générer aléatoirement une question médicale simple (ex: *"Quelle est la température corporelle normale ? (Réponse attendue: 37)"*).
  - Fournir un identifiant unique de captcha et stocker la réponse de manière chiffrée ou temporaire dans une session légère ou cache.
- [x] **Endpoint Public (Soumission)** :
  - `POST /api/public/pre-registrations` : Valider les données du patient, vérifier le captcha médical, et persister à l'état `AWAITING_VALIDATION`.
  - Rate limiting sur cet endpoint (ex: maximum 5 requêtes par minute par adresse IP).
- [x] **Endpoints Back-office (Sécurisés)** :
  - `GET /api/pre-registrations` (filtrable par statut, paginé).
  - `GET /api/pre-registrations/{id}`.
  - `POST /api/pre-registrations/{id}/validate` : convertit en patient officiel (ou fusionne) et génère la fiche d'admission PDF.
  - `POST /api/pre-registrations/{id}/reject`.
- [x] **Purge automatique** : Job planifié `@Scheduled` supprimant les demandes `AWAITING_VALIDATION` datant de plus de 24 heures.
- [x] **Tests** : Tests unitaires et d'intégration MockMvc avec couverture à plus de 80%.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0003 — Dossier Patient Unique (DPU) & Recherche |
| Sprint cible | SPRINT-0011 / SPRINT-0012 |
| Priorité business | P1 |
| Complexité | M |
| Story points | 5 |
| Profil recommandé | Backend Engineer (Senior) |
| Effort estimé senior | 2.0j |
| Effort estimé intermédiaire | 3.0j |
| Effort estimé junior | 5.0j |
| Responsable | Antigravity |
| Reviewer obligatoire | Tech Lead |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Definition of Ready

- ADR-0003 validé et accepté.
- Conception technique validée.

## 5. Definition of Done

- Code livré et build Maven passant sans erreur (`./mvnw clean verify`).
- Tests unitaires et d'intégration validés.
- Documentation technique et API mise à jour.

## 6. Action plan

- [x] Créer le script de migration SQL Flyway `V42__create_patient_pre_registrations.sql`.
- [x] Créer `PatientPreRegistrationEntity.java` et son enum `PreRegistrationStatus`.
- [x] Créer `MedicalCaptchaService.java` pour la génération et validation de questions médicales simples.
- [x] Créer les DTOs associés (`PatientPreRegistrationRequest`, `PreRegistrationValidationRequest`, `MedicalCaptchaResponse`).
- [x] Implémenter `PatientPreRegistrationService` contenant la logique de soumission, validation (avec liaison `PatientSimilarityService`), rejet, et le job `@Scheduled` de nettoyage.
- [x] Créer `PatientPreRegistrationController` avec l'endpoint public et les endpoints sécurisés.
- [x] Ajouter le rate limiting sur le point d'entrée public.
- [x] Écrire les tests unitaires et les tests MockMvc dans `PatientPreRegistrationControllerTest.java`.

## 7. Statut final

Statut : DONE
