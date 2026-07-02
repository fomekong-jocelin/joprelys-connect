# README IA — Source de vérité technique

Ce dossier centralise le contexte technique, les règles de qualité, les tickets, le suivi, les décisions et les références.

## Objectif

Éviter :

- les régressions ;
- les réponses contradictoires entre IA ;
- les tickets traités hors contexte ;
- les modifications sans traçabilité ;
- la dette technique invisible ;
- les tickets macro non découpés ;
- les livraisons sans preuve de test.

## Ordre de lecture obligatoire

1. `../../AGENTS.md`
2. `../../SKILL.md`
3. `../../PROJECT-MANAGER-SKILL.md` si contexte projet
4. `WORKFLOW-IA.md`
5. `PROJECT-TRACKING.md`
6. `CHANGELOG.md`
7. `review-checklist.md`
8. ticket concerné dans `tickets/`
9. `REFERENCES.md` si besoin d'une source externe

## Règle de centralisation

Toute information utile doit être inscrite dans un fichier.

| Information | Fichier |
|---|---|
| Règles IA générales | `../../AGENTS.md` |
| Règles engineering | `../../SKILL.md` |
| Règles chef de projet | `../../PROJECT-MANAGER-SKILL.md` |
| Workflow IA | `WORKFLOW-IA.md` |
| Suivi global technique | `PROJECT-TRACKING.md` |
| Changelog | `CHANGELOG.md` |
| Ticket actionnable | `tickets/TICKET-*.md` |
| Décision architecture | `adr/ADR-*.md` |
| Qualité/review | `review-checklist.md` |
| Références externes | `REFERENCES.md` |
| Sprint/capacité | `../pm/` |

## Résultat attendu d'une intervention IA

La réponse finale doit indiquer :

- ce qui a été fait ;
- le découpage éventuel ;
- les fichiers modifiés ;
- les tests à exécuter ou exécutés ;
- les risques restants ;
- l'impact planning ;
- les documents de suivi mis à jour ;
- ce qui reste à faire.
