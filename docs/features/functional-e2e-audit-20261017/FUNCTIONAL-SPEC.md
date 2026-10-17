# FUNCTIONAL-SPEC — Audit fonctionnel de bout en bout (2026-10-17)

## Besoin

Pouvoir expliquer sans ambiguïté comment le centre est configuré, qui fait quoi, comment le patient passe d'une étape à l'autre et comment les exceptions sont gérées, puis disposer d'un plan de correction applicable.

## Périmètre

Configuration de l'établissement, rôles et permissions, circuits ambulatoire, laboratoire, prescription et pharmacie, hospitalisation et lits, urgences, facturation, caisse et rendez-vous. Le portail patient, Connect, la comptabilité, le mobile et l'IA sont couverts statiquement ou listés comme limites.

## Livrables

| Livrable | Fichier |
|---|---|
| Rapport (synthèse, couverture, cartographie, configuration, responsabilités, constats, parcours cibles, questions) | [AUDIT-REPORT.md](AUDIT-REPORT.md) |
| Plan de recette | [TEST-PLAN.md](TEST-PLAN.md) |
| Preuves d'exécution | [RUNTIME-EVIDENCE.md](RUNTIME-EVIDENCE.md) |
| Backlog | [EPIC-0029](../../pm/backlog/EPIC-0029-functional-journey-coherence.md) |

## Règles de l'audit

- Les documents décrivent une intention ; le code et l'exécution établissent la réalité.
- Toute manipulation se fait dans un environnement isolé, avec des données fictives.
- Aucun protocole médical ni aucune obligation réglementaire ne sont inventés : ils sont formulés en questions (Q-01 à Q-11).
