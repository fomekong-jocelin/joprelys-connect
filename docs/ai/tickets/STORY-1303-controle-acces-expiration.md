# STORY-1303 — Contrôle d'accès & Expiration des droits externes

## 1. Description et contexte

**Epic** : Demande d'accès externe (Module 13)  
**Titre** : Contrôle d'accès & Expiration des droits externes  
**Statut** : READY  
**Priorité** : P0  
**Sprint** : SPRINT-0007  
**SP** : 5  
**Profil recommandé** : Senior  
**Estimation** : 1.5j (Senior: 1.0j, Junior: 2.5j)  

## 2. Objectifs

Verrouiller et sécuriser la consultation du DPU par un établissement externe. Un praticien externe ne doit pouvoir charger le DPU d'un patient que si une demande d'accès est active, validée (APPROUVEE) et non expirée, ou par mode d'accès "Brise-Glace" (urgence).

## 3. Critères d'acceptation (DoD)

- [ ] Validation de l'accès au niveau de la couche Spring Security ou dans le filtre de contrôle d'accès patient.
- [ ] Mode d'accès urgence ("Break the Glass") disponible pour les médecins (lève immédiatement les restrictions de consentement mais génère un log d'audit rouge critique `EMERGENCY_DPU_ACCESS` et alerte le patient).
- [ ] Planificateur Spring `@Scheduled` ou mécanisme automatique de passage des demandes d'accès à l'état `EXPIREE` dès la fin de validité de la durée d'accès.
- [ ] Tests d'intégration de sécurité simulant des tentatives d'accès externe illégitimes (validation de HTTP 403 Forbidden).

## 4. Reste à faire

- [ ] Implémentation du filtre de sécurité inter-établissements.
- [ ] Ajout du workflow d'urgence.
- [ ] Planification de la tâche d'expiration.
- [ ] Écriture des tests d'intégration de sécurité.
