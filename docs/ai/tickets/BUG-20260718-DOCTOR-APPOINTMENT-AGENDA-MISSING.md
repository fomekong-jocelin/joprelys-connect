# BUG-20260718 — Les rendez-vous réservés ne sont pas visibles par le médecin

## Métadonnées

| Champ | Valeur |
|---|---|
| Epic | EPIC-0025 — Disponibilités médecins et rendez-vous |
| Type | Bug fonctionnel + intégration manquante |
| Priorité | P1 |
| Statut | QA TECHNIQUE VERTE — PR #68 |
| Stack | Spring Boot / Angular / RBAC / QA |
| Estimation | 3 SP — 1,5 j Senior / 2,5 j Intermédiaire / 4 j Junior encadré |
| Profil recommandé | Senior full-stack |
| Reviewer | Tech Lead + QA + référent médical |
| SemVer | MINOR, fonctionnalité rétrocompatible |

## Constat

La réservation patient est persistée dans `appointments` et retire correctement le créneau de la liste des disponibilités. En revanche, aucun endpoint ni écran ne permet au médecin connecté de consulter ses propres rendez-vous. La page `clinic/availability` ne gère que les règles récurrentes et les indisponibilités.

## Cause racine

STORY-2603 a couvert uniquement le portail patient. Le cahier de rendez-vous professionnel a été reporté à STORY-2604, sans vue intermédiaire dédiée au médecin. Il ne s’agit pas d’un défaut de rafraîchissement mais d’un raccordement backend/frontend absent.

## Objectif

Ajouter un agenda personnel du médecin qui affiche automatiquement les rendez-vous de la semaine courante, sans exposer l’agenda d’un confrère ni des données patient inutiles.

## Critères d’acceptation

- [x] Un médecin authentifié consulte uniquement ses propres rendez-vous.
- [x] Un utilisateur non médecin reçoit `403` sans disposer de la permission dédiée.
- [x] Un compte désactivé est rejeté par le filtre d’authentification avec `401`.
- [x] La période est obligatoire, valide et limitée à 92 jours.
- [x] La réponse n’expose que les données patient nécessaires à l’agenda.
- [x] La vue semaine affiche les états confirmé, annulé, terminé et absence.
- [x] L’écran se rafraîchit automatiquement toutes les 30 secondes et propose un rafraîchissement manuel.
- [x] Le frontend fonctionne en FR/EN, light/dark, mobile et au clavier.
- [x] Les états chargement, vide et erreur sont couverts.
- [x] Les tests backend et Angular couvrent l’isolation médecin et le rafraîchissement.

## Tâches

### Documentation

- [x] Créer le ticket et le diagnostic.
- [x] Définir le périmètre fonctionnel.
- [x] Définir le contrat API et le plan de tests.
- [x] Ajouter le fragment de changelog.

### Backend

- [x] Ajouter le DTO d’agenda médecin.
- [x] Ajouter le service de requête tenanté et limité au médecin connecté.
- [x] Exposer `GET /api/doctor/appointments`.
- [x] Ajouter la requête repository demi-ouverte `[from, to)`.
- [x] Ajouter la permission dédiée `APPOINTMENT_READ_OWN` et la migration V72.
- [x] Ajouter les erreurs structurées et tests MockMvc.
- [x] Tester la matrice RBAC médecin / accueil / administrateur clinique.

### Frontend

- [x] Ajouter modèles et service API.
- [x] Ajouter la page `/clinic/appointments`.
- [x] Ajouter navigation et route sous `APPOINTMENT_READ_OWN`.
- [x] Ajouter les traductions FR/EN.
- [x] Ajouter les tests Vitest.

### Validation

- [x] Maven `clean verify` vert — CI #823.
- [x] Tests Angular verts — CI #823.
- [x] Build Angular production vert — CI #823.
- [x] Contrôle i18n couvert par la suite frontend et les dictionnaires alignés.
- [ ] Revue humaine Tech Lead et QA métier.

## Risques

- Confusion entre agenda personnel du médecin et cahier global de l’accueil : les deux restent séparés. STORY-2604 pourra réutiliser le service de requête sans élargir silencieusement le périmètre.
- Données personnelles : aucune coordonnée, donnée clinique ou historique médical n’est exposé dans la réponse.
- Actualisation : le polling est volontairement limité à 30 secondes ; aucun WebSocket n’est introduit sans besoin mesuré.

## Reste à faire

Réaliser la recette fonctionnelle authentifiée patient → médecin et obtenir l’approbation Tech Lead/QA avant fusion de PR #68.
