# STORY-1302 — Validation de demande d'accès externe (portail patient)

## 1. Description et contexte

**Epic** : Demande d'accès externe (Module 13)  
**Titre** : Validation de demande d'accès externe (portail patient)  
**Statut** : READY  
**Priorité** : P1  
**Sprint** : SPRINT-0007  
**SP** : 3  
**Profil recommandé** : Intermédiaire  
**Estimation** : 0.8j (Senior: 0.5j, Junior: 1.3j)  

## 2. Objectifs

Permettre au patient de visualiser toutes les demandes d'accès externes en attente de validation sur son espace portail, et d'approuver ou de refuser chaque demande en un clic.

## 3. Critères d'acceptation (DoD)

- [ ] API REST sécurisée patient `GET /api/patient/me/access-requests` pour lister ses demandes.
- [ ] API REST sécurisée patient `POST /api/patient/me/access-requests/{id}/approve` et `POST /api/patient/me/access-requests/{id}/reject`.
- [ ] Interface utilisateur sur le portail patient (Tailwind CSS v4, i18n FR/EN) affichant les demandes actives avec boutons d'action d'approbation et de rejet.
- [ ] Notification push/toast sur l'IHM après action.
- [ ] Tests unitaires Vitest de l'IHM portail patient.

## 4. Reste à faire

- [ ] Implémentation des endpoints backend dédiés aux actions du patient.
- [ ] Création du composant Angular d'affichage des demandes sur le portail patient.
- [ ] Tests unitaires et d'intégration frontend.
