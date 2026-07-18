# FUNCTIONAL-SPEC — STORY-2603 Prise de rendez-vous patient

## 1. Problème métier

Le patient ne dispose pas encore d’un parcours autonome pour identifier un médecin, consulter les disponibilités réelles, réserver puis gérer son rendez-vous. L’accueil doit actuellement intervenir pour toute planification.

## 2. Utilisateur concerné

- Patient authentifié sur le portail Joprelys.

## 3. Objectif

Permettre au patient de :

1. parcourir les médecins actifs de son établissement ;
2. filtrer par spécialité ou service ;
3. consulter les créneaux réellement réservables ;
4. réserver un créneau ;
5. consulter ses rendez-vous ;
6. annuler dans le délai autorisé.

## 4. Périmètre inclus

- annuaire same-tenant des utilisateurs actifs portant le rôle `MEDECIN` ;
- créneaux calculés côté serveur à partir des règles, exceptions et rendez-vous actifs ;
- réservation transactionnelle ;
- annulation patient ;
- liste « Mes rendez-vous » ;
- interface Angular mobile-first FR/EN, light/dark et accessible au clavier.

## 5. Hors périmètre

- paiement en ligne ;
- téléconsultation ;
- liste d’attente ;
- rappels e-mail/SMS, traités par STORY-2605 ;
- check-in et agenda accueil, traités par STORY-2604 ;
- replanification directe d’un rendez-vous : annulation puis nouvelle réservation.

## 6. Parcours utilisateur

### 6.1 Réserver

1. Le patient ouvre « Rendez-vous ».
2. Le système charge les médecins actifs de son établissement.
3. Le patient filtre ou sélectionne un médecin.
4. Le système affiche uniquement les créneaux futurs et dans l’horizon autorisé.
5. Le patient sélectionne un créneau et saisit éventuellement un motif.
6. Une confirmation récapitule médecin, date et heure.
7. Le backend recalcule la disponibilité dans la transaction.
8. En cas de succès, le rendez-vous apparaît dans « Mes rendez-vous ».
9. En cas de conflit, le patient reçoit un message clair et les créneaux sont actualisés.

### 6.2 Annuler

1. Le patient consulte « Mes rendez-vous ».
2. Il choisit un rendez-vous confirmé.
3. Le système vérifie le délai configurable.
4. Si la limite n’est pas dépassée, le rendez-vous passe à `CANCELLED_BY_PATIENT` et le créneau est libéré.
5. Sinon, l’annulation est refusée avec `CANCEL_DEADLINE_PASSED`.

## 7. Règles métier

- Un médecin doit être actif, appartenir au même établissement et porter le rôle `MEDECIN`.
- Un créneau doit être strictement futur : un créneau déjà commencé n’est jamais proposé.
- L’horizon de réservation est configurable, par défaut 30 jours.
- Un seul rendez-vous actif est autorisé pour un médecin et un instant de début.
- Un seul rendez-vous actif est autorisé pour un patient, un médecin et une journée dans le fuseau de la clinique.
- Le délai d’annulation est configurable, par défaut 24 heures.
- À la limite exacte, `now == startAt - délai`, l’annulation reste autorisée.
- Seul un rendez-vous `CONFIRMED` peut être annulé par le patient.
- Le patient ne voit et ne modifie que ses propres rendez-vous.
- Une nouvelle indisponibilité médecin ne peut pas recouvrir un rendez-vous actif futur.

## 8. Cas limites

- créneau affiché puis pris par un autre patient avant confirmation ;
- deux réservations simultanées sur le même créneau ;
- même patient réservant simultanément deux horaires du même médecin le même jour ;
- médecin désactivé entre l’affichage et la réservation ;
- créneau passé ou hors horizon ;
- rendez-vous annulé, terminé ou marqué absent ;
- tentative d’accès à l’identifiant d’un rendez-vous d’un autre patient ;
- filtres sans résultat ;
- changement de langue ou de thème pendant le parcours.

## 9. Critères d’acceptation

- [ ] L’annuaire n’expose que les médecins actifs same-tenant.
- [ ] Les créneaux masqués, réservés, passés, commencés ou hors horizon ne sont pas proposés.
- [ ] Une réservation concurrente ne produit jamais deux rendez-vous actifs sur le même créneau.
- [ ] RM-04 reste vraie sous concurrence.
- [ ] Les ressources d’un autre patient répondent 404.
- [ ] L’annulation respecte la limite configurable et libère le créneau.
- [ ] Les états chargement, vide, erreur et conflit sont présents.
- [ ] Le parcours fonctionne au clavier, sur mobile, en FR/EN et en light/dark.

## 10. Hypothèses

- Le fuseau de la clinique est fourni par `joprelys.appointments.timezone`.
- La durée de créneau est uniforme pour cette version et configurée côté serveur.
- L’identité patient est résolue par le numéro patient global porté par l’authentification existante.
