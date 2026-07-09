# STORY-2103 — Soins journaliers, médicaments et consommables en hospitalisation

## 1. Objectif

Permettre la consignation des soins journaliers, de l'administration de médicaments et des consommables lors d'une hospitalisation, et s'assurer que les soins facturables et consommables sont automatiquement répercutés dans la facturation patient.

## 2. Critères d'acceptation

- [x] Créer les tables de base de données `hospitalization_daily_cares`, `medication_administrations` et `patient_consumptions`.
- [x] Déclarer les entités JPA et les repositories Spring Boot correspondants.
- [x] Créer un service dédié `HospitalizationCareService` respectant la limite de 500 lignes de code et exposer les endpoints dans `HospitalizationController`.
- [x] Inclure automatiquement les soins journaliers facturables et les consommations patient dans la précalcul de la facture de visite (`BillingService`).
- [x] Mettre en place un système d'onglets premium dans l'interface Angular pour afficher/ajouter les soins, les médicaments et les consommables sans surcharger l'écran du séjour.
- [x] Assurer la compilation complète de l'application frontend et valider le bon fonctionnement via tests automatisés.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0017 |
| User story parent | STORY-2103 |
| Sprint cible | SPRINT-0012 |
| Priorité business | P0 |
| Complexité | M |
| Story points | 8 |
| Profil recommandé | Senior |
| Effort estimé senior | 1.5j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Moyen |
| Risque technique | Faible |
| Dépendances | STORY-2102 |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] Code existant analysé
- [x] Tests existants analysés
- [x] Contrats API analysés

## 5. Plan d'action & Avancement

- [x] Flyway migration schema : `V47__hospitalization_care_meds_consumables.sql`
- [x] Entités JPA : `HospitalizationDailyCareEntity`, `MedicationAdministrationEntity`, `PatientConsumptionEntity`
- [x] Repositories JPA : `HospitalizationDailyCareRepository`, `MedicationAdministrationRepository`, `PatientConsumptionRepository`
- [x] DTOs : `CreateDailyCareRequest`, `DailyCareResponse`, etc.
- [x] Service applicatif : `HospitalizationCareService`
- [x] REST Controller : `HospitalizationController` enrichi
- [x] Logique de précalcul facture : `BillingService` mis à jour
- [x] API Client Angular : `PatientApiService` enrichi
- [x] Interface utilisateur : `patient-hospitalization.component.ts` et `patient-hospitalization.component.html` avec onglets dynamiques
- [x] Tests unitaires et d'intégration validés (build Maven & build prod Angular OK)
