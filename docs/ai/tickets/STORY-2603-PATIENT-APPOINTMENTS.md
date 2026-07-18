# STORY-2603 — Prise de rendez-vous patient

> GitHub : #66  
> Epic : EPIC-0025  
> Statut : IN PROGRESS  
> Estimation : 8 SP / 3,5 j Senior  
> Profil : Senior full-stack  
> Reviewer : Tech Lead + QA

## User story

En tant que patient authentifié, je veux consulter les médecins de mon établissement, afficher leurs créneaux réellement disponibles, réserver un rendez-vous et l’annuler dans le délai autorisé, afin d’organiser ma prise en charge sans passer par l’accueil.

## Dépendances validées

- [x] STORY-2601 / PR #62 fusionnée — modèle, configuration et contrainte anti double-booking.
- [x] STORY-2602 / PR #63 fusionnée — disponibilités et `generateSlots()`.
- [x] CI verte sur les deux socles.

## Action plan

### T-2603.1 — Documentation et contrat
- [x] Créer le ticket GitHub #66.
- [x] Créer la documentation fonctionnelle initiale.
- [x] Créer la conception technique initiale.
- [x] Créer le plan de tests initial.
- [ ] Mettre à jour le suivi global et le changelog avec l’état final.

### T-2603.2 — Backend
- [ ] Exposer l’annuaire des médecins actifs same-tenant.
- [ ] Exposer les créneaux futurs consommant `AvailabilityService.generateSlots()`.
- [ ] Réserver dans une transaction après recalcul du créneau.
- [ ] Garantir RM-04 sous concurrence par verrou pessimiste du médecin.
- [ ] Mapper l’unicité DB du créneau vers `409 SLOT_UNAVAILABLE`.
- [ ] Lister uniquement les rendez-vous du patient authentifié.
- [ ] Annuler via `AppointmentEntity.cancel()` + `saveAndFlush()`.
- [ ] Ajouter D7 : exception d’indisponibilité recouvrant un RDV actif futur → 409.
- [ ] Ajouter les tests backend, sécurité et concurrence.

### T-2603.3 — Portail Angular
- [ ] Ajouter la route `/patient/appointments`.
- [ ] Ajouter le service API et les modèles typés.
- [ ] Ajouter l’annuaire, les filtres et le slot-picker partagé.
- [ ] Ajouter la confirmation de réservation et le rafraîchissement sur conflit.
- [ ] Ajouter la liste « Mes rendez-vous » et l’annulation.
- [ ] Ajouter i18n FR/EN, mobile-first, light/dark et clavier.
- [ ] Ajouter les tests Angular.

### T-2603.4 — Validation
- [ ] `./mvnw clean verify` vert.
- [ ] Tests Angular verts.
- [ ] Build Angular production vert.
- [ ] Aucun test désactivé ou contourné.
- [ ] Revue Tech Lead + QA.

## Règles métier

- RM-02 : un seul rendez-vous actif par médecin et instant de début, garanti par la base.
- RM-03 : réservation strictement future et dans l’horizon configurable.
- RM-04 : un seul rendez-vous actif par patient, médecin et journée clinique.
- RM-05 : annulation autorisée tant que `now <= startAt - délai`; défaut 24 h.
- RM-07/D7 : une nouvelle indisponibilité ne peut pas recouvrir un rendez-vous actif futur.
- Le patient est toujours résolu côté serveur via `PatientAccessGuardService`; aucun `patientId` en entrée.
- Les rendez-vous d’un autre patient ou tenant sont indistinguables d’une ressource absente.
- Toute mutation d’un rendez-vous passe par l’entité JPA et `save()`/`saveAndFlush()`; aucun bulk update.

## Definition of Ready

- [x] Objectif, périmètre et valeur métier documentés.
- [x] Contrat API cible disponible.
- [x] Dépendances fusionnées et CI vertes.
- [x] Risques de concurrence et anti-IDOR identifiés.
- [x] Tests attendus définis.

## Definition of Done

- [ ] Critères d’acceptation validés.
- [ ] Backend et Angular livrés.
- [ ] Tests de concurrence, sécurité et frontières verts.
- [ ] Documentation, suivi et changelog à jour.
- [ ] Aucun risque critique ouvert.

## Impact SemVer

**MINOR** — nouveaux endpoints et nouveau parcours patient, sans rupture des contrats existants.
