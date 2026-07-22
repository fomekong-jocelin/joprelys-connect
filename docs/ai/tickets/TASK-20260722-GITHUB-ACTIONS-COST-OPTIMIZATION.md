# TASK-20260722 — Réduire la consommation GitHub Actions

## Type

Diagnostic + Engineering CI/CD

## Contexte

Le compte GitHub Free a consommé les 2 000 minutes Actions incluses sur le mois courant et atteint un budget facturable de 10 USD. Le dépôt `joprelys-connect` représente l'usage visible concerné.

Le workflow actuel lance systématiquement deux runners lourds pour chaque changement non documentaire ciblant `main` ou `develop` :

- backend : Maven `clean verify` ;
- frontend : tests Angular + build production.

Cela double inutilement la consommation lorsqu'une modification ne touche qu'une seule stack. Les nombreuses itérations de PR accentuent l'effet. `concurrency.cancel-in-progress` est déjà présent et doit être conservé.

## Objectif

Réduire les minutes consommées sans supprimer de tests ni affaiblir la validation de la stack modifiée.

## Critères d'acceptation

- [x] Documenter le comportement actuel et la stratégie cible.
- [x] Conserver `paths-ignore` pour la documentation.
- [x] Conserver `concurrency.cancel-in-progress`.
- [x] Détecter les changements backend/frontend avant les jobs lourds.
- [x] Exécuter Maven uniquement lorsque `backend/**` ou le workflow CI change.
- [x] Exécuter Angular uniquement lorsque `web/**` ou le workflow CI change.
- [x] Conserver le `clean verify` Maven strict.
- [x] Conserver les tests Angular et le build production.
- [x] Ajouter des timeouts de sécurité.
- [x] Utiliser des permissions GitHub Actions minimales explicites.
- [ ] Obtenir une exécution GitHub Actions complète du workflow modifié.
- [ ] Mesurer la consommation après un cycle de développement et ajuster si nécessaire.

## Diagnostic

### Cause principale

Le workflow n'est pas path-aware au niveau des jobs. Toute modification applicative déclenche simultanément le backend et le frontend.

### Facteur aggravant

Le dépôt connaît un rythme élevé de commits/PR et les synchronisations de PR relancent la CI. L'annulation automatique limite les runs concurrents mais ne récupère pas les minutes déjà consommées.

### Non-causes

- le cache Maven est déjà activé ;
- le cache npm est déjà activé ;
- le cache du compilateur Angular existe ;
- les modifications documentaires sont déjà exclues ;
- l'annulation des runs obsolètes existe déjà.

## Action plan

- [x] Lire les règles du dépôt et la documentation CI/CD.
- [x] Auditer `.github/workflows/ci.yml`.
- [x] Créer la documentation fonctionnelle.
- [x] Créer la documentation technique.
- [x] Ajouter une détection de chemins légère.
- [x] Conditionner les jobs backend/frontend.
- [x] Ajouter des limites de durée.
- [x] Ne supprimer aucun test.
- [x] Mettre à jour le changelog.
- [x] Mettre à jour le suivi projet.
- [ ] Valider le run GitHub Actions après disponibilité du budget/minutes.

## Estimation

- Profil : DevOps / senior full-stack
- Estimation : 0,5 j
- Reviewer : Tech Lead
- Risque : faible à moyen, principalement lié aux filtres de chemins.

## Tests attendus

1. modification `backend/**` : backend exécuté, frontend skipped ;
2. modification `web/**` : frontend exécuté, backend skipped ;
3. modification des deux : deux jobs exécutés ;
4. modification `.github/workflows/ci.yml` : deux jobs exécutés ;
5. documentation seule : aucun workflow déclenché.

## Résultat

La correction est préparée sur `fix/ci-actions-cost-optimization`. Le workflow conserve le niveau de validation actuel pour chaque stack concernée et ajoute une sélection préalable des jobs coûteux.

## Reste à faire

La validation distante du workflow dépend du redémarrage effectif de GitHub Actions, actuellement susceptible d'être bloqué par le plafond budgétaire atteint.