# README PM — Gouvernance projet et Scrum

Ce dossier complète la gouvernance technique avec une couche chef de projet.

## Objectif

- Transformer les besoins macro en backlog exploitable.
- Estimer les tâches selon complexité, risque et profil.
- Planifier les sprints selon la capacité réelle.
- Mesurer les dérives sans piloter au ressenti.
- Identifier les blocages et la dette cachée.
- Produire des rapports synthétiques.

## Ordre de lecture

1. `../../PROJECT-MANAGER-SKILL.md`
2. `SCRUM-WORKFLOW.md`
3. `ESTIMATION-GUIDE.md`
4. `TEAM-PROFILES.md`
5. `CAPACITY-PLANNING.md`
6. `DELIVERY-DASHBOARD.md`
7. templates nécessaires dans `templates`

## Règle principale

Une demande macro doit être découpée avant d'être confiée à un développeur.

## Documents clés

| Document | Usage |
|---|---|
| `SCRUM-WORKFLOW.md` | Rythme Scrum et cérémonies |
| `ESTIMATION-GUIDE.md` | Story points et effort par profil |
| `TEAM-PROFILES.md` | Matrice junior/intermédiaire/senior |
| `CAPACITY-PLANNING.md` | Calcul de capacité sprint |
| `SPRINT-PLANNING.md` | Préparer un sprint |
| `DELIVERY-DASHBOARD.md` | Suivi prévu/réalisé/dérive |
| `templates` | Modèles opérationnels |


## Contraintes techniques par défaut

- Backend Spring Boot : Maven uniquement. Les estimations et capacités doivent intégrer les commandes Maven (`./mvnw test`, `./mvnw clean verify`).
- Frontend Angular : Tailwind CSS v4 uniquement. Ne pas planifier de tâche basée sur Angular Material sauf ADR validée.

## Standards configuration obligatoires

- Backend Spring Boot : `application.yml` obligatoire. Les tâches backend doivent prévoir la vérification de `src/main/resources/application.yml` et des profils YAML. `application.properties` doit être traité comme une dette ou une non-conformité à corriger.
- Frontend Angular : `proxy.conf.json` obligatoire. Les tâches frontend doivent vérifier que `angular.json` référence le proxy via `proxyConfig` et que les services utilisent des chemins API relatifs.
- Toute demande qui impose `application.properties`, une URL backend hardcodée côté Angular ou l’absence de proxy doit être bloquée ou documentée par ADR avant implémentation.
