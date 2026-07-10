# CI Baseline Validation — Test Plan

## Objectif

Vérifier que les jobs GitHub Actions atteignent les véritables suites de tests backend et frontend, sans neutraliser leurs résultats.

## Scénario backend

### Préconditions

- runner Ubuntu ;
- Java 21 ;
- dépôt checkouté ;
- datasource H2 et secret JWT de test injectés.

### Étapes

1. Exécuter `chmod +x mvnw` dans `backend`.
2. Exécuter :

```bash
./mvnw clean verify -B -Dspring.profiles.active=test
```

### Résultats attendus

- absence de `Permission denied` ;
- Maven démarre et résout le projet ;
- Flyway et les tests s’exécutent ;
- tout échec réel fait échouer le job.

## Scénario frontend

### Préconditions

- Node.js 22 ;
- dépendances installées via `npm ci` ;
- Angular 22 / Vitest.

### Étapes

```bash
npm test
npm run build
```

### Résultats attendus

- absence de `Unknown argument: run` ;
- exécution unique des tests en CI non-TTY ;
- sortie du processus après les tests ;
- build de production exécuté seulement si les tests réussissent ;
- tout échec réel fait échouer le job.

## Non-régression

- déclenchement conservé sur push et PR vers `main` et `develop` ;
- cache Maven et NPM conservé ;
- versions Java 21 et Node.js 22 conservées ;
- variables de test conservées ;
- aucun changement du code produit.

## Critères de sortie

- les deux erreurs d’invocation initiales ont disparu ;
- les résultats applicatifs réels sont visibles ;
- les défauts applicatifs éventuels sont documentés dans des tickets séparés ;
- la PR reste bloquée si une suite réelle échoue.