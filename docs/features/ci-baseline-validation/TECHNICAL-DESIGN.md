# CI Baseline Validation — Technical Design

## Architecture concernée

Workflow GitHub Actions : `.github/workflows/ci.yml`.

Aucun module Spring Boot ou Angular n’est modifié.

## Diagnostic

### Backend

Le fichier `backend/mvnw` est présent mais GitHub Actions ne peut pas l’exécuter directement sur le runner Ubuntu :

```text
./mvnw: Permission denied
```

La solution retenue est de rendre le wrapper exécutable dans le workspace du runner avant de lancer la commande standard du projet.

```yaml
- name: Ensure Maven wrapper is executable
  run: chmod +x mvnw

- name: Build and verify (Maven strict)
  run: ./mvnw clean verify -B -Dspring.profiles.active=test
```

### Frontend

Le script NPM est :

```json
"test": "ng test"
```

La commande actuelle `npm test -- --run` transmet une option inexistante à Angular CLI 22. Angular 22 utilise Vitest par défaut et son option `watch` vaut `false` dans un environnement non-TTY comme GitHub Actions.

La commande CI devient donc :

```yaml
- name: Run tests
  run: npm test
```

## Choix rejetés

### Exécuter Maven système

Rejeté : cela contournerait le wrapper versionné et pourrait introduire une version Maven différente.

### Utiliser `bash ./mvnw`

Fonctionnel mais non retenu : le pipeline doit conserver la forme standard `./mvnw` imposée par la gouvernance du dépôt.

### Utiliser une option Vitest directe `--run`

Rejeté : le script appelle Angular CLI, pas directement le binaire Vitest. Les options doivent appartenir à `ng test`.

### Désactiver le watch avec une option non nécessaire

Non retenu : Angular CLI désactive déjà le watch hors TTY. Une option explicite `--watch=false` serait acceptable mais plus verbeuse sans valeur supplémentaire dans ce contexte.

## Sécurité

- aucune permission GitHub supplémentaire ;
- aucun secret ajouté ;
- aucune exécution de script distant ;
- `chmod` limité au fichier `mvnw` versionné ;
- dépendances installées avec `npm ci` et cache Maven existant.

## Tests

1. Ouvrir une Pull Request vers `main`.
2. Vérifier que l’étape Maven dépasse l’invocation du wrapper.
3. Vérifier que `ng test` démarre sans erreur d’option.
4. Observer les résultats applicatifs réels.
5. Si les suites échouent ensuite, créer des tickets distincts sans étendre cette PR.

## Impact SemVer

Aucun impact sur la version applicative.

## Rollback

Restaurer les deux lignes précédentes du workflow. Aucun rollback de données ou applicatif n’est nécessaire.