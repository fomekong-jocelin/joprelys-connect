# FUNCTIONAL-SPEC — Agenda personnel du médecin

## 1. Problème métier

Un patient peut réserver un créneau auprès d’un médecin, mais le médecin ne dispose d’aucune vue lui permettant de connaître ses rendez-vous à venir. Le créneau disparaît bien des disponibilités, sans être matérialisé dans un agenda professionnel.

## 2. Utilisateur concerné

- médecin authentifié et actif dans l’établissement courant ;
- aucun accès patient, infirmier, caissier ou autre rôle non médecin ;
- l’administrateur et l’accueil disposeront d’un cahier global séparé dans STORY-2604.

## 3. Objectif

Permettre au médecin de :

1. consulter ses rendez-vous sur une semaine ;
2. naviguer vers la semaine précédente ou suivante ;
3. revenir à la semaine courante ;
4. distinguer les statuts du rendez-vous ;
5. identifier le patient avec un minimum de données ;
6. voir apparaître une nouvelle réservation sans recharger toute l’application.

## 4. Périmètre inclus

- agenda personnel, limité au médecin connecté ;
- période demi-ouverte `[from, to)` ;
- vue semaine responsive ;
- actualisation initiale, manuelle et automatique toutes les 30 secondes ;
- affichage du nom patient, numéro patient local, motif, heures et statut ;
- états chargement, vide et erreur ;
- i18n FR/EN, light/dark, navigation clavier.

## 5. Hors périmètre

- réservation ou replanification par le médecin ;
- annulation par la clinique ;
- check-in, no-show et conversion en visite ;
- ouverture directe du dossier patient depuis l’agenda ;
- agenda global de l’accueil ;
- notifications e-mail/SMS ;
- synchronisation Google Calendar, Outlook ou iCal ;
- WebSocket/SSE.

## 6. Règles métier

- Le backend déduit toujours l’identité du médecin du JWT ; aucun `doctorId` n’est accepté.
- Le compte doit être actif et porter le rôle `MEDECIN`.
- La permission `APPOINTMENT_READ_OWN` est réservée au rôle système `MEDECIN` ; elle est distincte de `APPOINTMENT_READ`, utilisée par le futur cahier global.
- La période est obligatoire et sa fin doit être strictement postérieure au début.
- Une requête ne peut couvrir plus de 92 jours.
- Tous les statuts sont retournés afin que l’annulation ou l’absence reste traçable.
- L’isolation établissement est appliquée par le tenant Hibernate et par la résolution same-tenant du compte.
- Les données exposées sont limitées au besoin d’agenda : aucune adresse, téléphone, donnée clinique, allergie ou historique médical.
- Le frontend n’infère aucune autorisation ; la route et le menu s’appuient sur `APPOINTMENT_READ_OWN`, comme l’API.

## 7. Parcours

1. Le médecin ouvre « Mon agenda ».
2. La semaine courante est calculée dans le fuseau du navigateur, du lundi 00:00 au lundi suivant 00:00.
3. L’application appelle l’API avec des instants ISO-8601 UTC.
4. Les rendez-vous sont regroupés par jour et triés par heure croissante.
5. Une réservation créée pendant que la page est ouverte apparaît au plus tard au prochain rafraîchissement automatique.
6. Une erreur réseau conserve l’écran et affiche un message explicite ; le prochain cycle peut réessayer.

## 8. Critères d’acceptation

- [x] La réservation patient apparaît dans l’agenda du médecin associé.
- [x] Elle n’apparaît jamais chez un autre médecin.
- [x] Un rôle sans `APPOINTMENT_READ_OWN` reçoit `403`.
- [x] Un médecin désactivé ne peut pas être authentifié et reçoit `401`.
- [x] Les périodes invalides ou supérieures à 92 jours reçoivent `400 VALIDATION_ERROR`.
- [x] Les données patient sensibles ne sont pas exposées.
- [x] La page respecte les standards UI du dépôt et ne dépasse pas 300 lignes cible.
- [x] Les tests backend et frontend couvrent le nominal, la sécurité et le rafraîchissement.
