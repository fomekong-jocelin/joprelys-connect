# STORY-1502 — Centre de notifications sur le portail patient (IHM)

## 1. Description et contexte

**Epic** : Notifications & Système d'Alertes (Module 15)  
**Titre** : Centre de notifications sur le portail patient (IHM)  
**Statut** : READY  
**Priorité** : P2  
**Sprint** : SPRINT-0007  
**SP** : 3  
**Profil recommandé** : Junior/Intermédiaire  
**Estimation** : 0.6j (Senior: 0.4j, Junior: 1.0j)  

## 2. Objectifs

Permettre au patient de consulter ses notifications (reçues lors des demandes d'accès ou autres actions) dans un tiroir ou un panneau dédié de son tableau de bord avec un design premium (Tailwind CSS v4).

## 3. Critères d'acceptation (DoD)

- [ ] Panneau ou composant de centre de notifications sur le tableau de bord patient.
- [ ] Badge indicateur avec le compte des notifications non lues.
- [ ] Actions pour marquer une notification individuelle comme lue ou tout marquer comme lu.
- [ ] Affichage dynamique de listes avec des styles distincts selon le type d'alerte (sécurité, information, urgence).
- [ ] Support i18n FR/EN et styles light/dark conformes à `DESIGN.md`.
- [ ] Tests unitaires Angular Vitest.

## 4. Reste à faire

- [ ] Création du composant Angular `PatientNotificationsComponent`.
- [ ] Raccordement à `PatientApiService` pour charger et actualiser les données.
- [ ] Écriture des tests unitaires Angular.
