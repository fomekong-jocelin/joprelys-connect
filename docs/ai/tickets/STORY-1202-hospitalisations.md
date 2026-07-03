# STORY-1202 — Module Hospitalisations, lits et notes journalières

## 1. Description et contexte

**Epic** : Gestion de la Clinique Pilote  
**Titre** : Module Hospitalisations, lits et notes journalières  
**Statut** : DONE  
**Priorité** : P1  
**Sprint** : SPRINT-0006  
**SP** : 8  
**Profil recommandé** : Senior  
**Estimation** : 2.5j (Senior: 1.8j, Junior: 4.0j)  

## 2. Objectifs

Permettre aux équipes de tri, infirmiers et médecins de gérer les séjours hospitaliers des patients : affectation à une chambre et un lit, saisie d'observations quotidiennes de suivi, et génération de la fiche de sortie d'hospitalisation au format PDF.

## 3. Critères d'acceptation (DoD)

- [x] Migration Flyway pour la gestion des hospitalisations (`hospitalizations`, `hospitalization_records`).
- [x] JPA Entity avec verrouillage optimiste (`@Version` sur un champ `version`) pour éviter les conflits d'affectation de lits et chambres en cas d'accès concurrent.
- [x] Endpoints backend REST CRUD de gestion des séjours hospitaliers sécurisés par rôle.
- [x] Intégration de la génération du résumé de sortie d'hospitalisation au format PDF (avec signatures).
- [x] Interface frontend pour l'admission (choix de chambre/lit), les transmissions/observations quotidiennes, et la déclaration de sortie.
- [x] Traductions FR/EN complètes dans `I18nService`.
- [x] Tests de concurrence et d'intégration unitaires pour la libération et l'affectation de lits.

## 4. Reste à faire

- [x] Scripts SQL de migration de schéma.
- [x] Implémentation du service d'hospitalisation avec concurrence optimiste.
- [x] Modèle PDF de sortie d'hospitalisation.
- [x] Interfaces utilisateur réactives sous Tailwind CSS v4.
- [x] Écriture de la couverture de tests.
