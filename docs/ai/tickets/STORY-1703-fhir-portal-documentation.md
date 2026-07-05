# STORY-1703 — Portail Développeur & Documentation OpenAPI/Swagger pour les endpoints FHIR

## 1. Description et contexte
**Epic** : Interopérabilité HL7 FHIR (EPIC-0011)  
**Titre** : Portail Développeur & Documentation OpenAPI/Swagger pour les endpoints FHIR  
**Statut** : TODO  
**Priorité** : P2  
**Sprint** : SPRINT-0009  
**SP** : 3  
**Profil recommandé** : Intermédiaire  
**Estimation** : 1.0j (Senior: 0.7j, Junior: 1.7j)  

## 2. Objectifs
Documenter le contrat d'API FHIR exposé et offrir une interface visuelle interactive pour les développeurs partenaires.

## 3. Critères d'acceptation (DoD)
- [ ] Documentation OpenAPI (Swagger UI) enrichie avec les endpoints `/fhir/*` et leurs schémas JSON R4 de retour.
- [ ] Intégration de descriptions claires et exemples de payloads de retour pour `Patient`, `Encounter` et `Observation`.
- [ ] Page de documentation ou guide développeur dans le projet (ex. `docs/features/epic-0011-fhir-interoperability/USER-GUIDE.md`).
- [ ] Vérification que la documentation OpenAPI compile et s'affiche sans erreur au démarrage.

## 4. Reste à faire
- [ ] Annoter les endpoints du contrôleur pour Swagger/Springdoc.
- [ ] Rédiger le guide d'utilisation de l'API externe.
