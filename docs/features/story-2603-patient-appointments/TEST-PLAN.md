# TEST-PLAN — STORY-2603 Prise de rendez-vous patient

## 1. Objectif

Prouver la disponibilité réelle des créneaux, l’intégrité transactionnelle, l’anti-IDOR, les règles de frontière et le parcours Angular complet.

## 2. Backend — annuaire

- patient authentifié : uniquement médecins actifs same-tenant ;
- utilisateur actif sans rôle médecin exclu ;
- médecin désactivé exclu ;
- médecin d’un autre tenant exclu ;
- filtres spécialité et service, séparés et combinés ;
- sans JWT → 401 ;
- rôle non patient → 403.

## 3. Backend — créneaux

- règles simples ;
- exception totale ou partielle ;
- créneau déjà réservé ;
- rendez-vous annulé libérant le créneau ;
- créneau passé exclu ;
- borne `from` au milieu d’un créneau : créneau commencé exclu ;
- horizon dépassé → `BOOKING_HORIZON_EXCEEDED` ;
- médecin absent, désactivé ou cross-tenant → `DOCTOR_NOT_FOUND` ;
- période invalide → `VALIDATION_ERROR`.

## 4. Backend — réservation

### Nominal

- création `CONFIRMED` ;
- `endAt` calculé côté serveur ;
- motif optionnel ;
- apparition dans « mes rendez-vous ».

### Validation

- créneau passé → `PAST_SLOT` ;
- hors horizon → `BOOKING_HORIZON_EXCEEDED` ;
- hors disponibilité ou masqué → `SLOT_UNAVAILABLE` ;
- créneau affiché puis réservé par un tiers → `SLOT_UNAVAILABLE` ;
- médecin désactivé avant confirmation → `DOCTOR_NOT_FOUND`.

### Concurrence

- deux patients, même médecin et même créneau : exactement un succès, une seule ligne active, l’autre réponse `SLOT_UNAVAILABLE` ;
- même patient, même médecin, deux créneaux différents le même jour : exactement un succès, l’autre `DUPLICATE_ACTIVE_APPOINTMENT` ;
- deux médecins différents le même jour : autorisé ;
- même médecin sur deux journées cliniques différentes : autorisé.

## 5. Backend — annulation

- plus de 24 h avant : succès ;
- limite exacte : succès ;
- après la limite : `CANCEL_DEADLINE_PASSED` ;
- rendez-vous d’un autre patient : 404 ;
- autre tenant : 404 ;
- déjà annulé : `INVALID_STATUS_TRANSITION` ;
- terminé ou no-show : `INVALID_STATUS_TRANSITION` ;
- `active_start_at` devient NULL ;
- le créneau est réservable après annulation.

## 6. Backend — D7

- création d’une indisponibilité sans rendez-vous : succès ;
- création recouvrant un rendez-vous actif futur : `409 AVAILABILITY_CONFLICT` ;
- fenêtre adjacente sans chevauchement : succès ;
- rendez-vous annulé dans la fenêtre : succès.

## 7. Angular

- chargement annuaire ;
- filtres et état vide ;
- sélection médecin ;
- chargement créneaux ;
- sélection clavier d’un créneau ;
- réservation nominale ;
- bouton désactivé pendant la requête ;
- conflit 409 : message + rafraîchissement ;
- chargement et affichage des rendez-vous ;
- annulation avec confirmation ;
- erreur de délai ;
- FR/EN ;
- light/dark ;
- mobile et desktop ;
- aucun texte visible codé en dur.

## 8. Commandes de validation

```bash
cd backend
./mvnw clean verify -Dspring.profiles.active=test

cd ../web
npm test
npm run build
```

## 9. Critères de sortie

- aucune erreur ou failure ;
- aucun test désactivé ;
- tests de concurrence reproductibles ;
- migrations H2/PostgreSQL existantes toujours vertes ;
- tests Angular et build production verts ;
- revue Tech Lead + QA sans bloquant.
