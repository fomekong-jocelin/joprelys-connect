# STORY-2603 — Prise de rendez-vous patient

> GitHub : #66  
> Pull request : #67  
> Epic : EPIC-0025  
> Statut : IN REVIEW  
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
- [x] Mettre à jour le suivi global et le changelog avec l’état final.

### T-2603.2 — Backend
- [x] Exposer l’annuaire des médecins actifs same-tenant.
- [x] Exposer les créneaux futurs consommant `AvailabilityService.generateSlots()`.
- [x] Réserver dans une transaction après recalcul du créneau.
- [x] Garantir RM-04 sous concurrence par verrou pessimiste du médecin.
- [x] Mapper l’unicité DB du créneau vers `409 SLOT_UNAVAILABLE`.
- [x] Lister uniquement les rendez-vous du patient authentifié.
- [x] Annuler via `AppointmentEntity.cancel()` + `saveAndFlush()`.
- [x] Ajouter D7 : exception d’indisponibilité recouvrant un RDV actif futur → 409.
- [x] Ajouter les tests backend, sécurité et concurrence.

### T-2603.3 — Portail Angular
- [x] Ajouter la route `/patient/appointments`.
- [x] Ajouter le service API et les modèles typés.
- [x] Ajouter l’annuaire, les filtres et le slot-picker partagé.
- [x] Ajouter la confirmation de réservation et le rafraîchissement sur conflit.
- [x] Ajouter la liste « Mes rendez-vous » et l’annulation.
- [x] Ajouter i18n FR/EN, mobile-first, light/dark et clavier.
- [x] Ajouter les tests Angular.

### T-2603.4 — Validation
- [x] `./mvnw clean verify` vert — CI #807.
- [x] Tests Angular verts — 241 tests sur CI #807.
- [x] Build Angular production vert — CI #807.
- [x] Aucun test désactivé ou contourné.
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

## Couverture ajoutée

- tests MockMvc : AuthN/AuthZ, annuaire, créneaux, réservation, listing, annulation, anti-IDOR, médecins cross-tenant/désactivés et D7 ;
- tests de concurrence réels : deux patients/même créneau et même patient/même médecin/jour ;
- tests purs de la limite exacte d’annulation ;
- tests Angular de la page et du slot-picker ;
- non-régression globale Maven, y compris suites prescription/pharmacie après correction de l’isolation des fixtures.

## Definition of Ready

- [x] Objectif, périmètre et valeur métier documentés.
- [x] Contrat API cible disponible.
- [x] Dépendances fusionnées et CI vertes.
- [x] Risques de concurrence et anti-IDOR identifiés.
- [x] Tests attendus définis.

## Definition of Done

- [ ] Critères d’acceptation validés par CI et revue humaine.
- [x] Backend et Angular implémentés.
- [x] Tests de concurrence, sécurité et frontières verts en CI.
- [x] Documentation, suivi et changelog finalisés.
- [x] Aucun risque critique technique ouvert.

## Validation technique

GitHub Actions **#807**, tête `e2c8cdd7` :

- backend Maven strict : ✅ ;
- tests H2/PostgreSQL et migrations existantes : ✅ ;
- scénarios de concurrence : ✅ ;
- tests Angular : ✅ ;
- build Angular production : ✅ ;
- aucun artefact de workflow temporaire dans le diff final : ✅.

## Impact SemVer

**MINOR** — nouveaux endpoints et nouveau parcours patient, sans rupture des contrats existants.
