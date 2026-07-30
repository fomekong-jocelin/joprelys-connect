# MOB-2806 — Dashboard chaleureux et file d’attente active

## 1. Objectif

Transformer l’accueil professionnel Flutter en un poste de travail clinique utile, connecté à la file d’attente active déjà exposée par le backend.

## 2. Contrat réutilisé

- endpoint : `GET /api/visits/active` ;
- authentification : session professionnelle protégée par le client API central ;
- tri : heure d’arrivée croissante, avec repli sur la date de création ;
- aucun nouvel endpoint backend dans ce lot.

## 3. Critères d’acceptation

- [x] Le dashboard conserve la marque officielle Joprelys, FR/EN et light/dark.
- [x] La file active est chargée depuis `/api/visits/active`.
- [x] Les visites sont triées de la plus ancienne à la plus récente.
- [x] Le résumé indique le total, les constantes saisies et les patients à évaluer.
- [x] Chaque carte présente le patient, la référence de visite, le DPU, le motif, l’orientation/service, l’heure d’arrivée et l’état des constantes.
- [x] Les états loading, empty et error sont distincts.
- [x] Le rafraîchissement est disponible par bouton et pull-to-refresh.
- [x] L’appel réseau ne démarre qu’après disponibilité d’une session authentifiée.
- [x] Aucun secret ni token n’est journalisé.
- [ ] Le rendu est validé sur appareil Android en FR/EN et light/dark.
- [ ] `flutter analyze`, `flutter test` et le build APK sont verts sur le HEAD final.

## 4. Architecture

- `domain/active_visit.dart` : contrat mobile défensif pour les visites et constantes ;
- `data/active_visits_api.dart` : gateway branchée sur `ApiClient` ;
- `application/active_queue_controller.dart` : chargement, tri et rafraîchissement Riverpod ;
- `presentation/widgets/active_queue_section.dart` : résumé et cartes de file ;
- `foundation_page.dart` : intégration dans l’accueil professionnel existant.

## 5. Tests ajoutés

- parsing complet du contrat visite/constantes ;
- repli `arrivalAt` vers `createdAt` ;
- rejet des charges incomplètes ;
- tri chronologique du contrôleur ;
- rafraîchissement avec gateway substituée.

## 6. Risques et suites

- la session mobile ne porte pas encore la matrice complète de permissions ; un refus backend est donc présenté comme un état d’erreur sans extrapoler les droits depuis le rôle ;
- l’ouverture du dossier patient ou de la consultation depuis une carte sera raccordée aux stories métier suivantes ;
- les getters `gen-l10n` seront régénérés lors du gate Flutter final, les catalogues ARB étant la source de vérité.

## 7. Definition of Done

- [ ] format Dart vert ;
- [ ] analyse statique verte ;
- [ ] tests unitaires/widgets verts ;
- [ ] APK recette généré ;
- [ ] validation visuelle réelle ;
- [ ] PR revue et fusionnée.
