# STORY-1201 — Module Allergies & Antécédents Médicaux

## 1. Description et contexte

**Epic** : Dossier Patient Unique (DPU)  
**Titre** : Module Allergies & Antécédents Médicaux  
**Statut** : READY  
**Priorité** : P1  
**Sprint** : SPRINT-0006  
**SP** : 5  
**Profil recommandé** : Intermédiaire / Senior  
**Estimation** : 1.2j (Senior: 0.8j, Junior: 2.0j)  

## 2. Objectifs

Permettre aux professionnels de santé d'enregistrer et de consulter de façon structurée les allergies, réactions, antécédents médicaux/chirurgicaux/familiaux et traitements permanents d'un patient pour assurer des prescriptions sûres.

## 3. Critères d'acceptation (DoD)

- [ ] Création des tables SQL `patient_allergies` et `patient_medical_history` avec multi-tenant.
- [ ] Endpoints backend REST CRUD de gestion des allergies et antécédents sécurisés par rôle.
- [ ] Raccordement à l'Audit Service pour tracer tout ajout, modification ou désactivation.
- [ ] Interface frontend de saisie et de visualisation sur la fiche du patient (visuels contrastés et harmonieux, Tailwind CSS v4).
- [ ] Les allergies actives doivent être affichées avec un niveau de criticité visuel élevé (badge rouge/orange).
- [ ] Les traductions FR/EN doivent être intégrées via `I18nService`.
- [ ] Couverture par tests unitaires backend et frontend supérieure à 80%.

## 4. Reste à faire

- [ ] Migration Flyway V18 pour la création des tables.
- [ ] Classes Entités JPA, Repositories, DTOs et Services associés.
- [ ] Contrôleurs REST et raccordement de la sécurité.
- [ ] Composants Angular et intégration des traductions.
- [ ] Écriture des suites de tests unitaires et d'intégration.
