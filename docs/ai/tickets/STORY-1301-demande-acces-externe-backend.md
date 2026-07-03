# STORY-1301 — Enregistrement de demande d'accès externe (backend)

## 1. Description et contexte

**Epic** : Demande d'accès externe (Module 13)  
**Titre** : Enregistrement de demande d'accès externe (backend)  
**Statut** : READY  
**Priorité** : P1  
**Sprint** : SPRINT-0007  
**SP** : 5  
**Profil recommandé** : Intermédiaire  
**Estimation** : 1.2j (Senior: 0.8j, Junior: 2.0j)  

## 2. Objectifs

Permettre à un établissement de santé du réseau (ex: hôpital externe) de formuler une demande d'accès de consultation temporaire pour le DPU d'un patient en saisissant son numéro DPU, un motif d'accès et la durée de validité demandée (en heures).

## 3. Critères d'acceptation (DoD)

- [ ] Table SQL `external_access_requests` avec DPU, organisation requérante, motif, durée, statut (EN_ATTENTE, APPROUVEE, REFUSEE, EXPIREE), date de création et date d'expiration.
- [ ] Entité JPA, Repository et Service d'enregistrement des demandes d'accès.
- [ ] API REST sécurisée `POST /api/external-access/requests` accessible aux praticiens.
- [ ] Enregistrement d'un log d'audit à chaque demande créée.
- [ ] Tests d'intégration unitaires complets sur la création et les validations de payload.

## 4. Reste à faire

- [ ] Création du script de migration Flyway V20.
- [ ] Implémentation de la couche JPA et des API du backend.
- [ ] Couverture par tests unitaires et d'intégration MockMvc.
