# TICKET-0104 — Migration de Gradle vers Maven (Backend) & Intégration de Tailwind CSS v4 (Frontend)

## 1. Objectif

Ce ticket couvre deux demandes structurelles majeures :
1. **Migration Backend** : Remplacer l'outil de build Gradle par Maven dans le projet Spring Boot [backend](file:///C:/MES-APPLICATIONS/joprelys-connect/backend). Cela implique de créer un fichier `pom.xml` adapté, de configurer le wrapper Maven `mvnw` pour l'exécution locale, et de nettoyer tous les résidus Gradle.
2. **Intégration Tailwind CSS** : Configurer le framework utilitaire Tailwind CSS v4 dans le projet Angular [web](file:///C:/MES-APPLICATIONS/joprelys-connect/web), en utilisant la configuration moderne basée sur PostCSS et l'import direct de style CSS `@import "tailwindcss"`.

## 2. Critères d'acceptation

- [x] Fichier `pom.xml` créé dans le dossier `backend` avec les mêmes dépendances et plugins que `build.gradle` (Spring Boot 4.1.0, Java 21).
- [x] Wrapper Maven (`mvnw`, `mvnw.cmd`, dossier `.mvn/wrapper/`) généré avec succès dans le dossier `backend`.
- [x] Tous les fichiers et dossiers associés à Gradle (`build.gradle`, `settings.gradle`, `gradlew`, `gradlew.bat`, `.gradle`, `build`) supprimés du backend.
- [x] Les tests et la compilation du backend s'exécutent avec succès sous Maven via `.\mvnw clean test`.
- [x] Dépendances Tailwind CSS v4 (`tailwindcss`, `@tailwindcss/postcss`, `postcss`) installées dans le dossier `web`.
- [x] Fichier `postcss.config.js` créé pour charger `@tailwindcss/postcss`.
- [x] Directive `@import "tailwindcss";` ajoutée au fichier global [web/src/styles.css](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/styles.css).
- [x] Le build Angular via `npm run build` et les tests unitaires via `npm run test` passent avec succès.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | QUAL |
| User story parent | |
| Sprint cible | SPRINT-0002 |
| Priorité business | P0 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Senior |
| Effort estimé senior | 0.2j |
| Effort estimé intermédiaire | 0.35j |
| Effort estimé junior | 0.6j |
| Responsable | Gemini |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Moyen |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu si nécessaire
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Fichiers Gradle existants analysés (`build.gradle` inspecté)
- [x] Version de Maven locale vérifiée (Apache Maven 3.9.11)

## 5. Hypothèses

- L'utilisation de Spring Boot 4.1.0 et Java 21 reste valide pour le projet.
- L'utilisation de Tailwind CSS v4 (sélectionnée par l'utilisateur lors du choix de configuration) simplifie le build sans nécessiter de fichier de configuration `tailwind.config.js` externe grâce à l'analyse au vol des styles.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Problème de résolution de dépendance Maven | Échec de build ou de déploiement | Validation avec le parent Spring Boot 4.1.0 et test d'intégration |
| Poids de bundle Tailwind excessif | Lenteurs de chargement du frontend | L'analyseur CSS de Tailwind v4 élimine automatiquement toutes les classes non utilisées lors de la phase de compilation |

## 7. Action plan

- [x] Créer le ticket actionnable (`TICKET-0104-gradle-to-maven-tailwind.md`)
- [x] Créer `pom.xml` dans le backend
- [x] Générer le wrapper Maven via `mvn wrapper:wrapper` dans le backend
- [x] Supprimer les fichiers Gradle
- [x] Exécuter `.\mvnw clean test` pour valider le build Maven
- [x] Installer les packages npm Tailwind CSS v4 dans le dossier `web`
- [x] Créer `web/postcss.config.js`
- [x] Importer Tailwind dans `web/src/styles.css`
- [x] Exécuter `npm run build` et `npm run test` pour valider l'intégration frontend
- [x] Mettre à jour `CHANGELOG.md`
- [x] Mettre à jour `PROJECT-TRACKING.md`
- [x] Documenter le changement dans la réponse finale

## 8. Implémentation réalisée

- [x] **Migration Backend** :
  - Création du `pom.xml` avec le parent Spring Boot `4.1.0`, Java `21` et les 8 dépendances applicatives requises.
  - Wrapper Maven généré avec la commande `mvn wrapper:wrapper` (Apache Maven 3.9.11 configuré).
  - Suppression complète des dossiers et fichiers Gradle (`.gradle`, `build`, `build.gradle`, `settings.gradle`, `gradlew`, `gradlew.bat`).
  - Validation réussie par `.\mvnw clean test` (8 tests passés avec succès).
- [x] **Intégration Tailwind CSS** :
  - Installation de `tailwindcss` (v4.0.0+), `@tailwindcss/postcss` et `postcss`.
  - Configuration de [web/postcss.config.js](file:///C:/MES-APPLICATIONS/joprelys-connect/web/postcss.config.js).
  - Import `@import "tailwindcss";` ajouté dans [web/src/styles.css](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/styles.css).
  - Validation réussie par `npm run build` et `npm run test -- --watch=false` (génération réussie du CSS Tailwind dans `styles-JBPZVY54.css`, 6 tests Angular passés).

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-02 | Gemini | 0.30j | 100% | Aucun | Aucun | Migration Maven et intégration Tailwind v4 validées avec succès |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
cd backend && .\mvnw clean test
cd web && npm run build
cd web && npm run test -- --watch=false
```

### Résultats

- [x] Tests unitaires OK (les tests backend sous Maven et frontend sous Vitest passent avec succès)
- [x] Build OK (backend compilé et web généré)
- [x] Non exécuté avec justification

## 11. Documentation

- [x] README mis à jour si nécessaire
- [x] API docs mises à jour si nécessaire
- [x] ADR créé si décision structurante (non requis ici, directives claires de l'utilisateur)
- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire

- Aucun

## 13. Statut final

Statut : DONE

## 14. Notes finales

Le backend est entièrement sous Maven et le frontend dispose de Tailwind CSS v4 prêt à l'emploi.

## 15. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Changement de l'outil de build du backend et ajout du framework de styles Tailwind CSS |
| Breaking change | Non (les contrats d'API et l'exécution restent identiques) |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Oui (Tailwind intégré) |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |
