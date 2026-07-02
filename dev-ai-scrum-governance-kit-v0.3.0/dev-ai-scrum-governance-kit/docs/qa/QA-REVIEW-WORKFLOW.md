# QA REVIEW WORKFLOW

## 1. Objectif

Vérifier qu'une tâche est réellement prête à merge/livrer.

## 2. Étapes

1. Lire le ticket.
2. Vérifier les critères d'acceptation.
3. Lire les fichiers modifiés.
4. Vérifier les tests.
5. Vérifier sécurité/régression.
6. Vérifier documentation/changelog.
7. Classer les commentaires P0 à P3.

## 3. Sévérité

| Sévérité | Signification | Action |
|---|---|---|
| P0 | Bloquant sécurité/régression | Rejet immédiat |
| P1 | Problème majeur | Corriger avant merge |
| P2 | Problème mineur | Corriger si simple ou créer ticket |
| P3 | Nit/style | Optionnel |

## 4. Rapport attendu

```markdown
## Verdict
APPROVED / CHANGES_REQUESTED / BLOCKED

## P0

## P1

## P2

## P3

## Tests vérifiés

## Risques restants
```
