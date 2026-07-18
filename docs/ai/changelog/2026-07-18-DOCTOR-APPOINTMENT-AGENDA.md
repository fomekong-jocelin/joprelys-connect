# 2026-07-18 — Agenda personnel du médecin

## Ajouté

- endpoint lecture seule `GET /api/doctor/appointments` ;
- permission dédiée `APPOINTMENT_READ_OWN` et migration V72 ;
- vue minimale des rendez-vous avec identité patient strictement nécessaire ;
- page Angular `/clinic/appointments` « Mon agenda » ;
- navigation semaine précédente, courante et suivante ;
- rafraîchissement manuel et automatique toutes les 30 secondes ;
- traductions françaises et anglaises ;
- tests backend de sécurité, isolation, période et confidentialité ;
- tests frontend de chargement, regroupement, navigation et polling.

## Corrigé

- une réservation effectuée par un patient devient visible dans l’agenda du médecin concerné ;
- l’agenda personnel est séparé du futur cahier global de l’accueil ;
- aucune lecture de l’agenda d’un confrère n’est possible via le contrat API.

## Sécurité

- l’identité médecin provient exclusivement du JWT ;
- aucun `doctorId` n’est accepté par l’API ;
- le rôle accueil et l’administrateur clinique ne reçoivent pas `APPOINTMENT_READ_OWN` ;
- aucune coordonnée ni donnée clinique patient n’est exposée ou journalisée.

## Compatibilité

- aucune rupture de contrat existant ;
- migration Flyway idempotente ;
- changement classé **MINOR** selon SemVer.

## Validation

- CI GitHub Actions #823 verte sur PR #68 ;
- Maven strict et suite backend globale réussis ;
- tests Angular et build production réussis ;
- recette métier croisée patient/médecin requise avant fusion.
