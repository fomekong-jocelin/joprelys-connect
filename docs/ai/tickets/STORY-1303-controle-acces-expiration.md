# STORY-1303 — Contrôle d'accès & Expiration des droits externes

## 1. Description et contexte

**Epic** : Demande d'accès externe (Module 13)  
**Titre** : Contrôle d'accès & Expiration des droits externes  
**Statut** : DONE  
**Priorité** : P0  
**Sprint** : SPRINT-0007  
**SP** : 5  
**Profil recommandé** : Senior  
**Estimation** : 1.5j (Senior: 1.0j, Junior: 2.5j)  

## 2. Objectifs

Verrouiller et sécuriser la consultation du DPU par un établissement externe. Un praticien externe ne doit pouvoir charger le DPU d'un patient que si une demande d'accès est active, validée (APPROUVEE) et non expirée, ou par mode d'accès "Brise-Glace" (urgence).

## 3. Critères d'acceptation (DoD)

- [x] Validation de l'accès au niveau de la couche Spring Security ou dans le filtre de contrôle d'accès patient.
- [x] Mode d'accès urgence ("Break the Glass") disponible pour les médecins (lève immédiatement les restrictions de consentement mais génère un log d'audit rouge critique `EMERGENCY_DPU_ACCESS` et alerte le patient).
- [x] Planificateur Spring `@Scheduled` ou mécanisme automatique de passage des demandes d'accès à l'état `EXPIREE` dès la fin de validité de la durée d'accès.
- [x] Tests d'intégration de sécurité simulant des tentatives d'accès externe illégitimes (validation de HTTP 403 Forbidden).

## 4. Reste à faire

- [x] Implémentation du filtre de sécurité inter-établissements.
- [x] Ajout du workflow d'urgence.
- [x] Planification de la tâche d'expiration.
- [x] Écriture des tests d'intégration de sécurité.
