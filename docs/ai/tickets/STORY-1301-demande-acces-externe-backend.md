# STORY-1301 — Enregistrement de demande d'accès externe (backend)

## 1. Description et contexte

**Epic** : Demande d'accès externe (Module 13)  
**Titre** : Enregistrement de demande d'accès externe (backend)  
**Statut** : DONE  
**Priorité** : P1  
**Sprint** : SPRINT-0007  
**SP** : 5  
**Profil recommandé** : Intermédiaire  
**Estimation** : 1.2j (Senior: 0.8j, Junior: 2.0j)  

## 2. Objectifs

Permettre à un établissement de santé du réseau (ex: hôpital externe) de formuler une demande d'accès de consultation temporaire pour le DPU d'un patient en saisissant son numéro DPU, un motif d'accès et la durée de validité demandée (en heures).

## 3. Critères d'acceptation (DoD)

- [x] Table SQL `external_access_requests` avec DPU, organisation requérante, motif, durée, statut (EN_ATTENTE, APPROUVEE, REFUSEE, EXPIREE), date de création et date d'expiration.
- [x] Entité JPA, Repository et Service d'enregistrement des demandes d'accès.
- [x] API REST sécurisée `POST /api/external-access/requests` accessible aux praticiens.
- [x] Enregistrement d'un log d'audit à chaque demande créée.
- [x] Tests d'intégration unitaires complets sur la création et les validations de payload.

## 4. Reste à faire

- [x] Création du script de migration Flyway V20.
- [x] Implémentation de la couche JPA et des API du backend.
- [x] Couverture par tests unitaires et d'intégration MockMvc.
