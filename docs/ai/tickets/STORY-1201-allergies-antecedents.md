# STORY-1201 — Module Allergies & Antécédents Médicaux

## 1. Description et contexte

**Epic** : Dossier Patient Unique (DPU)  
**Titre** : Module Allergies & Antécédents Médicaux  
**Statut** : DONE  
**Priorité** : P1  
**Sprint** : SPRINT-0006  
**SP** : 5  
**Profil recommandé** : Intermédiaire / Senior  
**Estimation** : 1.2j (Senior: 0.8j, Junior: 2.0j)  

## 2. Objectifs

Permettre aux professionnels de santé d'enregistrer et de consulter de façon structurée les allergies, réactions, antécédents médicaux/chirurgicaux/familiaux et traitements permanents d'un patient pour assurer des prescriptions sûres.

## 3. Critères d'acceptation (DoD)

- [x] Création des tables SQL `patient_allergies` et `patient_medical_history` avec multi-tenant.
- [x] Endpoints backend REST CRUD de gestion des allergies et antécédents sécurisés par rôle.
- [x] Raccordement à l'Audit Service pour tracer tout ajout, modification ou désactivation.
- [x] Interface frontend de saisie et de visualisation sur la fiche du patient (visuels contrastés et harmonieux, Tailwind CSS v4).
- [x] Les allergies actives doivent être affichées avec un niveau de criticité visuel élevé (badge rouge/orange).
- [x] Les traductions FR/EN doivent être intégrées via `I18nService`.
- [x] Couverture par tests unitaires backend et frontend supérieure à 80%.

## 4. Reste à faire

- [x] Migration Flyway V18 pour la création des tables.
- [x] Classes Entités JPA, Repositories, DTOs et Services associés.
- [x] Contrôleurs REST et raccordement de la sécurité.
- [x] Composants Angular et intégration des traductions.
- [x] Écriture des suites de tests unitaires et d'intégration.
