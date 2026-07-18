# Changelog — STORY-2603 Prise de rendez-vous patient

Date : 2026-07-18  
Issue : #66  
Pull request : #67  
Epic : EPIC-0025  
Impact SemVer : MINOR

## Added

- endpoints patient sous `/api/patient/appointments/**` :
  - annuaire des médecins actifs du même établissement ;
  - créneaux réservables calculés depuis les règles, indisponibilités et rendez-vous actifs ;
  - réservation transactionnelle ;
  - liste « Mes rendez-vous » ;
  - annulation patient ;
- résolution du patient exclusivement côté serveur via `PatientAccessGuardService` ;
- validation explicite du médecin : same-tenant, actif et porteur du rôle `MEDECIN` ;
- verrou pessimiste du médecin afin de sérialiser les réservations concurrentes ;
- protection RM-04 : un seul rendez-vous actif par patient, médecin et journée clinique ;
- mapping de la contrainte unique de créneau vers `409 SLOT_UNAVAILABLE` ;
- règle temporelle testable d’annulation, avec limite exacte autorisée ;
- garde D7 : création d’une indisponibilité recouvrant un rendez-vous actif futur refusée par `409 AVAILABILITY_CONFLICT` ;
- page Angular `/patient/appointments` avec annuaire, filtres, slot-picker, confirmation, liste et annulation ;
- composant partagé `appointment-slot-picker` accessible au clavier ;
- dictionnaires FR/EN `features/appointments` ;
- états chargement, vide, succès, erreur et conflit ;
- rafraîchissement automatique des créneaux après réservation, annulation ou conflit.

## Security

- aucun `patientId` accepté dans les requêtes du portail ;
- rendez-vous d’un autre patient ou tenant masqué en `404 APPOINTMENT_NOT_FOUND` ;
- endpoints limités à `ROLE_PATIENT` ;
- mutations effectuées via `AppointmentEntity` et `saveAndFlush()`, sans bulk update ;
- aucune donnée médicale sensible ajoutée aux logs.

## Tests

- tests MockMvc de l’annuaire, des créneaux, de la réservation, du listing, de l’annulation, de l’anti-IDOR et de D7 ;
- deux scénarios de concurrence réels :
  - deux patients sur le même créneau ;
  - même patient, même médecin et même journée avec deux créneaux différents ;
- tests de frontière avant, à et après la limite d’annulation ;
- tests Angular de la page et du slot-picker ;
- CI GitHub Actions #807 entièrement verte : Maven `clean verify`, tests Angular et build production.

## Remaining

- revue humaine Tech Lead + QA ;
- fusion de la PR #67 après approbation ;
- recette visuelle authentifiée sur environnement déployé.
