# DELIVERY DASHBOARD — Pilotage prévu / réalisé

## 1. Objectif

Donner une vision simple de la capacité, de l'avancement, des dérives et de la qualité.

## 2. Tableau sprint

| Sprint | Capacité planifiée | Charge engagée | Charge terminée | Taux livraison | Tickets réouverts | Bugs post-sprint | Commentaire |
|---|---:|---:|---:|---:|---:|---:|---|
| SPRINT-0001 | | | | | | | |
| SPRINT-0002 | 15.0j | 5.75j | 1.80j | En cours | 1 | 0 | Charge terminée = tickets DONE uniquement ; STORY-0402 terminée, STORY-0201 reste en review |
| SPRINT-0003 | 15.0j | 9.25j | À consolider | En cours | 0 | 0 | STORY-0104 backend/front implémentés ; validations Maven backend et build prod Angular restent bloquées par environnement |

## 3. Tableau par développeur

| Développeur | Profil | Capacité | Charge assignée | Temps passé | Tickets DONE | Tickets BLOCKED | Réouvertures | Commentaire |
|---|---|---:|---:|---:|---:|---:|---:|---|
| | | | | | | | | |

## 4. Tableau de dérive

| Ticket | Estimation | Temps passé | Écart | Cause probable | Action corrective |
|---|---:|---:|---:|---|---|
| STORY-0201 | 1.0j | 0.65j | -0.35j | Reprise UI ajoutée après review utilisateur, mais reste sous l'estimation initiale | Validation visuelle mobile + build production sous Node pair/LTS avant validation Lead |
| STORY-0402 | 0.8j | 0.90j | +0.10j | Correction complémentaire du blocage de file d'attente et sécurisation du fetch des constantes | Ticket passé DONE après tests backend/frontend/build |
| STORY-0104 | 0.8j | 0.55j | -0.25j | Backend et frontend réalisés ; validation Maven backend et build production Angular bloqués par réseau/dépendances externes | Relancer les tests backend et régler l'inlining Google Fonts pour le build production |

## 5. Qualité

| Indicateur | Valeur | Seuil alerte | Action |
|---|---:|---:|---|
| Taux réouverture | | > 10 % | Revoir DoD/tests |
| Bugs post-livraison | | > 3/sprint | Renforcer QA |
| Temps review moyen | | > 2 jours | Débloquer reviewers |
| Tickets BLOCKED | | > 20 % | Revoir cadrage/dépendances |
| WIP moyen/dev | | > 2 | Limiter tâches en cours |

## 6. Lecture

Ne jamais analyser un indicateur seul.

Exemple : un développeur lent avec zéro bug et beaucoup de tâches complexes peut être plus fiable qu'un développeur rapide avec beaucoup de réouvertures.


## Contraintes techniques par défaut

- Backend Spring Boot : Maven uniquement. Les estimations et capacités doivent intégrer les commandes Maven (`./mvnw test`, `./mvnw clean verify`).
- Frontend Angular : Tailwind CSS uniquement. Ne pas planifier de tâche basée sur Angular Material sauf ADR validée.

## Standards configuration obligatoires

- Backend Spring Boot : `application.yml` obligatoire. Les tâches backend doivent prévoir la vérification de `src/main/resources/application.yml` et des profils YAML. `application.properties` doit être traité comme une dette ou une non-conformité à corriger.
- Frontend Angular : `proxy.conf.json` obligatoire. Les tâches frontend doivent vérifier que `angular.json` référence le proxy via `proxyConfig` et que les services utilisent des chemins API relatifs.
- Toute demande qui impose `application.properties`, une URL backend hardcodée côté Angular ou l’absence de proxy doit être bloquée ou documentée par ADR avant implémentation.
