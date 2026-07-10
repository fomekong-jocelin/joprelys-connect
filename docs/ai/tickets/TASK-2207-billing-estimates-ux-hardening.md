# TASK-2207 — Durcissement UX des devis et actions financières

| Champ | Valeur |
|---|---|
| ID | TASK-2207 |
| Epic | EPIC-0020 / STORY-2205 |
| Type | Correctif UI/UX + tests |
| Priorité | P2 |
| Stack | Angular 22 / Tailwind CSS v4 / Vitest |
| Statut | QA |
| Profil recommandé | Senior Frontend |
| Estimation Senior | 0.8 j |
| Estimation Intermédiaire | 1.2 j |
| Estimation Junior | 2.0 j |
| Reviewer | Lead Developer + DAF |
| Impact SemVer | PATCH |
| Breaking change | Non |

## Problème

1. Annulation par `confirm()` navigateur.
2. Absence de liaison explicite du devis à une visite.
3. Messages succès/erreur invisibles.
4. Chargement deep-link sans état visuel.
5. Tests limités à l'ajout/suppression de lignes.
6. `BillingEstimatesComponent` dépasse la limite dure de 500 lignes.

## Critères d'acceptation

- [x] Le `confirm()` natif est supprimé.
- [x] Une modale maison accessible et compatible light/dark protège l'annulation.
- [x] Le devis peut être associé à une visite et transmet le `visitId`.
- [x] Les succès et erreurs sont visibles et traduits.
- [x] Le chargement deep-link est visible.
- [x] Le template et les styles du composant sont extraits.
- [x] Les tests couvrent devis, validation, annulation et avoir.
- [ ] Exécuter Vitest et le build Angular.
- [ ] Valider visuellement light/dark, clavier et mobile.

## Sécurité et régression

- Aucun contrat API ou backend n'est modifié.
- Le backend reste maître des règles financières.
- Aucun Angular Material, secret ou URL backend n'est ajouté.
