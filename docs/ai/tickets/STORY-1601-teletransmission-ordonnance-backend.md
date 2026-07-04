# STORY-1601 — Télétransmission d'ordonnances à AllôPharma (backend)

## 1. Description et contexte

**Epic** : API & Intégration Partenaire (Module 16)  
**Titre** : Télétransmission d'ordonnances à AllôPharma (backend)  
**Statut** : READY  
**Priorité** : P1  
**Sprint** : SPRINT-0008  
**SP** : 5  
**Profil recommandé** : Senior  
**Estimation** : 1.5j (Senior: 1.0j, Junior: 2.5j)  

## 2. Objectifs

Permettre la transmission d'une ordonnance active à la plateforme externe AllôPharma via un endpoint sécurisé et un client HTTP sortant simulé.

## 3. Critères d'acceptation (DoD)

- [ ] Ajout des colonnes de transmission dans la table `prescriptions` (ex. `transmission_status` VARCHAR, `transmitted_at` TIMESTAMP) via migration Flyway.
- [ ] Endpoint `POST /api/patient/me/prescriptions/{id}/transmit` (pour le patient) et `POST /api/prescriptions/{id}/transmit` (pour le médecin prescripteur) sécurisés.
- [ ] Service `AlloPharmaClient` simulant l'envoi HTTP de la prescription (payload JSON contenant le numéro unique, la liste structurée des médicaments, le nom du médecin, et l'établissement prescripteur) avec logs.
- [ ] Journalisation de l'audit log avec l'action `TRANSMIT_PRESCRIPTION`.
- [ ] Tests d'intégration et de sécurité MockMvc validant les droits de transmission (IDOR patient et rôles).

## 4. Reste à faire

- [ ] Script de migration Flyway.
- [ ] Modification de l'entité `PrescriptionEntity` et du DTO.
- [ ] Création du client d'intégration externe simulation.
- [ ] Exposition des routes du contrôleur.
- [ ] Écriture de la suite de tests backend.
