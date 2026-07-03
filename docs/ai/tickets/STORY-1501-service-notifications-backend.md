# STORY-1501 — Socle et service d'envoi de notifications (backend)

## 1. Description et contexte

**Epic** : Notifications & Système d'Alertes (Module 15)  
**Titre** : Socle et service d'envoi de notifications (backend)  
**Statut** : DONE  
**Priorité** : P1  
**Sprint** : SPRINT-0007  
**SP** : 3  
**Profil recommandé** : Intermédiaire  
**Estimation** : 0.9j (Senior: 0.6j, Junior: 1.5j)  

## 2. Objectifs

Créer une infrastructure backend pour émettre et historiser les notifications destinées aux patients (demandes d'accès, validation, alertes de sécurité) et praticiens.

## 3. Critères d'acceptation (DoD)

- [x] Table SQL `notifications` avec destinataire, titre, message, canal, statut (NON_LU, LU), et date de création.
- [x] Service centralisé `NotificationService` capable de journaliser en BDD et d'orchestrer l'envoi via un mock de passerelle Email/SMS/WhatsApp.
- [x] Endpoints REST `GET /api/patient/me/notifications` (historique) et `POST /api/patient/me/notifications/{id}/read` (marquer comme lu) protégés par authentification patient.
- [x] Raccordement automatique : l'émission d'une demande d'accès externe doit automatiquement générer et enregistrer une notification pour le patient.
- [x] Tests unitaires et d'intégration MockMvc.

## 4. Reste à faire

- [x] Script Flyway de création de la table `notifications`.
- [x] Entités, repository, service et contrôleur REST.
- [x] Écriture de la couverture de tests unitaires backend.
