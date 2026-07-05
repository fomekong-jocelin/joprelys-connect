# TICKET-0110 — Conformité Patient et DPU (Vaccinations, Groupe Sanguin, Email, Doublons)

> Fichier obligatoire pour l'intervention de mise en conformité réglementaire et fonctionnelle du dossier patient unique (DPU).

## 1. Objectif

Résoudre l'intégralité des écarts identifiés dans l'audit de conformité par rapport au Cahier des Charges complet de Joprelys Connect :
1. **Groupe Sanguin** : Ajouter la donnée `blood_group` en base, entités, DTOs, et IHM.
2. **Email Patient** : Ajouter la donnée `email` en base, entités, DTOs, et IHM.
3. **Vaccinations** : Implémenter le sous-module de vaccinations (table de base de données, entité JPA, endpoints REST, services, IHM d'affichage et d'ajout).
4. **Lien Doublons** : Ajouter l'accès direct à l'administration des doublons dans le menu Sidebar pour l'ADMIN_CLINIQUE.

## 2. Critères d'acceptation

- [x] La table `patients` contient les colonnes `blood_group` et `email` via Flyway migration.
- [x] L'entité `PatientEntity` expose les nouveaux champs avec getters/setters et les map dans le service.
- [x] Les mappers FHIR projettent l'adresse email dans la ressource FHIR `Patient.telecom`.
- [x] Le formulaire de création de patient permet de renseigner le groupe sanguin (sélectionneur O+/A+/etc.) et l'email.
- [x] L'onglet *Informations Administratives* du dossier patient et le tableau de bord patient affichent le groupe sanguin et l'email s'ils sont renseignés.
- [x] Le PDF de synthèse médicale généré affiche le groupe sanguin et l'email.
- [x] La table `patient_vaccinations` est créée et isolée par multi-tenant.
- [x] L'API REST expose `GET`, `POST` et `PUT` sur `/api/patients/{patientId}/vaccinations`.
- [x] L'IHM du profil patient affiche la section *Vaccinations* avec la liste des doses administrées (nom, lot, date, administrateur, notes, rappel) et permet l'ajout interactif d'une vaccination.
- [x] Le lien *Gestion des Doublons* est visible dans la Sidebar pour le rôle d'administration clinique.
- [x] L'intégralité du build (compilations backend & frontend) et des tests passe sans erreur.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0012 |
| User story parent | STORY-1801 |
| Sprint cible | SPRINT-0009 |
| Priorité business | P1 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Senior Fullstack |
| Effort estimé senior | 0.3j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |

## 4. Action plan

- [x] Rédiger la migration Flyway `V25` pour le groupe sanguin et l'email.
- [x] Rédiger la migration Flyway `V26` pour la table des vaccinations.
- [x] Mettre à jour `PatientEntity`, DTOs, et le service backend pour le groupe sanguin et l'email.
- [x] Implémenter l'entité, le repository, le service et les endpoints REST pour les vaccinations.
- [x] Ajouter l'email aux mappers FHIR.
- [x] Intégrer le groupe sanguin et l'email dans la synthèse PDF.
- [x] Ajouter le lien duplicates dans la Sidebar pour l'ADMIN_CLINIQUE.
- [x] Mettre à jour les interfaces TypeScript, formulaires, et fiches de profil Angular.
- [x] Concevoir le composant de listage et d'ajout de vaccinations dans `patient-medical-info.component.ts`.
- [x] Écrire les tests unitaires pour les vaccinations backend et compiler.
- [x] Lancer la compilation frontend et valider l'absence d'erreurs.

## 5. Implémentation réalisée

- **DB/Migrations** : Création de `V25__add_blood_group_and_email_to_patients.sql` et `V26__create_patient_vaccinations_table.sql`.
- **Backend Core** :
  - Modification de `PatientEntity.java`, `CreatePatientRequest.java`, `PatientResponse.java` et `PatientController.java` pour intégrer `blood_group` et `email`.
  - Extension de `FhirPatientMapper.java` pour ajouter l'email dans la ressource.
  - Extension de `PdfGeneratorService.java` pour afficher le groupe sanguin et l'email sur la synthèse médicale PDF.
  - Création de `PatientVaccinationEntity.java` et `PatientVaccinationRepository.java`.
  - Intégration de la logique de vaccinations dans `PatientMedicalInfoService.java` et exposition REST sous `/api/patients/{patientId}/vaccinations` dans `PatientMedicalInfoController.java` avec contrôle d'accès par rôles et scopes granulaires.
- **Frontend Core** :
  - Mise à jour des interfaces `Patient`, `CreatePatientDto`, `PatientPortalMeResponse` dans `patient.models.ts` et `patient-portal.service.ts`.
  - Enregistrement du lien duplicates dans `app-shell.component.ts` pour la navigation de l'ADMIN_CLINIQUE.
  - Liaison des nouveaux champs dans `patient-list.component.ts`, `patient-form.component.ts` (modèle + template HTML avec select/input thémés).
  - Affichage des informations de groupe sanguin et d'email sur la fiche de détails patient (`patient-profile-tab.component.ts`) et le tableau de bord (`patient-profile-card.component.ts`).
  - Intégration de la section et du modal de saisie/visualisation de vaccinations dans `patient-medical-info.component.ts`.
- **Tests & QA** :
  - Ajout des tests d'intégration dans `PatientMedicalInfoControllerTest.java` (couverture de GET, POST, et PUT pour les vaccinations).
  - Validation du build et exécution réussie de l'intégralité des 196 tests backend (0 erreur) et de la compilation Angular frontend (0 erreur).

## 6. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-05 | Antigravity | 0.35j | 100% | Aucun | Aucun | Développement complet et validations OK. |

## 7. Statut final

Statut : DONE
