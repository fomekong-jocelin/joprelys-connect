# SPRINT PLANNING — Préparer un sprint

## 1. Entrées nécessaires

- Objectif du sprint.
- Capacité réelle de l'équipe.
- Backlog priorisé.
- Tickets READY.
- Disponibilités.
- Dépendances.
- Risques connus.

## 2. Étapes

1. Définir l'objectif du sprint.
2. Calculer la capacité planifiable.
3. Sélectionner uniquement des tickets READY.
4. Vérifier l'équilibre junior/intermédiaire/senior.
5. Garder une marge pour bugs/support.
6. Identifier les reviewers.
7. Valider le DoR et le DoD.
8. Documenter dans `docs/pm/sprints/SPRINT-XXXX.md`.

## 3. Règles

- Ne pas planifier de ticket flou.
- Ne pas charger à 100 %.
- Ne pas donner plusieurs gros tickets à un même développeur.
- Ne pas démarrer une tâche bloquée par une décision métier.
- Ne pas planifier une story à 13 points : découper.

## 4. Sortie attendue

Le sprint plan doit contenir :

- objectif ;
- capacité ;
- tickets sélectionnés ;
- charge totale ;
- charge par développeur ;
- risques ;
- dépendances ;
- marge ;
- définition de succès.


## Contraintes techniques par défaut

- Backend Spring Boot : Maven uniquement. Les estimations et capacités doivent intégrer les commandes Maven (`./mvnw test`, `./mvnw clean verify`).
- Frontend Angular : Tailwind CSS v4 uniquement. Ne pas planifier de tâche basée sur Angular Material sauf ADR validée.

## Standards configuration obligatoires

- Backend Spring Boot : `application.yml` obligatoire. Les tâches backend doivent prévoir la vérification de `src/main/resources/application.yml` et des profils YAML. `application.properties` doit être traité comme une dette ou une non-conformité à corriger.
- Frontend Angular : `proxy.conf.json` obligatoire. Les tâches frontend doivent vérifier que `angular.json` référence le proxy via `proxyConfig` et que les services utilisent des chemins API relatifs.
- Toute demande qui impose `application.properties`, une URL backend hardcodée côté Angular ou l’absence de proxy doit être bloquée ou documentée par ADR avant implémentation.

## Capacité documentation

Lors du sprint planning, prévoir explicitement du temps pour :

- créer la documentation fonctionnelle ;
- créer la documentation technique ;
- documenter API / DB / configuration si impact ;
- mettre à jour changelog et décision SemVer ;
- relire la documentation en review.

Une story sans documentation initiale ne doit pas entrer en sprint, sauf urgence explicitement marquée et régularisée avant `DONE`.

## Cadrage EPIC-0020

EPIC-0020 est prête au refinement mais ne doit pas être engagée en développement complet avant :

- l'arbitrage DAF sur `PAID` versus `SETTLED` ;
- la validation de la vue détail facture (panneau latéral ou route dédiée) ;
- la disponibilité d'un jeu de données tiers-payant avec paiement patient partiel et règlement assurance ;
- la réservation d'une capacité de deux sprints à 60–70 % maximum.
