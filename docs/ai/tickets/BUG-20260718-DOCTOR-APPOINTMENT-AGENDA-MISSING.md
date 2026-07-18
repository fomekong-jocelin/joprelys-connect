# BUG-20260718 — Les rendez-vous réservés ne sont pas visibles par le médecin

## Métadonnées

| Champ | Valeur |
|---|---|
| Epic | EPIC-0025 — Disponibilités médecins et rendez-vous |
| Type | Bug fonctionnel + intégration manquante |
| Priorité | P1 |
| Statut | IN_PROGRESS |
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

- [ ] Un médecin authentifié consulte uniquement ses propres rendez-vous.
- [ ] Un utilisateur non médecin reçoit `403` même s’il possède une permission voisine.
- [ ] La période est obligatoire, valide et limitée à 92 jours.
- [ ] La réponse n’expose que les données patient nécessaires à l’agenda.
- [ ] La vue semaine affiche les états confirmé, annulé, terminé et absence.
- [ ] L’écran se rafraîchit automatiquement toutes les 30 secondes et propose un rafraîchissement manuel.
- [ ] Le frontend fonctionne en FR/EN, light/dark, mobile et au clavier.
- [ ] Les états chargement, vide et erreur sont couverts.
- [ ] Les tests backend et Angular couvrent l’isolation médecin et le rafraîchissement.

## Tâches

### Documentation

- [x] Créer le ticket et le diagnostic.
- [x] Définir le périmètre fonctionnel.
- [x] Définir le contrat API et le plan de tests.

### Backend

- [ ] Ajouter le DTO d’agenda médecin.
- [ ] Ajouter le service de requête tenanté et limité au médecin connecté.
- [ ] Exposer `GET /api/doctor/appointments`.
- [ ] Ajouter la requête repository demi-ouverte `[from, to)`.
- [ ] Ajouter les erreurs structurées et tests MockMvc.

### Frontend

- [ ] Ajouter modèles et service API.
- [ ] Ajouter la page `/clinic/appointments`.
- [ ] Ajouter navigation et route sous `APPOINTMENT_READ`.
- [ ] Ajouter les traductions FR/EN.
- [ ] Ajouter les tests Vitest.

### Validation

- [ ] Maven `clean verify` vert.
- [ ] Tests Angular verts.
- [ ] Build Angular production vert.
- [ ] Contrôle i18n vert.
- [ ] Revue humaine Tech Lead et QA métier.

## Risques

- Confusion entre agenda personnel du médecin et cahier global de l’accueil : les deux restent séparés. STORY-2604 pourra réutiliser le service de requête sans élargir silencieusement le périmètre.
- Données personnelles : aucune coordonnée, donnée clinique ou historique médical n’est exposé dans la réponse.
- Actualisation : le polling est volontairement limité à 30 secondes ; aucun WebSocket n’est introduit sans besoin mesuré.

## Reste à faire

Implémentation, validation CI et recette fonctionnelle authentifiée.